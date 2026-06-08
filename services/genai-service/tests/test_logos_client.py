import json

import httpx
import pytest
from pydantic import BaseModel

from genai_service.logos_client import LogosClient, LogosError

FAKE_API_KEY = "lg-test-key"


class Summary(BaseModel):
    title: str
    severity: str


def _client_with(transport: httpx.MockTransport, **kwargs) -> LogosClient:
    http = httpx.AsyncClient(transport=transport)
    return LogosClient(
        http,
        "https://logos.aet.cit.tum.de:8080",
        "openai/gpt-oss-120b",
        api_key=FAKE_API_KEY,
        retry_backoff_seconds=0.0,
        **kwargs,
    )


async def test_generate_returns_message_content():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["auth"] = request.headers.get("authorization")
        captured["body"] = json.loads(request.content)
        return httpx.Response(
            200, json={"choices": [{"message": {"content": "hello world"}}]}
        )

    logos = _client_with(httpx.MockTransport(handler))
    out = await logos.generate("say hi")

    assert out == "hello world"
    assert captured["url"].endswith("/v1/chat/completions")
    assert captured["auth"] == f"Bearer {FAKE_API_KEY}"
    assert captured["body"]["model"] == "openai/gpt-oss-120b"
    assert captured["body"]["messages"] == [{"role": "user", "content": "say hi"}]
    assert "response_format" not in captured["body"]


async def test_generate_with_system_prepends_system_message():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = json.loads(request.content)
        return httpx.Response(200, json={"choices": [{"message": {"content": "ok"}}]})

    logos = _client_with(httpx.MockTransport(handler))
    await logos.generate("hi", system="you are concise")

    assert captured["body"]["messages"] == [
        {"role": "system", "content": "you are concise"},
        {"role": "user", "content": "hi"},
    ]


async def test_generate_with_pydantic_model_uses_json_schema_and_parses():
    def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content)
        rf = body["response_format"]
        assert rf["type"] == "json_schema"
        assert "title" in rf["json_schema"]["schema"]["properties"]
        return httpx.Response(
            200,
            json={
                "choices": [
                    {"message": {"content": '{"title": "DB down", "severity": "SEV1"}'}}
                ]
            },
        )

    logos = _client_with(httpx.MockTransport(handler))
    out = await logos.generate("summarize", response_model=Summary)

    assert isinstance(out, Summary)
    assert out.title == "DB down"
    assert out.severity == "SEV1"


async def test_generate_retries_on_429_then_succeeds():
    calls = {"n": 0}

    def handler(request: httpx.Request) -> httpx.Response:
        calls["n"] += 1
        if calls["n"] == 1:
            return httpx.Response(429, text="rate limited")
        return httpx.Response(200, json={"choices": [{"message": {"content": "ok"}}]})

    logos = _client_with(httpx.MockTransport(handler))
    out = await logos.generate("hi")

    assert out == "ok"
    assert calls["n"] == 2


async def test_generate_raises_after_429_retries_exhausted():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(429, text="rate limited")

    logos = _client_with(httpx.MockTransport(handler), max_retries=2)
    with pytest.raises(LogosError, match="429"):
        await logos.generate("hi")


async def test_generate_raises_on_non_200():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, text="boom")

    logos = _client_with(httpx.MockTransport(handler))
    with pytest.raises(LogosError, match="status 500"):
        await logos.generate("hi")


async def test_generate_raises_on_transport_error():
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused")

    logos = _client_with(httpx.MockTransport(handler))
    with pytest.raises(LogosError, match="request failed"):
        await logos.generate("hi")


async def test_generate_raises_when_response_invalid_for_model():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200, json={"choices": [{"message": {"content": '{"title": "only"}'}}]}
        )

    logos = _client_with(httpx.MockTransport(handler))
    with pytest.raises(LogosError, match="did not match Summary"):
        await logos.generate("hi", response_model=Summary)


async def test_generate_raises_when_choices_missing():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"unexpected": "shape"})

    logos = _client_with(httpx.MockTransport(handler))
    with pytest.raises(LogosError, match="missing"):
        await logos.generate("hi")


async def test_reachable_true_on_200_models():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["auth"] = request.headers.get("authorization")
        return httpx.Response(200, json={"data": []})

    logos = _client_with(httpx.MockTransport(handler))
    assert await logos.reachable() is True
    assert captured["url"].endswith("/v1/models")
    assert captured["auth"] == f"Bearer {FAKE_API_KEY}"


async def test_reachable_false_on_error():
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused")

    logos = _client_with(httpx.MockTransport(handler))
    assert await logos.reachable() is False
