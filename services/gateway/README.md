# gateway

API entry point for the incident platform. Exposes routes from `api/openapi.yaml` under `/api/v1` and proxies to cluster-internal services.

OpenAPI exploration uses the standalone **swagger-ui** service (`api/openapi.yaml`), not springdoc on this service.

## Implemented routes

| Route                                 | Action                                      |
| ------------------------------------- | ------------------------------------------- |
| `GET /api/v1/health`                  | Gateway liveness (local)                    |
| `POST /api/v1/incidents/{id}/genai/*` | Proxy to `incident-service` regen endpoints |
| `GET/POST/PATCH /api/v1/incidents*`   | Proxy to `incident-service` incident REST API |

GenAI compute runs via NATS (`genai-service`); Ollama reachability is checked on that service (`/api/v1/genai/ollama/health`), not exposed on the gateway.

Ingress (and local compose `edge` on port 8080) sends traffic with prefix `/api`; clients should use `/api/v1/...`. Swagger UI is at `/swagger/` on the same host.

## Configuration

Downstream base URLs are required (`@NotBlank` on `GatewayProperties`). Local defaults live in `application.properties`; compose and Helm set `GATEWAY_*` env vars for deployed environments.

| Property / env                                                  | Local default (`application.properties`) |
| --------------------------------------------------------------- | ---------------------------------------- |
| `gateway.incident-service-url` / `GATEWAY_INCIDENT_SERVICE_URL` | `http://localhost:8081`                  |

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
- Routes for user-service and event-service
- Genai write-back `PATCH .../genai/*/result` (cluster-internal; gateway returns 403)
