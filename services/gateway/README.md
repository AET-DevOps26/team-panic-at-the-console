# gateway

API entry point for the incident platform. Exposes routes from `api/openapi.yaml` under `/api/v1` and proxies to cluster-internal services.

OpenAPI exploration uses the standalone **swagger-ui** service (`api/openapi.yaml`), not springdoc on this service.

## Implemented routes

| Route                                      | Action                                                |
| ------------------------------------------ | ----------------------------------------------------- |
| `GET /api/v1/health`                       | Gateway liveness (local)                              |
| `POST /api/v1/incidents/{id}/genai/*`      | Proxy to `incident-service` regen endpoints           |
| `GET/POST/PATCH /api/v1/incidents*`        | Proxy to `incident-service` incident REST API         |
| `POST /api/v1/auth/*`                      | Proxy to `user-service` (forwards session cookie)     |
| `GET /api/v1/users*`                       | Proxy to `user-service` (forwards session cookie)     |
| `GET/POST /api/v1/notifications*`          | Proxy to `notification-service` notification REST API |
| `GET /api/v1/external-events*`             | Proxy to `webhook-service` external-events audit API  |
| `GET/POST/DELETE /api/v1/webhook-sources*` | Proxy to `webhook-service` source management API      |
| `GET /api/v1/incidents/stream`             | NATS-backed SSE invalidation stream                   |

GenAI compute runs via NATS (`genai-service`); Ollama reachability is checked on that service (`/api/v1/genai/ollama/health`), not exposed on the gateway.

Webhook ingest (`POST /webhooks/{source}`) is NOT proxied here: the ingress / compose edge routes it directly to `webhook-service`, since senders authenticate via HMAC rather than a session.

Ingress (and local compose `edge` on port 8080) sends traffic with prefix `/api`; clients should use `/api/v1/...`. Swagger UI is at `/swagger/` on the same host.

## Configuration

Downstream base URLs are required (`@NotBlank` on `GatewayProperties`). Local defaults live in `application.properties`; compose and Helm set `GATEWAY_*` env vars for deployed environments.

| Property / env                                                          | Local default (`application.properties`) |
| ----------------------------------------------------------------------- | ---------------------------------------- |
| `gateway.incident-service-url` / `GATEWAY_INCIDENT_SERVICE_URL`         | `http://localhost:8081`                  |
| `gateway.user-service-url` / `GATEWAY_USER_SERVICE_URL`                 | `http://localhost:8084`                  |
| `gateway.notification-service-url` / `GATEWAY_NOTIFICATION_SERVICE_URL` | `http://localhost:8085`                  |
| `gateway.webhook-service-url` / `GATEWAY_WEBHOOK_SERVICE_URL`           | `http://localhost:8086`                  |

## Local development

```bash
pixi run --manifest-path services/gateway/pixi.toml test
pixi run --manifest-path services/gateway/pixi.toml start
```

With Compose (`docker compose up`), use the `edge` proxy on port **8080** (`/api/v1/...` for API, `/swagger/` for docs). Downstream URLs are set via environment variables.

`pixi run start` on this module binds **8080** directly (API only, no `/swagger/`).

## Authentication and real-time updates

The gateway validates the `session` JWT cookie on protected API routes and derives `X-User-Id` / `X-User-Role` from its validated claims. Client-supplied identity headers are never forwarded. Downstream services receive only the gateway-derived headers.

Gateway subscribes to `incident.>` on NATS and exposes `GET /api/v1/incidents/stream` as an SSE cache-invalidation stream. The frontend reloads data through REST after receiving an event.

Event-service routes and GenAI write-back `PATCH .../genai/*/result` remain cluster-internal; the gateway returns `403` for GenAI write-back paths.
