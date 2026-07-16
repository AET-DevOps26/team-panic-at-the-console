## Incident Management System - Use Case Diagram

```mermaid
flowchart LR
    member["Member"]
    responder["Responder"]
    commander["Commander"]
    external["External system"]

    subgraph system["Incident Management System"]
        browse["Browse incidents and timeline"]
        comment["Add immutable comment"]
        create["Create incident"]
        update["Update status, severity, and assignment"]
        ai["Request summary, severity suggestion,<br/>solutions, or postmortem"]
        notifications["Read notifications"]
        sources["Manage webhook sources"]
        ingest["Ingest signed webhook"]
    end

    member --> browse
    member --> comment
    member --> notifications

    responder --> browse
    responder --> comment
    responder --> create
    responder --> update
    responder --> ai
    responder --> notifications

    commander --> browse
    commander --> comment
    commander --> create
    commander --> update
    commander --> ai
    commander --> notifications
    commander --> sources

    external --> ingest
```
