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

All regen endpoints return `202 Accepted`. Persistence, NATS publishing (`incident.regen.requested`), and state validation (e.g. postmortem requires resolved status) are not yet implemented; they will land in follow-up PRs that introduce the relevant deps.

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
