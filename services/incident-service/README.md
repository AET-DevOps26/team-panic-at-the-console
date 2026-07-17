# incident-service

Core incident CRUD + lifecycle. Owns the `incidents` database and is the single source of truth for incident state, including AI-generated content (summary, severity suggestion, solutions, postmortem) produced by `genai-service`.

Port: **8081**

## Endpoints

| Method | Path                               | Description                                               |
| ------ | ---------------------------------- | --------------------------------------------------------- |
| `POST` | `/incidents/{id}/genai/summary`    | Trigger summary regeneration                              |
| `POST` | `/incidents/{id}/genai/severity`   | Trigger severity suggestion regeneration                  |
| `POST` | `/incidents/{id}/genai/solutions`  | Trigger solution suggestions regeneration                 |
| `POST` | `/incidents/{id}/genai/postmortem` | Trigger postmortem regeneration (resolved incidents only) |

All regen endpoints return `202 Accepted` and publish `incident.regen.requested` with a `task` field (`SUMMARY`, `SEVERITY_SUGGESTION`, `SOLUTION_SUGGESTIONS`, or `POSTMORTEM`). Postmortem regen requires the incident to be resolved (`409` otherwise). Internal read/write-back paths used by genai-service are not routed through the gateway.

## Webhook-driven incident creation

The incident service also listens for `external.event.received` messages published by the webhook service. For a simple MVP rule set, it evaluates incoming events and creates a `SEV2` incident when the event type or payload looks failure-like (for example `ci_failure`, `failure`, `error`, or a payload containing `conclusion: failure`). Duplicate deliveries are ignored to avoid duplicate incidents.

This is intentionally lightweight and easy to trial; it is not yet a full rule engine with configurable policies or a UI.

## Local dev

```bash
cd services/incident-service
pixi install
pixi run test
pixi run start
```

From the repo root:

```bash
pixi run --manifest-path services/incident-service/pixi.toml test
```
