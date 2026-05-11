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

## Relationships

- An **Incident** has exactly one current **IncidentStatus** and one current **Severity**
- An **Incident** produces many **Events** over its lifecycle
- An **External Event** is evaluated by one or more **Rules**; a matching **Rule** auto-creates an **Incident**
- An **Incident** has one **Summary** (regenerated on demand) and at most one **Postmortem** (generated post-resolution)

## Architecture decisions

**Inter-service communication**: NATS JetStream for side effects (notifications, event log writes, genai triggers). REST for query/response flows (frontend → gateway → service data).

**Event Log role**: Secondary append-only audit trail. `incident-service` owns Incident state in its own Postgres DB and publishes NATS events on every state change. `event-service` subscribes and appends Incident Events to the Event Log. The Event Log is NOT the primary write model.

**GenAI triggering and ownership**: `genai-service` is stateless (no DB). It subscribes to NATS `incident.created` and `incident.resolved`, fetches incident data from `incident-service` via REST, calls Ollama, then PATCHes results (Summary, Solutions, Postmortem) back to `incident-service`. `incident-service` owns and stores all AI-generated results alongside incident data. Frontend can also call genai-service via REST (through gateway) to trigger on-demand regeneration.

**Data aggregation**: None needed. `incident-service` owns all incident data including AI results. Frontend makes a single REST call to get a complete incident view.

**NATS event shape**: Thin events. `incident.created` (and similar) carry only `{incidentId, timestamp}`. Consumers fetch full data via REST if needed.

**Real-time updates**: SSE. Gateway subscribes to NATS incident events and fans out to connected frontend clients via Server-Sent Events (`SseEmitter` in Spring Boot). Frontend uses a single `EventSource` connection.

**Authentication**: JWT in `httpOnly` cookie (`SameSite=Strict`). `user-service` issues tokens on login; browser stores them as httpOnly cookies (not accessible to JS). Gateway reads the cookie, validates signature, injects `X-User-Id` / `X-User-Role` headers. Downstream services trust injected headers — no per-request call to user-service.

**Rule condition format**: Fixed JSON field-matcher schema. Each Rule has `conditions` (list of `{field, operator, value}`) ANDed together, and an `action` (`{createIncident: true, severity: "SEV2"}`). Rule engine evaluates conditions against normalized External Event fields. No expression language.

**Notifications**: In-app only. `notification-service` stores notifications in its own DB. No email. Frontend fetches via REST or receives via SSE.

**NATS subjects**:
| Publisher          | Subject                     | Subscribers                                                                    |
| ------------------ | --------------------------- | ------------------------------------------------------------------------------ |
| `incident-service` | `incident.created`          | event-service, genai-service, notification-service, gateway (SSE)              |
| `incident-service` | `incident.updated`          | event-service, gateway (SSE)                                                   |
| `incident-service` | `incident.severity.escalated` | event-service, notification-service, gateway (SSE)                          |
| `incident-service` | `incident.resolved`         | event-service, genai-service (postmortem), notification-service, gateway (SSE) |
| `incident-service` | `incident.comment.added`    | event-service, notification-service                                            |
| `incident-service` | `incident.assigned`         | event-service, notification-service                                            |
| `webhook-service`  | `external.event.received`   | rule-engine                                                                    |
| `rule-engine`      | `incident.create.requested`            | incident-service                                                               |
| `rule-engine`      | `incident.severity.escalate.requested` | incident-service                                                               |

**Tech stack**:
- Frontend: React + Vite + TypeScript + shadcn/ui + tanstack-query
- Backend services: Java Spring Boot (latest)
- GenAI service: Python (FastAPI + nats.py + ollama client)

**LLM provider**: Ollama (`qwen2.5:3b`), both local dev (docker-compose) and production (K8s). Ollama runs as a separate deployment. genai-service calls `http://ollama:11434`. No cloud LLM dependency.

**Comments**: Immutable. No edit or delete. Consistent with the append-only Event Log.

**Observability stack**: `kube-prometheus-stack` (Prometheus + Grafana + Alertmanager) + `loki-stack` (Loki + Promtail) as Helm dependencies added to flat `infra/helm/devops-platform` chart. All services expose `/metrics` (Micrometer for Spring Boot, `prometheus-fastapi-instrumentator` for Python). Services log structured JSON to stdout; Promtail ships to Loki. Grafana is the single pane for metrics + logs. Distributed tracing: out of scope.

**Helm chart structure**: flat chart (one chart, all services as templates). No subcharts.

**K8s resource defaults** (tune to cluster once node specs known):
| Workload | Memory req/limit | CPU req/limit |
|---|---|---|
| Ollama | 4Gi / 6Gi | 2 / 4 |
| Spring Boot services (×7) | 256Mi / 512Mi | 100m / 500m |
| genai-service (Python) | 128Mi / 256Mi | 50m / 200m |
| Postgres | 256Mi / 512Mi | 100m / 500m |
| NATS | 64Mi / 128Mi | 50m / 200m |
| kube-prometheus-stack | 512Mi / 1Gi | 200m / 500m |
| loki-stack | 256Mi / 512Mi | 100m / 200m |

**Alerting**: Alertmanager (bundled in `kube-prometheus-stack`), routing to Grafana UI only (no Slack/email). Full alert set:
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
| `ai_generation_seconds` | histogram | `type=summary\|postmortem` |
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
