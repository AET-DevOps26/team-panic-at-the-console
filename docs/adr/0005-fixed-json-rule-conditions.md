# Lightweight webhook failure evaluation

**Status:** Supersedes the earlier fixed JSON rule-condition proposal.

`ExternalEventRuleService` in `incident-service` evaluates `external.event.received` messages. It creates a `SEV2` incident when the normalized event type or raw payload contains failure-like values, and records processed external event IDs to prevent duplicates.

The project does not provide configurable policies, an expression language, or a rule-management UI. Those features would add parser, sandboxing, validation, and user-interface complexity beyond the current incident-management workflow.
