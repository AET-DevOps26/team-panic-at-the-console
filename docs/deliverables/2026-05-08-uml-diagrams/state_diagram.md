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

Severity is an attribute of the Incident, not a status. It can change in any state.

**Two sources of severity change:**

- **Automatic (rule-engine)**: detects repeated `external.event.received` events from the same source within a time window → publishes `incident.severity.escalate.requested` to NATS → incident-service raises severity one level
- **Manual (Commander)**: overrides severity directly via REST

**Note:** genai-service also suggests a severity level (displayed as advisory in the UI) but does NOT change the actual severity — that is rule-engine or Commander only.

```mermaid
stateDiagram-v2
    direction LR

    [*] --> SEV_RULE: incident created\n(severity set by matching rule action)

    SEV_RULE --> SEV4 : rule action = SEV4
    SEV_RULE --> SEV3 : rule action = SEV3
    SEV_RULE --> SEV2 : rule action = SEV2
    SEV_RULE --> SEV1 : rule action = SEV1

    SEV4 --> SEV3: escalate (rule-engine auto or Commander)
    SEV3 --> SEV2: escalate (rule-engine auto or Commander)
    SEV2 --> SEV1: escalate (rule-engine auto or Commander)

    SEV3 --> SEV4: de-escalate (Commander)
    SEV2 --> SEV3: de-escalate (Commander)
    SEV1 --> SEV2: de-escalate (Commander)
```
