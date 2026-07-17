# Service schema snapshot

This document is a snapshot of the persistence schema currently expressed in the JPA entities under the backend services. It is intended as a reference for the incident domain, event log, user accounts, notifications, and the persistent storage layer required by the project specification.

The schema is part of the documented data model for the system's relational persistence layer and complements the REST-based microservice architecture.

## Overview

| Service              | Primary entity | Storage note                          |
| -------------------- | -------------- | ------------------------------------- |
| incident-service     | Incident       | Stored in the `incidents` table       |
| event-service        | TimelineEvent  | Stored in the `timeline_events` table |
| user-service         | UserAccount    | Stored in the `users` table           |
| notification-service | Notification   | Stored in the `notifications` table   |

## Incidents

Source: [services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Incident.java](../services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Incident.java)

### Incident

| Field                         | Type           | Notes                                                    |
| ----------------------------- | -------------- | -------------------------------------------------------- |
| id                            | UUID           | Primary key                                              |
| status                        | IncidentStatus | `OPEN`, `INVESTIGATING`, `RESOLVED`                      |
| severity                      | Severity       | `SEV1` to `SEV4`                                         |
| createdAt                     | Instant        | Creation timestamp                                       |
| updatedAt                     | Instant        | Updated on state or content changes                      |
| resolvedAt                    | Instant        | Set when incident is resolved                            |
| sourceId                      | UUID           | Reference to the triggering external event, if any       |
| title                         | String         | Auto-generated or user supplied                          |
| description                   | String         | Human-written incident description                       |
| summary                       | String         | AI-generated summary                                     |
| summaryGeneratedAt            | Instant        | When the summary was last generated                      |
| severitySuggestion            | String         | AI-generated severity suggestion                         |
| severitySuggestionGeneratedAt | Instant        | When the severity suggestion was generated               |
| solutions                     | String         | AI-generated solution suggestions                        |
| solutionsGeneratedAt          | Instant        | When solutions were generated                            |
| postmortem                    | String         | AI-generated postmortem, intended for resolved incidents |
| postmortemGeneratedAt         | Instant        | When the postmortem was generated                        |
| assignedUsers                 | Set<UUID>      | Stored in the `incident_assigned_users` join table       |
| comments                      | List<Comment>  | One-to-many relation to immutable comments               |

### Comment

Source: [services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Comment.java](../services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Comment.java)

| Field     | Type     | Notes                    |
| --------- | -------- | ------------------------ |
| id        | UUID     | Primary key              |
| incident  | Incident | Many-to-one relation     |
| authorId  | UUID     | ID of the comment author |
| content   | String   | Full comment body        |
| createdAt | Instant  | Creation timestamp       |

## Events

Source: [services/event-service/src/main/java/com/panicattheconsole/eventservice/domain/TimelineEvent.java](../services/event-service/src/main/java/com/panicattheconsole/eventservice/domain/TimelineEvent.java)

### TimelineEvent

| Field          | Type     | Notes                             |
| -------------- | -------- | --------------------------------- |
| id             | UUID     | Primary key                       |
| incidentId     | UUID     | Incident the event belongs to     |
| eventType      | String   | Type of the timeline event        |
| eventTimestamp | Instant  | Timestamp of the event            |
| payload        | JsonNode | JSON payload persisted as `jsonb` |

This is the append-only event log for incidents and is stored in the `timeline_events` table.

## Users

Source: [services/user-service/src/main/java/com/panicattheconsole/userservice/users/UserAccount.java](../services/user-service/src/main/java/com/panicattheconsole/userservice/users/UserAccount.java)

### UserAccount

| Field        | Type     | Notes                                                 |
| ------------ | -------- | ----------------------------------------------------- |
| id           | UUID     | Primary key                                           |
| email        | String   | Unique, stored lowercased                             |
| displayName  | String   | Human-readable display name                           |
| passwordHash | String   | Password hash; never exposed outside the user service |
| role         | UserRole | `MEMBER`, `RESPONDER`, or `COMMANDER`                 |
| createdAt    | Instant  | Creation timestamp                                    |

## Notifications

Source: [services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/Notification.java](../services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/Notification.java)

### Notification

| Field       | Type             | Notes                                                                                                                 |
| ----------- | ---------------- | --------------------------------------------------------------------------------------------------------------------- |
| id          | UUID             | Primary key                                                                                                           |
| incidentId  | UUID             | Related incident                                                                                                      |
| type        | NotificationType | `INCIDENT_CREATED`, `SEVERITY_ESCALATED`, `STATUS_CHANGED`, `INCIDENT_RESOLVED`, `COMMENT_ADDED`, `INCIDENT_ASSIGNED` |
| recipientId | UUID             | Target user for personal notifications; `null` for broadcasts                                                         |
| actorId     | UUID             | User whose action triggered the notification; `null` for machine events                                               |
| message     | String           | Notification message body                                                                                             |
| createdAt   | Instant          | Creation timestamp                                                                                                    |

### NotificationRead

Source: [services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/NotificationRead.java](../services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/NotificationRead.java)

| Field          | Type    | Notes                                         |
| -------------- | ------- | --------------------------------------------- |
| notificationId | UUID    | Part of composite key                         |
| userId         | UUID    | Part of composite key                         |
| readAt         | Instant | Timestamp when the user read the notification |

