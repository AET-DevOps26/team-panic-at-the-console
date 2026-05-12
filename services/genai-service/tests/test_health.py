from unittest.mock import AsyncMock, patch


def test_health_ollama_reachable(client):
    with patch.object(
        client.app.state.ollama_client, "reachable", AsyncMock(return_value=True)
    ):
        resp = client.get("/api/v1/genai/health")

    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["ollamaReachable"] is True
    assert body["model"] == "qwen2.5:3b"


def test_health_ollama_unreachable(client):
    with patch.object(
        client.app.state.ollama_client, "reachable", AsyncMock(return_value=False)
    ):
        resp = client.get("/api/v1/genai/health")

    assert resp.status_code == 503
    assert resp.json()["ollamaReachable"] is False
