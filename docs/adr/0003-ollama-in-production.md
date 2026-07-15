# No paid LLM: Ollama for local dev, TUM Logos in the cluster

`genai-service` never calls a commercial LLM provider (OpenAI, Anthropic, etc.). The team has no LLM API credits for the course, so paid providers were ruled out on cost grounds. Two free options cover the two environments:

- **Local dev (docker-compose)**: Ollama running `qwen2.5:3b` (~2GB, strong structured JSON output for its size) as a container in the stack. This is the default (`LLM_PROVIDER=ollama`) because it needs no network access; Logos is only reachable from the TUM network (eduVPN).
- **Kubernetes**: TUM Logos (`logos.aet.cit.tum.de`, `openai/gpt-oss-120b`), free for TUM members and reachable because the cluster sits on the TUM network.

The chart still ships an Ollama Deployment, but it is disabled (`services.ollama.enabled: false`). The stud cluster's project ResourceQuota caps the namespace at 4 cpu / 6Gi of limits and the 3B model alone needs ~4Gi, which leaves no room for the rest of the stack. Where a namespace has the headroom, `enabled: true` restores in-cluster inference with no other change.

The cluster also sets `LLM_FALLBACK_ENABLED=false`, because with the Ollama pod disabled there is nothing to fall back to. Where both providers are available, the fallback wraps Logos so a failed or rate-limited call transparently retries against Ollama.

Trade-offs: the cluster now depends on a service the team does not operate, so a Logos outage means no AI generation until it recovers (incidents still work; they arrive without summaries). In exchange, cluster inference is much faster and stronger (120b, GPU-backed) than CPU Ollama at 10-30s per generation. Neither environment carries an availability SLA, which is acceptable for a course demo.

Both APIs are OpenAI-compatible, so switching provider is a config change (`LLM_PROVIDER` plus the matching URL and key), never a code change.
