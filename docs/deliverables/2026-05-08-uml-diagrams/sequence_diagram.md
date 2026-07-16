## Incident Management System - Incident Creation and Asynchronous Processing

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend
    participant GW as Gateway
    participant IS as incident-service
    participant DB as incidents DB
    participant NATS as NATS JetStream
    participant ES as event-service
    participant NS as notification-service
    participant GS as genai-service
    participant LLM as Ollama or TUM Logos

    User->>FE: Create incident
    FE->>GW: POST /api/v1/incidents
    GW->>IS: Proxied REST request with identity headers
    IS->>DB: Persist incident
    IS->>NATS: Publish incident.created
    IS-->>GW: 201 Created
    GW-->>FE: Incident response

    par Event log
        NATS->>ES: incident.created
        ES->>ES: Append immutable timeline event
    and Notifications
        NATS->>NS: incident.created
        NS->>NS: Store in-app notification
    and AI analysis
        NATS->>GS: incident.created
        GS->>IS: GET incident and timeline context
        IS-->>GS: Incident data
        GS->>LLM: Generate summary, severity suggestion, solutions
        LLM-->>GS: Structured result
        GS->>NATS: Publish incident.genai.*.generated
        NATS->>IS: Generated AI result
        IS->>DB: Store generated fields
        IS->>NATS: Publish incident.updated
    and Live UI update
        NATS->>GW: incident.created and incident.updated
        GW-->>FE: Server-Sent Event
    end
```

Webhook ingestion follows a separate path: `webhook-service` persists an external event and publishes `external.event.received`. The event is retained for audit; automatic rule evaluation is not active while the legacy `rule-engine` placeholder remains.
