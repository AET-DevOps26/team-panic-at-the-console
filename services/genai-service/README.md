# genai-service

FastAPI scaffold:

- **`GET /health`**: process is up; **no Ollama call** (use this for Docker / Kubernetes probes when Ollama is optional).
- **`GET /api/v1/genai/ollama/health`**: checks a local [Ollama](https://ollama.com) instance (`OLLAMA_URL`, default `http://localhost:11434`); JSON **`status`: `"ok"`** when Ollama answers, **`"degraded"`** with **503** when it does not (for monitoring, not for liveness).
- **`POST /api/v1/genai/_debug/generate`** _(opt-in)_: manual smoke test that calls Ollama with a free-form prompt. Mounted only when `DEBUG_ENDPOINTS=true`. Never enable in production — the service's real surface is NATS (see [ADR-0002](../../docs/adr/0002-genai-service-stateless.md)).

## NATS consumer

On startup the service connects to NATS (`NATS_URL`) and subscribes — in the `genai-service` queue group — to:

| Subject                    | Action                                                               |
| -------------------------- | -------------------------------------------------------------------- |
| `incident.created`         | Generate Summary, Severity suggestion, and Solutions; PATCH back     |
| `incident.resolved`        | Generate Postmortem; PATCH back to incident-service                  |
| `incident.regen.requested` | Re-run one task (`task` in payload: SUMMARY, SEVERITY_SUGGESTION, …) |

For each message the service reads `incidentId` from the payload, fetches the incident and its event log from `incident-service` (`INCIDENT_SERVICE_URL`), builds a prompt via `PromptBuilder`, generates a structured response with `OllamaClient.generate(... response_model=...)`, and PATCHes the result back. Handler errors are logged and swallowed so a single bad message does not stop the consumer.

Set `NATS_ENABLED=false` to skip the subscription on startup (useful when running the service without infrastructure).

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

## Manual end-to-end smoke

`scripts/demo_generate.py` runs `PromptBuilder → OllamaClient` against a live Ollama for every `PromptTask` and prints the JSON response. Useful to eyeball whether the model produces reasonable output before relying on the NATS flow (which depends on `incident-service` implementing the read + write-back endpoints).

```bash
# With the compose stack up (ollama on host:11434, model pulled)
pixi run --manifest-path services/genai-service/pixi.toml demo
```

Override with `OLLAMA_URL` / `OLLAMA_MODEL` to point at a different Ollama.

### Input

The script hard-codes a single fixture: one open incident plus a resolved variant (used only for the postmortem run), and a four-entry event log shared by all tasks.

**Open incident** (`summary`, `severity_suggestion`, `solution_suggestions`):

| Field       | Value                                             |
| ----------- | ------------------------------------------------- |
| id          | `018e2c5f-1234-7abc-8def-000000000001`            |
| title       | `Checkout service 5xx spike`                      |
| description | `Errors on /checkout up sharply since 09:00 UTC.` |
| status      | `open`                                            |
| severity    | `SEV2`                                            |
| createdAt   | `2026-05-20T09:00:00+00:00`                       |

**Resolved incident** (`postmortem`): same as above with `id` ending `…002`, `status=resolved`, and `resolvedAt=2026-05-20T10:30:00+00:00`.

**Event log** (chronological, shared by all tasks):

| Timestamp (UTC)       | Type             | Description                                                      |
| --------------------- | ---------------- | ---------------------------------------------------------------- |
| `2026-05-20T09:02:00` | `status_changed` | `status: open -> investigating`                                  |
| `2026-05-20T09:05:00` | `comment_added`  | `Recent deploy to checkout-service at 08:55 UTC may be related.` |
| `2026-05-20T09:20:00` | `comment_added`  | `Rolled back checkout-service; error rate dropping.`             |
| `2026-05-20T10:30:00` | `status_changed` | `status: investigating -> resolved`                              |

### Expected output

One JSON block per `PromptTask` (`summary`, `severity_suggestion`, `solution_suggestions`, `postmortem`), each validated against the corresponding response model. Exact text varies by model and run; shape is fixed. Example (`qwen2.5:3b`, trimmed):

```text
## summary
{ "summary": "The checkout service is experiencing a spike in 5xx errors..." }

## severity_suggestion
{ "severity": "SEV3", "reason": "Significant but not catastrophic spike..." }

## solution_suggestions
{ "solutions": ["Verify the rollback...", "Check application logs...", ...] }

## postmortem
{
  "root_cause": "A recent deployment introduced an error in checkout-service...",
  "timeline": ["09:02 UTC status changed to investigating", ...],
  "action_items": ["Tighten pre-deploy validation", ...]
}
```

If a generation fails Pydantic validation (small models occasionally produce malformed JSON), the script raises `OllamaError`; rerun. The integration tests below cover the same path with retries.

## Integration tests against a real LLM

`tests/integration/` exercises `.generate` and the full `PromptBuilder → LLM` round-trip against a live provider. The provider-agnostic tests run once per configured provider via the parametrized `llm_client` fixture; each provider is opt-in via env, so the suite stays skipped by default.

### Ollama

Skipped unless `OLLAMA_INTEGRATION_URL` is set. Run locally against the compose stack:

```bash
pixi run compose-up   # in the repo root
OLLAMA_INTEGRATION_URL=http://localhost:11434 \
OLLAMA_INTEGRATION_MODEL=qwen2.5:3b \
  pixi run test-integration
```

In CI they run nightly (and on demand) via `.github/workflows/ollama-integration.yml`.

### Logos (TUM)

Skipped unless `LOGOS_INTEGRATION_KEY` is set. Logos is reachable only from the TUM network / eduVPN, so these run **locally, not on GitHub-hosted CI**. Defaults to `https://logos.aet.cit.tum.de:8080` and `openai/gpt-oss-120b` (override with `LOGOS_INTEGRATION_URL` / `LOGOS_INTEGRATION_MODEL`):

```bash
# On eduVPN. Pull the key from SOPS rather than pasting it:
LOGOS_INTEGRATION_KEY="$(pixi run -e deploy sops --decrypt \
  --extract '["secrets"]["logosApiKey"]' infra/helm/secrets/values.prod.enc.yaml)" \
  pixi run --manifest-path services/genai-service/pixi.toml test-integration
```

The preflight proceeds on a `429` (endpoint up but rate-limited at 60 RPM); `generate()` absorbs 429s with backoff.

Regular PR CI skips all integration tests — they're slow (~10–30 s per call) and smaller models occasionally produce JSON that fails strict validation. Each integration test carries `@pytest.mark.flaky(reruns=3)` (via `pytest-rerunfailures`) to absorb that baseline flakiness; unit `test` stays retry-free.

## Logging

**Uvicorn** uses Python’s standard **`logging`** module and prints lines like `INFO: Started server process ...`.

**This service** uses **structlog**. By default it uses a **console** renderer (key=value style). Set **`LOG_JSON=true`** for one JSON line per event (typical in production log collectors).
