# Ollama runs in the Kubernetes cluster (no cloud LLM)

`genai-service` calls a local Ollama instance (`qwen2.5:3b`) deployed as a pod in the same Kubernetes cluster, in both development (docker-compose) and production. There is no dependency on a cloud LLM provider (OpenAI, Anthropic, etc.).

The team has no LLM API credits for the course. Cloud providers were ruled out on cost grounds. Ollama with `qwen2.5:3b` (~2GB, strong structured JSON output for its size) runs on CPU within the cluster's resource budget. The trade-off is slow inference (10-30s per generation on CPU) and no fallback if the Ollama pod is unavailable. This is acceptable for a course demo where throughput and availability SLAs are not requirements.

Ollama's API is OpenAI-compatible, so switching to a cloud provider later is a config change (`LLM_BASE_URL` + `LLM_API_KEY`) with no code changes.
