# Event Log is a secondary audit trail, not the primary write model

Despite the name "immutable event log," the system does NOT use event sourcing. `incident-service` owns Incident state in its own Postgres table and is the source of truth. On every state change, it publishes a thin NATS event; `event-service` subscribes and appends a record to the Event Log. The Event Log is a derived read model used for the timeline UI and audit trail.

The alternative was true event sourcing: every state change is an event appended to the Event Log; current state is reconstructed by replaying events. We rejected this because event sourcing adds significant complexity (projections, replay, eventual consistency) that the course project timeline cannot absorb, and the user-facing requirements ("immutable timeline," "audit trail") are fully satisfied by the secondary audit trail approach. A reader seeing "append-only event log" should not infer event sourcing.
