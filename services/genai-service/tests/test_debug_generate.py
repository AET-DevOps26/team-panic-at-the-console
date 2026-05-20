from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

from genai_service.ollama_client import OllamaError

DEBUG_PATH = "/api/v1/genai/_debug/generate"


@pytest.fixture()
def debug_client(monkeypatch):
    """Rebuilds the app with DEBUG_ENDPOINTS=true so the route is mounted."""
    monkeypatch.setenv("DEBUG_ENDPOINTS", "true")

    import importlib

    import genai_service.config as config_mod
    import genai_service.main as main_mod

    importlib.reload(config_mod)
    importlib.reload(main_mod)

    with TestClient(main_mod.app) as c:
        yield c

    # Restore defaults for other tests.
    monkeypatch.delenv("DEBUG_ENDPOINTS", raising=False)
    importlib.reload(config_mod)
    importlib.reload(main_mod)


def test_debug_generate_disabled_by_default(client):
    resp = client.post(DEBUG_PATH, json={"prompt": "hi"})
    assert resp.status_code == 404


def test_debug_generate_returns_model_output(debug_client):
    with patch.object(
        debug_client.app.state.ollama_client,
        "generate",
        AsyncMock(return_value="hello back"),
    ):
        resp = debug_client.post(DEBUG_PATH, json={"prompt": "hi"})

    assert resp.status_code == 200
    body = resp.json()
    assert body == {"model": "qwen2.5:3b", "response": "hello back"}


def test_debug_generate_propagates_ollama_error_as_502(debug_client):
    with patch.object(
        debug_client.app.state.ollama_client,
        "generate",
        AsyncMock(side_effect=OllamaError("ollama down")),
    ):
        resp = debug_client.post(DEBUG_PATH, json={"prompt": "hi"})

    assert resp.status_code == 502
    assert "ollama down" in resp.json()["detail"]


def test_debug_generate_rejects_empty_prompt(debug_client):
    resp = debug_client.post(DEBUG_PATH, json={"prompt": ""})
    assert resp.status_code == 422
