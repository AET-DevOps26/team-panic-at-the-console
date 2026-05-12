# genai-service

FastAPI scaffold: exposes **`GET /api/v1/genai/health`**, which checks reachability of a local [Ollama](https://ollama.com) instance (`GENAI_OLLAMA_URL`, default `http://localhost:11434`) and reports the configured model name.

## Local dev

From the service directory:

```bash
cd services/genai-service
pixi install
pixi run test
pixi run start
```

Health URL with default port: **http://localhost:8087/api/v1/genai/health** (`/genai/health` alone returns 404).

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
