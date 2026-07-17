# External events remain auditable and can create incidents

External events are persisted by `webhook-service` before publication to NATS. `incident-service` consumes `external.event.received` and evaluates the embedded failure-like classifier.

The canonical external-event identifier is `ExternalEvent.id` (`UUID`). Documentation calls it `externalEventId`; the current NATS payload and incident persistence field name it `sourceId`. Both carry the same External Event UUID.

- A new failure-like event creates one `SEV2` incident and stores its `externalEventId`.
- Duplicate deliveries do not create another incident because `ProcessedExternalEvent.externalEventId` is recorded.
- Non-failure events remain available through the external-event audit API without creating an incident.

Automatic linking of follow-up events to existing incidents is out of scope. Operators can retain the event for audit and link context manually when needed.
