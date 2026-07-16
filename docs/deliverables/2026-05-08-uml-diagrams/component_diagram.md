## Incident Management System - Subsystem Decomposition Diagram

```mermaid
flowchart LR
    user["Users"] --> frontend["Frontend dashboard<br/>React + Vite"]
    frontend -->|REST and SSE| gateway["Gateway<br/>Spring Boot"]

    gateway -->|REST| incident["incident-service<br/>Incident state and AI results"]
    gateway -->|REST| events["event-service<br/>Append-only timeline"]
    gateway -->|REST| users["user-service<br/>Authentication and roles"]
    gateway -->|REST| notifications["notification-service<br/>In-app notifications"]
    gateway -->|REST| webhooks["webhook-service<br/>Webhook sources and events"]

    external["External systems"] -->|Webhook| webhooks
    webhooks --> webhookDb[("webhooks DB")]
    webhooks -->|external.event.received| nats["NATS JetStream"]

    incident --> incidentDb[("incidents DB")]
    events --> eventsDb[("events DB")]
    users --> usersDb[("users DB")]
    notifications --> notificationsDb[("notifications DB")]

    incident -->|incident.* events| nats
    nats -->|incident.* events| events
    nats -->|incident.* events| notifications
    nats -->|incident.created, resolved,<br/>regen.requested| genai["genai-service<br/>FastAPI"]
    nats -->|incident.* events| gateway
    gateway -->|SSE fan-out| frontend

    genai -->|GET/PATCH incident data| incident
    genai -->|Generation| llm["Ollama locally<br/>TUM Logos in Kubernetes"]

    legacy["rule-engine<br/>Legacy placeholder: no application behavior"]
    nats -.->|external.event.received<br/>currently unconsumed| legacy

    classDef app fill:#e3f2fd,stroke:#1565c0,color:#000
    classDef data fill:#fff3e0,stroke:#ef6c00,color:#000
    classDef bus fill:#e8f5e9,stroke:#2e7d32,color:#000
    classDef legacy fill:#ffebee,stroke:#c62828,color:#000
    class frontend,gateway,incident,events,users,notifications,webhooks,genai app
    class incidentDb,eventsDb,usersDb,notificationsDb,webhookDb data
    class nats bus
    class legacy legacy
```

REST serves request/response flows. NATS JetStream carries asynchronous side effects and gateway fan-out for real-time updates. Each stateful service owns its database; `incident-service` owns the current incident state and generated AI fields.
