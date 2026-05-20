"""Fixtures for tests that hit a real Ollama.

Skipped unless `OLLAMA_INTEGRATION_URL` is set. Defaults to `qwen2.5:0.5b` because
that's small enough to download and run on a CPU CI runner; override with
`OLLAMA_INTEGRATION_MODEL` to test against the production model (`qwen2.5:3b`).
"""

from __future__ import annotations

import os
from collections.abc import AsyncIterator

import httpx
import pytest
import pytest_asyncio

from genai_service.ollama_client import OllamaClient


def pytest_collection_modifyitems(
    config: pytest.Config, items: list[pytest.Item]
) -> None:
    url = os.environ.get("OLLAMA_INTEGRATION_URL")
    if url:
        return
    skip = pytest.mark.skip(reason="OLLAMA_INTEGRATION_URL not set")
    for item in items:
        if "integration" in item.keywords:
            item.add_marker(skip)


@pytest.fixture(scope="session")
def ollama_url() -> str:
    return os.environ["OLLAMA_INTEGRATION_URL"]


@pytest.fixture(scope="session")
def ollama_model() -> str:
    return os.environ.get("OLLAMA_INTEGRATION_MODEL", "qwen2.5:0.5b")


@pytest_asyncio.fixture()
async def _ollama_preflight(ollama_url: str, ollama_model: str) -> None:
    """Verify Ollama is reachable AND the model is pulled; skip all integration tests otherwise."""
    async with httpx.AsyncClient() as http:
        try:
            resp = await http.get(f"{ollama_url.rstrip('/')}/api/tags", timeout=5.0)
        except (httpx.HTTPError, OSError) as exc:
            pytest.skip(f"Ollama at {ollama_url} not reachable: {exc}")

        if resp.status_code != 200:
            pytest.skip(
                f"Ollama at {ollama_url} returned {resp.status_code} for /api/tags"
            )

        available = {m.get("name", "") for m in resp.json().get("models", [])}
        if ollama_model not in available:
            pytest.skip(
                f"Model {ollama_model!r} not pulled on Ollama at {ollama_url}. "
                f"Available: {sorted(available)}. "
                f"Pull it (`ollama pull {ollama_model}`) or set OLLAMA_INTEGRATION_MODEL "
                f"to one of the available models."
            )


@pytest_asyncio.fixture()
async def ollama_client(
    ollama_url: str,
    ollama_model: str,
    _ollama_preflight: None,
) -> AsyncIterator[OllamaClient]:
    # Loose timeout: small models on CPU still spend a few seconds per call.
    async with httpx.AsyncClient() as http:
        yield OllamaClient(
            http,
            ollama_url,
            ollama_model,
            generate_timeout_seconds=120.0,
        )
