# notification-service

In-app notifications for incident events. Consumes incident lifecycle events from
NATS and materializes them as per-user notifications. Owns the `notifications` database.

Port: **8085**

## Behavior

Subscribes to five NATS subjects published by `incident-service` (see the subjects
table in [CONTEXT.md](../../CONTEXT.md)):

| Subject                       | Type                                   | Recipients                         |
| ----------------------------- | -------------------------------------- | ---------------------------------- |
| `incident.created`            | `INCIDENT_CREATED`                     | broadcast (all users)              |
| `incident.severity.escalated` | `SEVERITY_ESCALATED`                   | assignees (`assignedUserIds`)      |
| `incident.status.changed`     | `STATUS_CHANGED` / `INCIDENT_RESOLVED` | assignees (`assignedUserIds`)      |
| `incident.comment.added`      | `COMMENT_ADDED`                        | assignees (`assignedUserIds`)      |
| `incident.assigned`           | `INCIDENT_ASSIGNED`                    | the newly assigned user (`userId`) |

A notification with a null `recipientId` is a broadcast visible to everyone;
assignee-targeted events fan out to one personal notification per assignee. The
acting user (`actorId` on the event) is never notified about their own action:
fan-outs skip the actor at write time, broadcasts are filtered at read time. In
line with the thin NATS event shape, the service does not call other services:
it uses only the fields carried on each event. It deliberately does not subscribe
to `incident.updated` (no notification-worthy change) or `incident.resolved`
(resolution already arrives as `incident.status.changed` with `newStatus=resolved`).

If NATS is unreachable at startup the service logs a warning and runs in degraded
mode: the REST API keeps serving already-stored notifications and no new events are
consumed. Set `nats.failOnStartup=true` to abort startup instead.

## Endpoints

| Method | Path                       | Description                                                                  |
| ------ | -------------------------- | ---------------------------------------------------------------------------- |
| `GET`  | `/health`                  | Health check (OpenAPI `healthCheck`)                                         |
| `GET`  | `/notifications`           | List for the calling user, newest first. Query: `unreadOnly`, `page`, `size` |
| `POST` | `/notifications/{id}/read` | Mark one read (`204`, or `404` if unknown or not visible)                    |
| `POST` | `/notifications/read-all`  | Mark all visible to the caller read; returns `204`                           |

All endpoints are scoped to the calling user, identified by the `X-User-Id`
header the gateway injects after validating the session (ADR 0007); a missing
or malformed header is a `400`. `GET /notifications` returns items plus `total`
and `unreadCount`, so the frontend can render an unread badge in one request.

The endpoints are defined in `api/openapi.yaml` (tag `notifications`) and the
controller implements the generated `NotificationsApi`, so the spec is the single
source of truth. The gateway proxies them under `/api/v1/notifications*`
(`NotificationsProxyController` in `services/gateway`).

## Read state

Read/unread is tracked per user in the `notification_reads` table (one mark per
notification and reader), so a broadcast marked read by one user stays unread for
everyone else. Mark-read is idempotent (`ON CONFLICT DO NOTHING`).

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
