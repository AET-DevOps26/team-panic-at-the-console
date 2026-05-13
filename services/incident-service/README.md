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

All regen endpoints return `202 Accepted`. NATS publishing (`incident.regen.requested`) and state validation (e.g. postmortem requires resolved status) are not yet implemented.

## Local dev

Requires Postgres on `localhost:5432` (database `incidents`, user/pass `devops/devops`). Use `pixi run compose-up` from the repo root to start infrastructure.

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

## Configuration

| Env var                      | Default                                      | Description       |
| ---------------------------- | -------------------------------------------- | ----------------- |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5432/incidents` | Postgres JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `devops`                                     | DB username       |
| `SPRING_DATASOURCE_PASSWORD` | `devops`                                     | DB password       |
| `NATS_URL`                   | `nats://localhost:4222`                      | NATS server URL   |
