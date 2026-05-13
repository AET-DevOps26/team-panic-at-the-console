## Incident Management System - Subsystem Decomposition Diagram

```mermaid
C4Container

  Person(users, "Users", "Incident Responders, Commanders, Team Members")

  Container_Boundary(system, "Incident Management System") {
    Container(frontend, "Frontend Dashboard", "React + Vite + TypeScript", "Web UI for incident management, real-time updates via SSE, timeline view, collaboration features")
    Container(notification, "Notification Service", "Spring Boot", "Stores in-app notifications; subscribes to NATS incident events")

    Container_Boundary(backend, "Backend Services") {
      Container(gateway, "Gateway", "Spring Boot", "Validates JWT cookie, injects user headers, routes requests, fans out SSE to clients")
      Container(user_service, "User Service", "Spring Boot", "User profiles, authentication, JWT issuance")

      Container(incident_service, "Incident Service", "Spring Boot", "CRUD operations, lifecycle management (open → investigating → resolved), owns AI results")
      Container(rule_engine, "Rule Engine", "Spring Boot", "Evaluates external events against rules, triggers incident creation and severity escalation")

      Container(genai_service, "GenAI Service", "Python + FastAPI", "Stateless: subscribes to NATS events, calls Ollama, patches AI results back to incident-service")
      Container(event_log, "Event Log Service", "Spring Boot", "Append-only audit log, immutable event storage, timeline data")
      Container(nats, "NATS JetStream", "Message Bus", "Async event bus for side effects: event log writes, notifications, genai triggers, SSE fan-out")
      Container(ollama, "Ollama", "qwen2.5:3b", "Local LLM inference, no cloud dependency")

      ContainerDb(database, "PostgreSQL Database", "Relational DB", "Shared instance: one database per stateful service (incidents, events, users, notifications, rules)")
      Container(webhook, "Webhook Service", "Spring Boot", "Receives and normalizes data from external sources")
    }
  }

  Container_Boundary(ext, "Data Sources"){
    System_Ext(external_systems, "External Systems", "GitHub Actions pipelines, logging services, other webhook sources")
  }

  Rel(users, frontend, "Interacts with", "HTTPS + SSE")
  Rel(frontend, gateway, "All incident operations", "REST API")
  Rel(gateway, incident_service, "Validated incident requests", "REST API")
  Rel(gateway, user_service, "Login / user management calls", "REST API")
  Rel(gateway, nats, "Subscribes for SSE fan-out", "NATS")

  Rel(external_systems, webhook, "Sends webhook events", "JSON/HTTPS")
  Rel(webhook, nats, "Publishes external.event.received", "NATS")

  Rel(nats, rule_engine, "external.event.received", "NATS")
  Rel(nats, event_log, "incident.* events", "NATS")
  Rel(nats, notification, "incident.* events", "NATS")
  Rel(nats, genai_service, "incident.created / incident.resolved / incident.regen.requested", "NATS")

  Rel(rule_engine, nats, "Publishes incident.create.requested / incident.severity.escalate.requested", "NATS")
  Rel(nats, incident_service, "incident.create.requested / incident.severity.escalate.requested", "NATS")
  Rel(incident_service, nats, "Publishes incident.* events", "NATS")

  Rel(genai_service, incident_service, "PATCHes AI results", "REST API")
  Rel(genai_service, ollama, "Generate summary / postmortem", "REST API")

  Rel(event_log, database, "Reads/Writes", "JDBC")
  Rel(incident_service, database, "Reads/Writes", "JDBC")
  Rel(user_service, database, "Reads/Writes", "JDBC")
  Rel(notification, database, "Reads/Writes", "JDBC")
  Rel(rule_engine, database, "Reads/Writes", "JDBC")

  UpdateRelStyle(gateway, incident_service, $offsetX="-50", $offsetY="-20")
  UpdateRelStyle(gateway, user_service, $offsetX="-40", $offsetY="-40")
  UpdateRelStyle(users, frontend, $offsetX="-50", $offsetY="40")
  UpdateRelStyle(frontend, gateway, $offsetX="10", $offsetY="20")
  UpdateRelStyle(event_log, database, $offsetX="-40", $offsetY="-10")

  UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```
