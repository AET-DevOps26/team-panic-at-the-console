# notification-service

In-app notifications for incident events. Consumes incident lifecycle events from
NATS and materializes them as per-user notifications. Owns the `notifications` database.

Port: **8085**

## Behavior

Subscribes to five NATS subjects published by `incident-service` (see the subjects
table in [CONTEXT.md](../../CONTEXT.md)) and stores one notification per event:

| Subject                       | Type                 | Recipient                    |
| ----------------------------- | -------------------- | ---------------------------- |
| `incident.created`            | `INCIDENT_CREATED`   | broadcast (all users)        |
| `incident.severity.escalated` | `SEVERITY_ESCALATED` | broadcast (all users)        |
| `incident.resolved`           | `INCIDENT_RESOLVED`  | broadcast (all users)        |
| `incident.comment.added`      | `COMMENT_ADDED`      | broadcast (all users)        |
| `incident.assigned`           | `INCIDENT_ASSIGNED`  | the assigned user (`userId`) |

A notification with a null `recipientId` is a broadcast visible to everyone; a
personal notification (assignment) targets a single user. In line with the thin
NATS event shape, the service does not call other services: it uses only the fields
carried on each event. It deliberately does not subscribe to `incident.updated`.

If NATS is unreachable at startup the service logs a warning and runs in degraded
mode: the REST API keeps serving already-stored notifications and no new events are
consumed. Set `nats.failOnStartup=true` to abort startup instead.

## Endpoints

| Method | Path                       | Description                                                            |
| ------ | -------------------------- | ---------------------------------------------------------------------- |
| `GET`  | `/health`                  | Health check (OpenAPI `healthCheck`)                                   |
| `GET`  | `/notifications`           | List, newest first. Query: `recipientId`, `unreadOnly`, `page`, `size` |
| `POST` | `/notifications/{id}/read` | Mark one notification read (`204`, or `404` if unknown)                |
| `POST` | `/notifications/read-all`  | Mark all read (optional `recipientId`); returns `204`                  |

`GET /notifications` returns items plus `total` and `unreadCount` for the same
scope, so the frontend can render an unread badge in one request. When `recipientId`
is supplied the result is scoped to that user's personal notifications plus
broadcasts; when omitted, all notifications are returned.

The endpoints are defined in `api/openapi.yaml` (tag `notifications`) and the
controller implements the generated `NotificationsApi`, so the spec is the single
source of truth. The gateway proxies them under `/api/v1/notifications*`
(`NotificationsProxyController` in `services/gateway`).

## Read state

Read/unread is tracked per notification row. Full per-user read tracking depends on
gateway-injected identity (`X-User-Id`), which is still landing; until then callers
pass `recipientId` explicitly.

## Local dev

```bash
cd services/notification-service
pixi install
pixi run test
pixi run start
```

From the repo root:

```bash
pixi run --manifest-path services/notification-service/pixi.toml test
```

Docker (build context is `services/`, same as the other Java services):

```bash
docker build -f services/notification-service/Dockerfile -t notification-service:local services
```
