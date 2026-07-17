# webhook-service

Receives webhooks from external systems (e.g. GitHub Actions), persists each one verbatim as an **External Event** (ADR 0008: auditability), and publishes `external.event.received` to NATS for `incident-service` evaluation.

Ingestion is decoupled from delivery: the event is committed to Postgres first, then published. If NATS is down, the webhook is still accepted (202) and a scheduled retrier republishes pending events once NATS is back. `external.event.received` is therefore at-least-once; the stable `sourceId` (the External Event id) is the dedup key for subscribers.

## Endpoints

| Route                                          | Action                                                                     |
| ---------------------------------------------- | -------------------------------------------------------------------------- |
| `POST /webhooks/{source}`                      | Ingest a webhook for a source slug (e.g. `github`); responds 202 + receipt |
| `GET /external-events`                         | Audit list (filters: `source`, `eventType`; paging: `page`, `size`)        |
| `GET /external-events/{id}`                    | Single event including the raw payload                                     |
| `GET /webhook-sources`                         | Registered sources (never includes secrets)                                |
| `POST /webhook-sources`                        | Register a source; generates + returns its HMAC secret (only here)         |
| `POST /webhook-sources/{source}/rotate-secret` | Replace the secret; returns the new one (only here)                        |
| `DELETE /webhook-sources/{source}`             | Delete the registration (its external events are kept)                     |
| `GET /health`                                  | Liveness                                                                   |
| `GET /actuator/prometheus`                     | Metrics (`webhooks_received_total{source_type}`, `nats_messages_total`)    |

All endpoints are documented in `api/openapi.yaml` (tags `webhooks`, `external-events`, `webhook-sources`) and visible in Swagger UI. The read API, source management, and `/health` implement the generated interfaces (`ExternalEventsApi`, `WebhookSourcesApi`, `HealthApi`); the ingest controller stays hand-written because HMAC verification needs the exact raw request bytes, which the generated (parsed-body) signature cannot provide — its response bodies still use the generated models, so the wire contract matches the spec. The `external-events` and `webhook-sources` routes are also served through the gateway (`/api/v1/...`, session-authenticated) for the frontend Sources page; webhook senders authenticate per source via HMAC instead.

## Public routing

Only the ingest prefix `/webhooks` is exposed; the read/audit API and actuator stay cluster-internal.

- **Kubernetes**: the Helm ingress routes `https://<host>/webhooks/{source}` directly to webhook-service. Because the endpoint is public, the chart sets `WEBHOOK_REQUIRE_SIGNATURE=true`: only sources registered via the Sources page (or env-provisioned via `secrets.webhookGithubSecret` → `WEBHOOK_SECRETS_GITHUB` in the `webhook-credentials` secret) are accepted.
- **Compose**: the `edge` proxy forwards `http://localhost:8080/webhooks/{source}` (the service is also reachable directly on `:8086`). Signatures stay optional locally (`WEBHOOK_REQUIRE_SIGNATURE=false`).

## Adding a source

Sources are self-service: register one on the frontend Sources page (or `POST /webhook-sources`), which generates the HMAC secret server-side and shows it exactly once. The registration lives in the `webhook_sources` table (owned `webhooks` DB); because HMAC verification needs the original secret it is stored as-is, not hashed — rotate it from the same page if it leaks.

1. Register a slug, e.g. `grafana`, copy the payload URL and secret, and configure both on the sender (content type `application/json`). Registered sources must sign every delivery (`X-Hub-Signature-256`), so they work in deployments with `WEBHOOK_REQUIRE_SIGNATURE=true` without any operator involvement.
2. Make the events matchable: either the sender includes an `eventType` field / `X-Event-Type` header, or extend `EventTypeNormalizer` with an inference rule for that sender.
3. Send a failure-like event type or payload value so the embedded `incident-service` evaluator creates a `SEV2` incident.

For GitHub specifically: repo → Settings → Webhooks → add `https://<host>/webhooks/github`, content type `application/json`, secret = the secret from registration, and select the events (e.g. workflow runs).

Unregistered slugs still work the old way: an env-configured secret (`WEBHOOK_SECRETS_<SOURCE>`, e.g. the Helm `secrets.webhookGithubSecret` SOPS value) acts as a deployment-level fallback, and slugs without any secret are accepted unverified unless `WEBHOOK_REQUIRE_SIGNATURE=true`. A registration always takes precedence over the env fallback for the same slug.

### Ingestion behaviour

- The body must be a JSON object; it is stored byte-for-byte as the `rawPayload` later carried on the NATS event.
- **Dedup**: redeliveries with the same `X-GitHub-Delivery` (or `X-Delivery-Id`) header are acknowledged with the original event id (`duplicate: true`) and not re-published.
- **Event type** (`eventType`, what Rules match on) is derived by precedence: explicit `eventType` field in the payload → `X-Event-Type` header → GitHub inference (`X-GitHub-Event: workflow_run` with a failed conclusion becomes `ci_failure`, success `ci_success`; other GitHub events become `github.<event>`) → `unknown`.

### Signatures

GitHub-convention HMAC: `X-Hub-Signature-256: sha256=<hex HMAC-SHA256 of the raw body>`.

- The secret for a slug is looked up in `webhook_sources` first, then the env fallback (`WEBHOOK_SECRETS_<SOURCE>`).
- A source with a secret (registered or env) must send a valid signature; otherwise 401.
- Sources without any secret are accepted unverified unless `WEBHOOK_REQUIRE_SIGNATURE=true`, which rejects them outright (the Helm chart sets this, so in Kubernetes every source must be registered or env-provisioned).

## Configuration

| Property / env                                            | Default                                     |
| --------------------------------------------------------- | ------------------------------------------- |
| `spring.datasource.url` / `SPRING_DATASOURCE_URL`         | `jdbc:postgresql://localhost:5432/webhooks` |
| `nats.url` / `NATS_URL`                                   | `nats://localhost:4222`                     |
| `webhook.secrets.<source>` / `WEBHOOK_SECRETS_<SOURCE>`   | unset (fallback for unregistered slugs)     |
| `webhook.require-signature` / `WEBHOOK_REQUIRE_SIGNATURE` | `false`                                     |
| `webhook.publish.retry-delay-ms`                          | `30000`                                     |
| `webhook.publish.min-age-ms`                              | `10000`                                     |
| `webhook.publish.max-attempts`                            | `10`                                        |

Owns the `webhooks` database (created by `infra/helm/devops-platform/files/init-dbs.sh`); schema is managed by Flyway. The publish retrier assumes a single replica (two replicas would double-publish pending events).

## Local development

```bash
pixi run --manifest-path services/webhook-service/pixi.toml test    # needs Docker (Testcontainers)
pixi run --manifest-path services/webhook-service/pixi.toml start   # needs compose postgres + nats
```

Smoke test against the Compose stack (`docker compose up`):

```bash
curl -i -X POST http://localhost:8086/webhooks/github \
  -H 'Content-Type: application/json' \
  -H 'X-GitHub-Event: workflow_run' \
  -H 'X-GitHub-Delivery: demo-1' \
  -d '{"action":"completed","workflow_run":{"name":"CI","head_branch":"main","conclusion":"failure"}}'
```

## Out of scope (for now)

- **Comment-linking follow-up events to incidents** (second half of ADR 0008): needs incident-service support for machine-authored comments (comments currently require a gateway-injected `X-User-Id`) plus a correlation lookup. The External Events persisted here are the input for that later step.
