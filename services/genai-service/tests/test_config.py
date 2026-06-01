import importlib


def test_settings_reads_ollama_env(monkeypatch):
    monkeypatch.setenv("OLLAMA_URL", "http://ollama:11434")
    monkeypatch.setenv("OLLAMA_MODEL", "custom-model")

    import genai_service.config as config_mod

    importlib.reload(config_mod)

    assert config_mod.settings.ollama_url == "http://ollama:11434"
    assert config_mod.settings.ollama_model == "custom-model"

    importlib.reload(config_mod)
