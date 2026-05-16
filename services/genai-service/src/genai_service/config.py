from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="GENAI_", extra="ignore")

    ollama_url: str = "http://localhost:11434"
    ollama_model: str = "qwen2.5:3b"
    # True: one JSON line per event (good for log aggregation). False: human-readable in the terminal.
    log_json: bool = False


settings = Settings()
