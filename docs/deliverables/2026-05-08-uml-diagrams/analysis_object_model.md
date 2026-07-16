## Incident Management System - Analysis Object Model

```mermaid
classDiagram
    class Incident {
        +UUID id
        +IncidentStatus status
        +Severity severity
        +UUID sourceId
        +String title
        +String description
        +String summary
        +String severitySuggestion
        +String solutions
        +String postmortem
        +Instant createdAt
        +Instant updatedAt
        +Instant resolvedAt
    }

    class Comment {
        +UUID id
        +UUID authorId
        +String content
        +Instant createdAt
    }

    class TimelineEvent {
        +UUID id
        +UUID incidentId
        +String eventType
        +Json payload
        +Instant eventTimestamp
    }

    class UserAccount {
        +UUID id
        +String email
        +String displayName
        +UserRole role
    }

    class Notification {
        +UUID id
        +UUID incidentId
        +UUID recipientId
        +UUID actorId
        +NotificationType type
        +String message
        +Instant createdAt
    }

    class ExternalEvent {
        +UUID id
        +UUID sourceId
        +String eventType
        +Json rawPayload
        +Instant receivedAt
        +Instant publishedAt
    }

    class ProcessedExternalEvent {
        +UUID id
        +String externalEventId
        +Instant processedAt
    }

    class WebhookSource {
        +UUID id
        +String name
        +String sourceType
        +Boolean active
    }

    class IncidentStatus {
        <<enumeration>>
        OPEN
        INVESTIGATING
        RESOLVED
    }

    class Severity {
        <<enumeration>>
        SEV1
        SEV2
        SEV3
        SEV4
    }

    class UserRole {
        <<enumeration>>
        MEMBER
        RESPONDER
        COMMANDER
    }

    Incident "1" *-- "0..*" Comment : has
    Incident "1" --> "0..*" TimelineEvent : produces
    Incident "0..*" --> "0..*" UserAccount : assigned users
    Incident "1" --> "0..*" Notification : concerns
    Comment "*" --> "1" UserAccount : author
    Notification "*" --> "0..1" UserAccount : recipient
    WebhookSource "1" --> "0..*" ExternalEvent : receives
    ExternalEvent "0..1" --> "0..1" Incident : source of
    ProcessedExternalEvent --> ExternalEvent : deduplicates
    Incident --> IncidentStatus
    Incident --> Severity
    UserAccount --> UserRole
```

`incident-service` owns Incident, Comment, and ProcessedExternalEvent. `ProcessedExternalEvent` prevents duplicate webhook deliveries from creating multiple incidents. `event-service`, `user-service`, `notification-service`, and `webhook-service` own their respective models. The diagram shows domain relationships; services exchange identifiers and events instead of sharing a database.
