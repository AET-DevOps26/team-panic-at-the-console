import importlib

import pytest

import genai_service.config as config_module


@pytest.fixture()
def reload_settings():
    """Reload the config module so each test gets a fresh Settings() from current env."""
    yield lambda: importlib.reload(config_module).settings
    importlib.reload(config_module)


def test_defaults_match_module_constants(reload_settings, monkeypatch):
    for var in (
        "LLM_PROVIDER",
        "LLM_FALLBACK_ENABLED",
        "OLLAMA_URL",
        "OLLAMA_MODEL",
        "NATS_URL",
        "NATS_ENABLED",
        "INCIDENT_SERVICE_URL",
        "DEBUG_ENDPOINTS",
        "LOG_JSON",
    ):
        monkeypatch.delenv(var, raising=False)

    s = reload_settings()

    assert s.llm_provider == "ollama"
    assert s.llm_fallback_enabled is False
    assert s.ollama_url == "http://localhost:11434"
    assert s.ollama_model == "qwen2.5:3b"
    assert s.nats_url == "nats://localhost:4222"
    assert s.nats_enabled is True
    assert s.incident_service_url == "http://localhost:8081"
    assert s.debug_endpoints is False
    assert s.log_json is False


def test_unprefixed_env_vars_override_defaults(reload_settings, monkeypatch):
    """Settings must read unprefixed env names so the repo-wide compose/helm convention works."""
    monkeypatch.setenv("OLLAMA_URL", "http://ollama-host:11434")
    monkeypatch.setenv("OLLAMA_MODEL", "qwen2.5:7b")
    monkeypatch.setenv("NATS_URL", "nats://broker:4222")
    monkeypatch.setenv("INCIDENT_SERVICE_URL", "http://incidents:8081")
    monkeypatch.setenv("NATS_ENABLED", "false")
    monkeypatch.setenv("DEBUG_ENDPOINTS", "true")
    monkeypatch.setenv("LOG_JSON", "true")
    monkeypatch.setenv("LLM_PROVIDER", "logos")

    s = reload_settings()

    assert s.ollama_url == "http://ollama-host:11434"
    assert s.ollama_model == "qwen2.5:7b"
    assert s.nats_url == "nats://broker:4222"
    assert s.incident_service_url == "http://incidents:8081"
    assert s.nats_enabled is False
    assert s.debug_endpoints is True
    assert s.log_json is True
    assert s.llm_provider == "logos"


def test_genai_prefix_is_ignored(reload_settings, monkeypatch):
    """Regression guard: re-introducing env_prefix='GENAI_' would silently break compose."""
    monkeypatch.delenv("NATS_URL", raising=False)
    monkeypatch.setenv("GENAI_NATS_URL", "nats://prefixed:4222")

    s = reload_settings()

    assert s.nats_url == "nats://localhost:4222"
