# gateway

API entry point for the incident platform. Exposes routes from `api/openapi.yaml` under `/api/v1` and proxies to cluster-internal services.

OpenAPI exploration uses the standalone **swagger-ui** service (`api/openapi.yaml`), not springdoc on this service.

## Implemented routes

| Route                                 | Action                                                 |
| ------------------------------------- | ------------------------------------------------------ |
| `GET /api/v1/health`                  | Gateway liveness (local)                               |
| `GET /api/v1/genai/health`            | Proxy to `genai-service` `/api/v1/genai/ollama/health` |
| `POST /api/v1/incidents/{id}/genai/*` | Proxy to `incident-service` regen endpoints            |

Ingress (and local compose `edge` on port 8080) sends traffic with prefix `/api`; clients should use `/api/v1/...`. Swagger UI is at `/swagger/` on the same host.

## Configuration

| Property / env                                                  | Default                 |
| --------------------------------------------------------------- | ----------------------- |
| `gateway.incident-service-url` / `GATEWAY_INCIDENT_SERVICE_URL` | `http://localhost:8081` |
| `gateway.genai-service-url` / `GATEWAY_GENAI_SERVICE_URL`       | `http://localhost:8087` |

## Local development

```bash
pixi run --manifest-path services/gateway/pixi.toml test
pixi run --manifest-path services/gateway/pixi.toml start
```

With compose (`pixi run compose-up`), use the `edge` proxy on port **8080** (`/api/v1/...` for API, `/swagger/` for docs). Downstream URLs are set via environment variables.

`pixi run start` on this module binds **8080** directly (API only, no `/swagger/`).

## Not yet implemented

- JWT cookie validation and `X-User-Id` / `X-User-Role` injection
- NATS → SSE fan-out to browsers
- Routes for user-service, event-service, and remaining incident CRUD APIs
