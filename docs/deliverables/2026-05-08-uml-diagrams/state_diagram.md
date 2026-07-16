## Incident Management System - Incident Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> OPEN: manual creation or NATS create request

    OPEN --> INVESTIGATING: responder updates status
    OPEN --> RESOLVED: responder resolves
    INVESTIGATING --> RESOLVED: responder resolves
    RESOLVED --> [*]

    note right of OPEN
        incident.created is published to NATS.
        It triggers the event log, notifications,
        GenAI analysis, and gateway SSE fan-out.
    end note

    note right of INVESTIGATING
        incident.status.changed is published to NATS
        and appended to the immutable timeline.
    end note

    note right of RESOLVED
        incident.status.changed and incident.resolved
        are published. GenAI generates a postmortem.
    end note
```

Severity is independent of lifecycle state. Responders can set it manually; each change publishes `incident.severity.escalated` and creates a timeline event and notification.
