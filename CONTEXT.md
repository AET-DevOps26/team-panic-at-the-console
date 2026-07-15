# Incident Management System

Lightweight incident management system for detecting, tracking, and resolving incidents with an immutable audit trail and AI-assisted analysis. TUM DevOps Project, Spring 2026.

## Language

**Incident**:
A tracked system failure or problem, with a lifecycle (open → investigating → resolved), severity, and assigned responders.
_Avoid_: Issue, alert, ticket

**Event** (Incident Event):
An immutable, timestamped record of something that happened to an Incident — status change, comment, assignment, severity change. Stored in the Event Log.
_Avoid_: Log entry, audit record, message

**External Event**:
A raw payload received from an external system (e.g. GitHub Actions webhook) that may trigger incident creation via the Rule Engine.
_Avoid_: Webhook payload, signal (overloaded)

**Event Log**:
The append-only, chronological sequence of Incident Events for a given Incident. Owned by the event-service. Serves as the audit trail and timeline — it is a secondary read model, not the primary write model.
_Avoid_: Event store, timeline (use timeline only in UI context), audit log

**Rule**:
A configurable condition evaluated against an External Event that determines whether to auto-create an Incident and at what severity.
_Avoid_: Trigger, automation, policy

**Summary**:
An AI-generated concise description of an Incident's current state, derived from the Event Log and Incident metadata.
_Avoid_: AI summary (redundant), description (that's user-authored)

**Postmortem**:
An AI-generated document produced after Incident resolution, containing root cause analysis, timeline, and action items.
_Avoid_: Post-incident report, retrospective

**RuleEvaluator**:
The deep module in `rule-engine` that tests a single Rule's conditions against a single External Event and returns a `MatchResult`. All condition-matching logic (field extraction, operator dispatch, AND-ing) lives here. Interface: `evaluate(rule, externalEvent) → MatchResult`.
_Avoid_: rule checker, rule processor, condition evaluator

**MatchResult**:
The output of `RuleEvaluator.evaluate()` — whether the Rule matched and, if so, what action to take (`createIncident`, `severity`). A non-match carries no action.

**PromptBuilder**:
The deep module in `genai-service` that constructs a fully-formed Ollama prompt from an Incident, its Event Log, and a `PromptTask`. All context-length management, formatting, and structured output schema specification lives here. Interface: `build(incident, events, task) → Prompt`.
_Avoid_: prompt generator, prompt factory, LLM formatter

**PromptTask**:
An enumeration of what the `PromptBuilder` is asked to produce: `SUMMARY`, `SEVERITY_SUGGESTION`, `SOLUTION_SUGGESTIONS`, `POSTMORTEM`. Each task maps to a different system prompt and response schema.

## Relationships

- An **Incident** has exactly one current **IncidentStatus** and one current **Severity**
- An **Incident** produces many **Events** over its lifecycle
- An **External Event** is evaluated by one or more **Rules**; a matching **Rule** auto-creates an **Incident**
- An **Incident** has one **Summary** (regenerated on demand) and at most one **Postmortem** (generated post-resolution)

## Architecture decisions

**Inter-service communication**: NATS JetStream for side effects (notifications, event log writes, genai triggers). REST for query/response flows (frontend → gateway → service data).

**Event Log role**: Secondary append-only audit trail. `incident-service` owns Incident state in its own Postgres DB and publishes NATS events on every state change. `event-service` subscribes and appends Incident Events to the Event Log. The Event Log is NOT the primary write model.

**GenAI triggering and ownership**: `genai-service` is stateless (no DB) and purely NATS-driven — it exposes no user-facing HTTP endpoints. It subscribes to `incident.created`, `incident.resolved`, and `incident.regen.requested`; fetches incident data from `incident-service` via REST; calls Ollama; then PATCHes results (Summary, Solutions, Postmortem) back to `incident-service`. `incident-service` owns and stores all AI-generated results alongside incident data. For on-demand regeneration, the frontend calls `POST /incidents/{id}/regen` on `incident-service` (through gateway); `incident-service` publishes `incident.regen.requested` to NATS and genai-service handles it like any other trigger. `genai-service` does retain a minimal HTTP server (FastAPI) for `/health` and `/metrics` endpoints only.

**Data aggregation**: None needed. `incident-service` owns all incident data including AI results. Frontend makes a single REST call to get a complete incident view.

**NATS event shape**: Events are thin by default — `{incidentId, timestamp}` only. Exceptions carry the minimal extra fields a subscriber needs to act without a REST callback: `incident.created` adds `title` and `severity` (timeline rendering), `incident.status.changed` adds `oldStatus` and `newStatus` (the generic `incident.updated` cannot distinguish status changes from other updates), `incident.severity.escalated` adds `oldSeverity` and `newSeverity`, `incident.comment.added` adds `commentId` and `content` (comments are immutable, so the copy never goes stale), `incident.assigned` adds `userId`, `incident.regen.requested` adds `task` (which AI field to regenerate), rule-engine events add the minimal fields needed to act (`sourceId`, `severity`, `requestedSeverity`). Full schemas in `api/specs/nats/*.schema.json`.

**`incident.create.requested` and incident title/description**: The schema carries only `{sourceId, severity, timestamp}`. `sourceId` references the External Event in webhook-service. `incident-service` must either: (a) call `GET /external-events/{sourceId}` on webhook-service to build a title (requires exposing that endpoint + `WEBHOOK_SERVICE_URL` env), or (b) auto-generate a title from the sourceId (e.g. `"Auto-created from external event {sourceId}"`). The genai-service will generate a Summary on `incident.created` regardless. Decision needed before implementing `incident-service`'s NATS consumer.

**Real-time updates**: SSE. Gateway subscribes to NATS incident events and fans out to connected frontend clients via Server-Sent Events (`SseEmitter` in Spring Boot). Frontend uses a single `EventSource` connection.

**Authentication**: JWT in `httpOnly` cookie (`SameSite=Strict`). `user-service` issues tokens on login; browser stores them as httpOnly cookies (not accessible to JS). Gateway reads the cookie, validates signature, injects `X-User-Id` / `X-User-Role` headers. Downstream services trust injected headers — no per-request call to user-service. **Security invariant**: downstream services must only be reachable via the gateway (cluster-internal networking only); they must reject requests that include `X-User-*` headers not originating from the gateway to prevent header spoofing.

**Rule condition format**: Fixed JSON field-matcher schema. Each Rule has `conditions` (list of `{field, operator, value}`, all must match) and an `action` (`{createIncident: true, severity: "SEV2"}`). Rule engine evaluates conditions against normalized External Event fields. No expression language.

**Notifications**: In-app only. `notification-service` stores notifications in its own DB. No email. Frontend fetches via REST or receives via SSE.

**NATS subjects**:
| Publisher          | Subject                     | Subscribers                                                                    |
| ------------------ | --------------------------- | ------------------------------------------------------------------------------ |
| `incident-service` | `incident.created`          | event-service, genai-service, notification-service, gateway (SSE)              |
| `incident-service` | `incident.updated`          | event-service, gateway (SSE)                                                   |
| `incident-service` | `incident.status.changed`   | event-service, gateway (SSE)                                                   |
| `incident-service` | `incident.severity.escalated` | event-service, notification-service, gateway (SSE)                          |
| `incident-service` | `incident.resolved`         | event-service, genai-service (postmortem), notification-service, gateway (SSE) |
| `incident-service` | `incident.comment.added`    | event-service, notification-service                                            |
| `incident-service` | `incident.assigned`         | event-service, notification-service                                            |
| `webhook-service`  | `external.event.received`   | rule-engine                                                                    |
| `rule-engine`      | `incident.create.requested`            | incident-service                                                               |
| `rule-engine`      | `incident.severity.escalate.requested` | incident-service                                                               |
| `incident-service` | `incident.regen.requested`             | genai-service                                                                  |

**Tech stack**:
- Frontend: React + Vite + TypeScript + shadcn/ui + tanstack-query
- Backend services: Java Spring Boot (latest)
- GenAI service: Python (FastAPI + nats.py + ollama client)

**LLM provider**: no paid provider (see ADR-0003). Local dev (docker-compose) defaults to Ollama (`qwen2.5:3b`) as a separate container; genai-service calls `http://ollama:11434`. In K8s the in-cluster Ollama deployment is disabled (`services.ollama.enabled: false`): the project quota cannot fit the model, so genai-service uses TUM Logos (`openai/gpt-oss-120b`) with the Ollama fallback off. Logos is only reachable from the TUM network (eduVPN).

**Comments**: Immutable. No edit or delete. Consistent with the append-only Event Log.

**Observability stack**: the chart **self-hosts Prometheus + Grafana inside the release namespace** (`monitoring.enabled`, default on). Both are plain `Deployment`/`Service`/`ConfigMap` resources: no `kube-prometheus-stack`, no CRDs, no cluster-scoped RBAC, so a namespace-scoped deploy user can install them. Prometheus scrapes each service's `/metrics` via static `scrape_configs`, evaluates alert rules itself (no Alertmanager), and provisions Grafana with a Prometheus datasource plus the bundled dashboards. Grafana is exposed under `/grafana` and Prometheus under `/prometheus`; the Grafana admin password comes from `secrets.grafanaPassword` (falls back to `monitoring.grafana.adminPassword`). An optional `monitoring.operatorCrds.enabled` compatibility mode instead ships namespaced CRs (`PodMonitor`, `PrometheusRule`, Grafana dashboard `ConfigMap`) for clusters that already run a shared prometheus-operator. The local docker-compose stack self-hosts Prometheus + Grafana for development. All services expose `/metrics` (Micrometer for Spring Boot, `prometheus-fastapi-instrumentator` for Python). Distributed tracing: out of scope.

**Helm chart structure**: flat chart (one chart, all services as templates). No subcharts.

**K8s resources**: the stud cluster's project quota caps the namespace at **4 cpu / 6Gi of limits** and rejects any pod that declares none, so every workload sets both. Anything without its own block inherits `global.defaultResources`.
| Workload | Memory req/limit | CPU req/limit |
|---|---|---|
| Spring Boot services (×7) + genai-service | 256Mi / 512Mi | 100m / 250m |
| frontend, swagger-ui | 32Mi / 128Mi | 25m / 100m |
| Postgres | 256Mi / 512Mi | 100m / 500m |
| NATS | 64Mi / 128Mi | 50m / 200m |
| Prometheus | 256Mi / 512Mi | 50m / 300m |
| Grafana | 128Mi / 256Mi | 50m / 250m |
| Ollama (disabled in K8s, see ADR-0003) | 3Gi / 4Gi | 250m / 2 |

The enabled workloads total ~5.6Gi / 3.45 cpu of limits, which leaves no room for a surge pod. Every Deployment therefore rolls without one (`maxSurge: 0`, or `Recreate` for RWO volumes and stateful singletons); the shared `devops-platform.rolloutStrategy` helper renders this. The Deployment default of `maxSurge: 25%` rounds up to +1 pod at `replicas: 1`, and that pod's limits would breach the quota, so the rollout would deadlock instead of finishing. The trade-off is a brief gap per workload on each deploy.

**Alerting**: the self-hosted Prometheus evaluates alert rules directly from its config (no Alertmanager, no external routing); alerts surface in the Prometheus and Grafana UIs. In `operatorCrds` compatibility mode the same rules ship as a `PrometheusRule` CR for the shared cluster operator instead. Full alert set:
| Alert | Condition | Severity |
|---|---|---|
| Service down | health check fails >1min | critical |
| High error rate | HTTP 5xx >5% over 5min | warning |
| Incident storm | incident creation rate >10/min | warning |
| Stale open incident | incident open >2h with no update | warning |
| Slow AI generation | `ai_generation_seconds` p95 >60s | warning |
| NATS consumer lag | JetStream pending >100 | warning |

**Custom metrics** (in addition to auto-instrumented HTTP/JVM defaults):
| Metric | Type | Labels |
|---|---|---|
| `incidents_total` | counter | `source=manual\|auto`, `severity` |
| `active_incidents` | gauge | `severity` |
| `incident_resolution_seconds` | histogram | — |
| `ai_generation_seconds` | histogram | `type`, `provider=ollama\|logos` |
| `ai_generations_total` | counter | `type`, `provider`, `outcome=success\|error` |
| `llm_fallback_total` | counter | `from_provider`, `to_provider` |
| `nats_messages_total` | counter | `subject`, `outcome` |
| `nats_consumer_connected` | gauge | — |
| `rule_evaluations_total` | counter | `matched=true\|false` |
| `webhooks_received_total` | counter | `source_type` |

**DB ownership**: one shared Postgres pod, one database per stateful service (`init-dbs.sh` creates them).
| Service | Database | Stateful? |
|---|---|---|
| `incident-service` | `incidents` | yes |
| `event-service` | `events` | yes |
| `user-service` | `users` | yes |
| `notification-service` | `notifications` | yes |
| `rule-engine` | `rules` | yes |
| `gateway` | — | no |
| `webhook-service` | — | no |
| `genai-service` | — | no |

**OpenAPI spec**: one combined `api/openapi.yaml` covering all services. Existing hook auto-regenerates clients on spec change.

## Example dialogue

> **Dev:** "When a user changes incident status, does incident-service write to the event log directly?"
> **Domain expert:** "No — incident-service writes to its own DB and publishes a NATS event. Event-service subscribes and appends the Incident Event to the Event Log."

> **Dev:** "So the Event Log is the source of truth?"
> **Domain expert:** "No — incident-service's DB is the source of truth for Incident state. The Event Log is the audit trail."
