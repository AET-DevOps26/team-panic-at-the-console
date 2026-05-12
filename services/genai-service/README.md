# genai-service

FastAPI scaffold:

- **`GET /health`**: process is up; **no Ollama call** (use this for Docker / Kubernetes probes when Ollama is optional).
- **`GET /api/v1/genai/ollama/health`**: checks a local [Ollama](https://ollama.com) instance (`GENAI_OLLAMA_URL`, default `http://localhost:11434`); returns **503** if Ollama is unreachable (for monitoring, not for liveness).

## Local dev

From the service directory:

```bash
cd services/genai-service
pixi install
pixi run test
pixi run start
```

URLs (default port **8087**):

- **http://localhost:8087/health** (probes, load balancers)
- **http://localhost:8087/api/v1/genai/ollama/health** (Ollama status)

Reload during development (requires the `dev` Pixi environment):

```bash
pixi run -e dev start
```

From the repository root (uses the workspace Pixi manifest):

```bash
pixi run test-genai
```

## Logging

**Uvicorn** uses Python’s standard **`logging`** module and prints lines like `INFO: Started server process ...`.

**This service** uses **structlog**. By default it uses a **console** renderer (key=value style). Set **`GENAI_LOG_JSON=true`** for one JSON line per event (typical in production log collectors).
