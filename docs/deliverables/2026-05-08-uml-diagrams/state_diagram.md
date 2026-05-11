## Incident Management System - Incident Lifecycle State Machine

Shows valid status transitions and the side effects triggered at each state change.

```mermaid
stateDiagram-v2
    direction LR

    [*] --> OPEN: created (manual or auto via rule-engine)

    OPEN --> INVESTIGATING: Commander updates status
    OPEN --> RESOLVED: Commander marks resolved directly

    INVESTIGATING --> RESOLVED: Commander marks resolved

    RESOLVED --> [*]

    note right of OPEN
        On entry:
        - genai-service generates Summary + Solutions
        - notification-service notifies users
        - Event appended to Event Log
    end note

    note right of INVESTIGATING
        On entry:
        - status change recorded in Event Log
        - Severity may be escalated by rule-engine
          at any point (independent of status)
    end note

    note right of RESOLVED
        On entry:
        - genai-service generates Postmortem
        - resolution recorded in Event Log
    end note
```

## Severity (independent of status)

Severity is an attribute of the Incident, not a status. It can change in any state:

```mermaid
stateDiagram-v2
    direction LR

    [*] --> SEV4: incident created (default)

    SEV4 --> SEV3: rule-engine escalates
    SEV3 --> SEV2: rule-engine escalates
    SEV2 --> SEV1: rule-engine escalates

    SEV3 --> SEV4: Commander de-escalates
    SEV2 --> SEV3: Commander de-escalates
    SEV1 --> SEV2: Commander de-escalates
```
