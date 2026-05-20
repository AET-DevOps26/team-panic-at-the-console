# genai-service

FastAPI scaffold:

- **`GET /health`**: process is up; **no Ollama call** (use this for Docker / Kubernetes probes when Ollama is optional).
- **`GET /api/v1/genai/ollama/health`**: checks a local [Ollama](https://ollama.com) instance (`GENAI_OLLAMA_URL`, default `http://localhost:11434`); JSON **`status`: `"ok"`** when Ollama answers, **`"degraded"`** with **503** when it does not (for monitoring, not for liveness).
- **`POST /api/v1/genai/_debug/generate`** _(opt-in)_: manual smoke test that calls Ollama with a free-form prompt. Mounted only when `GENAI_DEBUG_ENDPOINTS=true`. Never enable in production — the service's real surface is NATS (see [ADR-0002](../../docs/adr/0002-genai-service-stateless.md)).

## Ollama client

`OllamaClient.generate(prompt, *, system=None, response_model=None, timeout=None)` calls `POST /api/generate` with `stream=false`. Pass a Pydantic model as `response_model` to constrain the LLM to that model's JSON schema and get back a parsed instance:

```python
from pydantic import BaseModel

class Summary(BaseModel):
    title: str
    severity: str

result = await ollama.generate("summarize incident #42", response_model=Summary)
# result is a Summary instance, validated.
```

Non-2xx responses, transport errors, and schema-validation failures all raise `OllamaError`.

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
