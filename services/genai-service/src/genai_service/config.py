from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="GENAI_", extra="ignore")

    ollama_url: str = "http://localhost:11434"
    ollama_model: str = "qwen2.5:3b"
    # Ollama inference on CPU can take 10-30s per call (ADR-0003); leave generous headroom.
    ollama_generate_timeout_seconds: float = 120.0
    # Exposes POST /api/v1/genai/_debug/generate for manual smoke testing. Never enable in prod.
    debug_endpoints: bool = False
    # True: one JSON line per event (good for log aggregation). False: human-readable in the terminal.
    log_json: bool = False


settings = Settings()
