import httpx
import pytest
from pydantic import BaseModel

from genai_service.ollama_client import OllamaClient, OllamaError


class Summary(BaseModel):
    title: str
    severity: str


def _client_with(transport: httpx.MockTransport) -> OllamaClient:
    http = httpx.AsyncClient(transport=transport)
    return OllamaClient(http, "http://ollama:11434", "qwen2.5:3b")


async def test_generate_returns_raw_text():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["body"] = request.content
        return httpx.Response(200, json={"response": "hello world"})

    ollama = _client_with(httpx.MockTransport(handler))
    out = await ollama.generate("say hi")

    assert out == "hello world"
    assert captured["url"] == "http://ollama:11434/api/generate"
    assert b'"stream":false' in captured["body"]
    assert b'"format"' not in captured["body"]


async def test_generate_with_pydantic_model_parses_response():
    def handler(request: httpx.Request) -> httpx.Response:
        # Schema is forwarded as `format` so Ollama produces matching JSON.
        assert b'"format"' in request.content
        assert b'"title"' in request.content
        return httpx.Response(
            200, json={"response": '{"title": "DB down", "severity": "SEV1"}'}
        )

    ollama = _client_with(httpx.MockTransport(handler))
    out = await ollama.generate("summarize", response_model=Summary)

    assert isinstance(out, Summary)
    assert out.title == "DB down"
    assert out.severity == "SEV1"


async def test_generate_with_system_prompt_forwards_field():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = request.content
        return httpx.Response(200, json={"response": "ok"})

    ollama = _client_with(httpx.MockTransport(handler))
    await ollama.generate("hi", system="you are concise")

    assert b'"system":"you are concise"' in captured["body"]


async def test_generate_raises_on_non_200():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, text="boom")

    ollama = _client_with(httpx.MockTransport(handler))
    with pytest.raises(OllamaError, match="status 500"):
        await ollama.generate("hi")


async def test_generate_raises_on_transport_error():
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused")

    ollama = _client_with(httpx.MockTransport(handler))
    with pytest.raises(OllamaError, match="request failed"):
        await ollama.generate("hi")


async def test_generate_raises_when_response_invalid_for_model():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"response": '{"title": "only title"}'})

    ollama = _client_with(httpx.MockTransport(handler))
    with pytest.raises(OllamaError, match="did not match Summary"):
        await ollama.generate("hi", response_model=Summary)


async def test_generate_raises_when_response_field_missing():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"unexpected": "shape"})

    ollama = _client_with(httpx.MockTransport(handler))
    with pytest.raises(OllamaError, match="missing 'response'"):
        await ollama.generate("hi")
