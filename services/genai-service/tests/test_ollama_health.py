from unittest.mock import AsyncMock, patch

OLLAMA_HEALTH_PATH = "/api/v1/genai/ollama/health"


def test_ollama_health_when_reachable(client):
    with patch.object(
        client.app.state.ollama_client, "reachable", AsyncMock(return_value=True)
    ):
        resp = client.get(OLLAMA_HEALTH_PATH)

    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["ollamaReachable"] is True
    assert body["model"] == "qwen2.5:3b"


def test_ollama_health_when_unreachable(client):
    with patch.object(
        client.app.state.ollama_client, "reachable", AsyncMock(return_value=False)
    ):
        resp = client.get(OLLAMA_HEALTH_PATH)

    assert resp.status_code == 503
    body = resp.json()
    assert body["status"] == "degraded"
    assert body["ollamaReachable"] is False
