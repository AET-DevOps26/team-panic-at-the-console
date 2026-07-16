# External events remain auditable and can create incidents

External events are persisted by `webhook-service` before publication to NATS. `incident-service` consumes `external.event.received` and evaluates the embedded failure-like classifier.

- A new failure-like event creates one `SEV2` incident and links its `sourceId` to the triggering External Event.
- Duplicate deliveries do not create another incident because processed event IDs are recorded.
- Non-failure events remain available through the external-event audit API without creating an incident.

Automatic linking of follow-up events to existing incidents is out of scope. Operators can retain the event for audit and link context manually when needed.
