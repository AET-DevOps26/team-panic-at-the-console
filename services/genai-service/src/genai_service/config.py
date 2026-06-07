from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Env vars are unprefixed (OLLAMA_URL, NATS_URL, ...) to match compose/Helm and global.env inheritance.
    model_config = SettingsConfigDict(extra="ignore")

    # Active provider for incident generation. "ollama" keeps off-VPN local dev working;
    # the cluster sets "logos" (Logos is only reachable from the TUM network / eduVPN).
    llm_provider: str = "ollama"
    # When true, fall back to Ollama if the primary (Logos) call fails or is rate-limited.
    llm_fallback_enabled: bool = False

    ollama_url: str = "http://localhost:11434"
    ollama_model: str = "qwen2.5:3b"
    # Ollama inference on CPU can take 10-30s per call (ADR-0003); leave generous headroom.
    ollama_generate_timeout_seconds: float = 120.0

    # TUM Logos: OpenAI-compatible API. Key is a secret (injected via env / k8s secret).
    logos_url: str = "https://logos.aet.cit.tum.de:8080"
    logos_model: str = "openai/gpt-oss-120b"
    logos_api_key: str = ""
    logos_generate_timeout_seconds: float = 120.0

    nats_url: str = "nats://localhost:4222"
    # Lets unit tests (and dev runs without infra) skip the NATS subscription on startup.
    nats_enabled: bool = True
    nats_queue_group: str = "genai-service"
    nats_connect_timeout_seconds: float = 5.0

    incident_service_url: str = "http://localhost:8081"
    incident_service_timeout_seconds: float = 10.0

    # Exposes POST /api/v1/genai/_debug/generate for manual smoke testing. Never enable in prod.
    debug_endpoints: bool = False
    # True: one JSON line per event (good for log aggregation). False: human-readable in the terminal.
    log_json: bool = False


settings = Settings()
