## Incident Management System - Webhook-driven Incident Processing

```mermaid
sequenceDiagram
    participant EXT as External system
    participant WH as webhook-service
    participant NATS as NATS JetStream
    participant IS as incident-service
    participant DB as incidents DB
    participant ES as event-service
    participant NS as notification-service
    participant GS as genai-service
    participant LLM as Ollama or TUM Logos
    participant GW as Gateway
    participant FE as Frontend

    EXT->>WH: POST signed webhook
    WH->>WH: Persist ExternalEvent
    WH->>NATS: Publish external.event.received
    NATS->>IS: external.event.received
    IS->>IS: Evaluate failure-like event type and payload

    alt New failure-like event
        IS->>DB: Persist ProcessedExternalEvent and SEV2 Incident
        IS->>NATS: Publish incident.created
        par Event log
            NATS->>ES: incident.created
            ES->>ES: Append immutable timeline event
        and Notifications
            NATS->>NS: incident.created
            NS->>NS: Store in-app notification
        and AI analysis
            NATS->>GS: incident.created
            GS->>IS: GET incident context
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
    else Duplicate or non-failure event
        IS->>DB: Record processed event or skip duplicate
        Note over IS: No incident is created
    end
```

The embedded rule is deliberately small: it detects failure-like event types or payload values and creates a `SEV2` incident. It is not yet a configurable rule-policy engine.