## Webhook Service

Source: [services/webhook-service/src/main/java/com/panicattheconsole/webhookservice/event/ExternalEvent.java](../services/webhook-service/src/main/java/com/panicattheconsole/webhookservice/event/ExternalEvent.java)

### ExternalEvent

| Field           | Type    | Notes                                                                                                              |
| --------------- | ------- | ------------------------------------------------------------------------------------------------------------------ |
| id              | UUID    | Primary key                                                                                                        |
| source          | String  | Source system identifier (e.g. GitHub)                                                                             |
| eventType       | String  | Type of the received external event                                                                                |
| deliveryId      | String  | Optional/nullable sender-provided delivery identifier; deduplication is applied only when a delivery ID is present |
| receivedAt      | Instant | Timestamp when the webhook was received                                                                            |
| rawPayload      | String  | Original webhook payload in the application model; persisted as JSONB in the database                              |
| publishedAt     | Instant | Timestamp when the event was successfully published to NATS                                                        |
| publishAttempts | int     | Number of failed publication attempts recorded for the event                                                       |

The `external_events` table stores immutable webhook payloads received from external systems. It provides an auditable record of incoming events and tracks publication to the event bus.

### WebhookSource

Source: [services/webhook-service/src/main/java/com/panicattheconsole/webhookservice/source/WebhookSource.java](../services/webhook-service/src/main/java/com/panicattheconsole/webhookservice/source/WebhookSource.java)

| Field           | Type    | Notes                                                |
| --------------- | ------- | ---------------------------------------------------- |
| slug            | String  | Primary key; unique webhook endpoint identifier      |
| secret          | String  | Shared HMAC secret used to verify webhook signatures |
| createdAt       | Instant | Creation timestamp                                   |
| secretRotatedAt | Instant | Timestamp of the last secret rotation                |

The `webhook_sources` table stores registered webhook endpoints and their associated HMAC secrets used to authenticate incoming webhook requests.

## Rules

Source: [services/incident-service/src/main/java/com/panicattheconsole/incidentservice/rule/Rule.java](../services/incident-service/src/main/java/com/panicattheconsole/incidentservice/rule/Rule.java)

Rules decide which `external.event.received` messages become incidents. They are evaluated in ascending `priority` order and the first enabled rule whose `source` and every condition match creates the incident (first-match-wins). Field paths and templates use dotted notation rooted at an object exposing `source`, `eventType`, and `payload` (the raw webhook body), e.g. `payload.workflow_run.conclusion`.

### Rule

| Field               | Type                      | Notes                                                               |
| ------------------- | ------------------------- | ------------------------------------------------------------------- |
| id                  | UUID                      | Primary key                                                         |
| name                | String                    | Human-readable rule name                                            |
| enabled             | boolean                   | Disabled rules are skipped during evaluation                        |
| priority            | int                       | Lower runs first; the first matching enabled rule wins              |
| source              | String                    | Source slug to scope to; null/blank matches any source              |
| severity            | Severity                  | Severity of incidents this rule creates (SEV1-SEV4)                 |
| titleTemplate       | String                    | Incident title; supports `{{dotted.path}}` placeholders             |
| descriptionTemplate | String                    | Optional leading description (Markdown) with placeholders           |
| dedupKeyTemplate    | String                    | Optional placeholder template; at most one incident per (rule, key) |
| conditions          | List\<RuleCondition\>     | AND-ed match conditions (`rule_conditions` element collection)      |
| metadataFields      | List\<RuleMetadataField\> | Fields rendered into the description (`rule_metadata_fields`)       |
| createdAt           | Instant                   | Creation timestamp                                                  |
| updatedAt           | Instant                   | Last-modified timestamp                                             |

### RuleCondition (embedded in `rule_conditions`)

| Field     | Type              | Notes                                                                          |
| --------- | ----------------- | ------------------------------------------------------------------------------ |
| fieldPath | String            | Dotted path into the event                                                     |
| operator  | ConditionOperator | equals, not_equals, contains, not_contains, matches, in, exists, not_exists    |
| value     | String            | Comparison value (comma-separated list for `in`; ignored by exists/not_exists) |

### RuleMetadataField (embedded in `rule_metadata_fields`)

| Field     | Type   | Notes                                     |
| --------- | ------ | ----------------------------------------- |
| label     | String | Display label in the incident description |
| fieldPath | String | Dotted path into the event                |

### RuleMatchDedup

Source: [services/incident-service/src/main/java/com/panicattheconsole/incidentservice/rule/RuleMatchDedup.java](../services/incident-service/src/main/java/com/panicattheconsole/incidentservice/rule/RuleMatchDedup.java)

| Field     | Type    | Notes                                           |
| --------- | ------- | ----------------------------------------------- |
| id        | UUID    | Primary key (generated)                         |
| ruleId    | UUID    | Rule that produced the incident                 |
| dedupKey  | String  | Rendered dedup key (falls back to the event id) |
| createdAt | Instant | Timestamp when the incident was created         |

The `rule_match_dedup` table enforces at most one incident per `(rule_id, dedup_key)` via a unique constraint. This is what collapses a whole pipeline run (many deliveries sharing e.g. `payload.workflow_run.id`) into a single incident, and provides idempotency against redelivered events.
