## Incident Management System - Subsystem Decomposition Diagram

```mermaid
flowchart LR
    user["Users"] --> frontend["Frontend dashboard<br/>React + Vite"]
    frontend -->|REST and SSE| gateway["Gateway<br/>Spring Boot"]

    gateway -->|REST| incident["incident-service<br/>Incident state, AI results, and webhook rules"]
    gateway -->|REST| events["event-service<br/>Append-only timeline"]
    gateway -->|REST| users["user-service<br/>Authentication and roles"]
    gateway -->|REST| notifications["notification-service<br/>In-app notifications"]
    gateway -->|REST| webhooks["webhook-service<br/>Webhook sources and events"]

    external["External systems"] -->|Webhook| webhooks
    webhooks --> webhookDb[("webhooks DB")]
    webhooks -->|external.event.received| nats["NATS JetStream"]
    nats -->|external.event.received| incident

    incident --> incidentDb[("incidents DB")]
    events --> eventsDb[("events DB")]
    users --> usersDb[("users DB")]
    notifications --> notificationsDb[("notifications DB")]

    incident -->|publishes: incident.created, incident.updated,<br/>incident.status.changed, incident.resolved, incident.assigned,<br/>incident.comment.added, incident.severity.escalated,<br/>incident.regen.requested| nats
    nats -->|incident.created, incident.updated, incident.status.changed,<br/>incident.resolved, incident.assigned, incident.comment.added,<br/>incident.severity.escalated, incident.regen.requested| events
    nats -->|incident.created, incident.updated, incident.status.changed,<br/>incident.resolved, incident.assigned, incident.comment.added,<br/>incident.severity.escalated, incident.regen.requested| notifications
    nats -->|incident.created, incident.resolved,<br/>incident.regen.requested| genai["genai-service<br/>FastAPI"]
    genai -->|publishes: incident.genai.summary.generated,<br/>incident.genai.severity.generated, incident.genai.solutions.generated,<br/>incident.genai.postmortem.generated| nats
    nats -->|incident.genai.summary.generated, incident.genai.severity.generated,<br/>incident.genai.solutions.generated, incident.genai.postmortem.generated| incident
    nats -->|incident.created, incident.updated, incident.status.changed,<br/>incident.resolved, incident.assigned, incident.comment.added,<br/>incident.severity.escalated, incident.regen.requested| gateway
    gateway -->|SSE fan-out| frontend

    genai -->|GET/PATCH incident data| incident
    genai -->|Generation| llm["Ollama locally<br/>TUM Logos in Kubernetes"]

    classDef app fill:#e3f2fd,stroke:#1565c0,color:#000
    classDef data fill:#fff3e0,stroke:#ef6c00,color:#000
    classDef bus fill:#e8f5e9,stroke:#2e7d32,color:#000
    class frontend,gateway,incident,events,users,notifications,webhooks,genai app
    class incidentDb,eventsDb,usersDb,notificationsDb,webhookDb data
    class nats bus
```

REST serves request/response flows. NATS JetStream carries asynchronous side effects and gateway fan-out for real-time updates. `incident-service` also consumes `external.event.received`, evaluates the embedded failure-like rule, and deduplicates processed event IDs before creating a `SEV2` incident. Each stateful service owns its database; `incident-service` owns the current incident state and generated AI fields.
