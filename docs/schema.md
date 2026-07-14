# Service schema snapshot

This document is a snapshot of the persistence schema currently expressed in the JPA entities under the backend services. It is intended as a reference for the incident domain, event log, user accounts, notifications, and the persistent storage layer required by the project specification.

The schema is part of the documented data model for the system's relational persistence layer and complements the REST-based microservice architecture.

## Overview

| Service | Primary entity | Storage note |
| --- | --- | --- |
| incident-service | Incident | Stored in the `incidents` table |
| event-service | TimelineEvent | Stored in the `timeline_events` table |
| user-service | UserAccount | Stored in the `users` table |
| notification-service | Notification | Stored in the `notifications` table |

## Incidents

Source: [services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Incident.java](../services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Incident.java)

### Incident

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key |
| status | IncidentStatus | `OPEN`, `INVESTIGATING`, `RESOLVED` |
| severity | Severity | `SEV1` to `SEV4` |
| createdAt | Instant | Creation timestamp |
| updatedAt | Instant | Updated on state or content changes |
| resolvedAt | Instant | Set when incident is resolved |
| sourceId | UUID | Reference to the triggering external event, if any |
| title | String | Auto-generated or user supplied |
| description | String | Human-written incident description |
| summary | String | AI-generated summary |
| summaryGeneratedAt | Instant | When the summary was last generated |
| severitySuggestion | String | AI-generated severity suggestion |
| severitySuggestionGeneratedAt | Instant | When the severity suggestion was generated |
| solutions | String | AI-generated solution suggestions |
| solutionsGeneratedAt | Instant | When solutions were generated |
| postmortem | String | AI-generated postmortem, intended for resolved incidents |
| postmortemGeneratedAt | Instant | When the postmortem was generated |
| assignedUsers | Set<UUID> | Stored in the `incident_assigned_users` join table |
| comments | List<Comment> | One-to-many relation to immutable comments |

### Comment

Source: [services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Comment.java](../services/incident-service/src/main/java/com/panicattheconsole/incidentservice/incident/Comment.java)

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key |
| incident | Incident | Many-to-one relation |
| authorId | UUID | ID of the comment author |
| content | String | Full comment body |
| createdAt | Instant | Creation timestamp |

## Events

Source: [services/event-service/src/main/java/com/panicattheconsole/eventservice/domain/TimelineEvent.java](../services/event-service/src/main/java/com/panicattheconsole/eventservice/domain/TimelineEvent.java)

### TimelineEvent

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key |
| incidentId | UUID | Incident the event belongs to |
| eventType | String | Type of the timeline event |
| eventTimestamp | Instant | Timestamp of the event |
| payload | JsonNode | JSON payload persisted as `jsonb` |

This is the append-only event log for incidents and is stored in the `timeline_events` table.

## Users

Source: [services/user-service/src/main/java/com/panicattheconsole/userservice/users/UserAccount.java](../services/user-service/src/main/java/com/panicattheconsole/userservice/users/UserAccount.java)

### UserAccount

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key |
| email | String | Unique, stored lowercased |
| displayName | String | Human-readable display name |
| passwordHash | String | Password hash; never exposed outside the user service |
| role | UserRole | `MEMBER`, `RESPONDER`, or `COMMANDER` |
| createdAt | Instant | Creation timestamp |

## Notifications

Source: [services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/Notification.java](../services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/Notification.java)

### Notification

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | Primary key |
| incidentId | UUID | Related incident |
| type | NotificationType | `INCIDENT_CREATED`, `SEVERITY_ESCALATED`, `STATUS_CHANGED`, `INCIDENT_RESOLVED`, `COMMENT_ADDED`, `INCIDENT_ASSIGNED` |
| recipientId | UUID | Target user for personal notifications; `null` for broadcasts |
| actorId | UUID | User whose action triggered the notification; `null` for machine events |
| message | String | Notification message body |
| createdAt | Instant | Creation timestamp |

### NotificationRead

Source: [services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/NotificationRead.java](../services/notification-service/src/main/java/com/panicattheconsole/notificationservice/domain/NotificationRead.java)

| Field | Type | Notes |
| --- | --- | --- |
| notificationId | UUID | Part of composite key |
| userId | UUID | Part of composite key |
| readAt | Instant | Timestamp when the user read the notification |

