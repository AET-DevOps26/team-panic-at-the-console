## Incident Management System - Subsystem Decomposition Diagram

```mermaid
C4Container

  Person(users, "Users", "Incident Responders, Commanders, Team Members")

  Container_Boundary(system, "Incident Management System") {
    Container(frontend, "Frontend Dashboard", "Vue.js", "Web UI for incident management, real-time updates, timeline view, collaboration features")

    Container_Boundary(backend, "Backend Services") {
      Container(incident_service, "Incident Service", "REST API", "CRUD operations, lifecycle management (open → investigating → resolved), status & assignment updates")

      Container(rule_engine, "Rule Engine", "Business Logic", "Webhook evaluation, incident auto-creation rules, severity escalation logic")
      Container(genai_service, "GenAI Service", "Python/LLM", "Incident summaries, severity suggestions, solution proposals, postmortem drafts")
      Container(event_log, "Event Log Service", "REST API", "Append-only audit log, immutable event storage, timeline data")
    }

    Container(notification, "Notification Service", "Event Handler", "Sends alerts for incident creation, status changes, severity escalation")

    Container_Boundary(db, "DB") {
      ContainerDb(database, "PostgreSQL Database", "Relational DB", "Incident data, user accounts, configuration, event log storage")
    }
  }

  Container_Boundary(ext, "Data Sources"){
    System_Ext(external_systems, "External Systems", "GitHub Actions pipelines, logging services, other webhook sources")
  }

  Rel(users, frontend, "Interacts with", "HTTPS")
  Rel(frontend, incident_service, "All incident operations", "REST API")

  Rel(external_systems, event_log, "Sends event logs", "JSON/HTTPS")
  Rel(rule_engine, incident_service, "Auto-creates incidents", "REST API")

  Rel(incident_service, event_log, "Records/reads events", "REST API")
  Rel(incident_service, genai_service, "Requests analysis", "REST API")
  Rel(incident_service, notification, "Triggers notifications", "Events")

  Rel(event_log, database, "Reads/writes", "JDBC")
  Rel(event_log, rule_engine, "Triggers analysis of new events", "REST API")

  Rel(genai_service, event_log, "Reads event log", "REST API")
  Rel(genai_service, database, "Reads metadata", "JDBC")

  Rel(notification, users, "Sends alerts", "Email/Push")

  UpdateRelStyle(users, frontend, $offsetX="-50",$offsetY="40")
  UpdateRelStyle(frontend, incident_service, $offsetY="20")
  UpdateRelStyle(rule_engine, incident_service, $offsetX="-60", $offsetY="-30")
  UpdateRelStyle(incident_service, notification, $offsetX="60", $offsetY="-50")
  UpdateRelStyle(event_log, database, $offsetX="-40", $offsetY="-10")
  UpdateRelStyle(genai_service, event_log, $offsetX="-40", $offsetY="-30")
  UpdateRelStyle(external_systems, event_log, $offsetY="-70")
  UpdateRelStyle(genai_service, database, $offsetX="15", $offsetY="-20")
  UpdateRelStyle(notification, users, $offsetY="-60")

  UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```
