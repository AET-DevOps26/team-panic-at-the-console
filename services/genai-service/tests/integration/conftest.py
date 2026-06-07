"""Fixtures for tests that hit a real LLM provider.

Two providers, each opt-in via env so the suite stays skipped by default:

- Ollama: set `OLLAMA_INTEGRATION_URL`. Defaults to `qwen2.5:0.5b` (small enough
  to pull and run on a CPU CI runner); override with `OLLAMA_INTEGRATION_MODEL`.
- Logos (TUM): set `LOGOS_INTEGRATION_KEY` (lg-...). Reachable only from the TUM
  network / eduVPN, so this runs locally, not on GitHub-hosted CI. Override the
  endpoint/model with `LOGOS_INTEGRATION_URL` / `LOGOS_INTEGRATION_MODEL`.

Provider-agnostic tests use the parametrized `llm_client` fixture and run once per
configured provider; provider-specific tests use `ollama_client` / `logos_client`.
"""

from __future__ import annotations

import os
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import httpx
import pytest
import pytest_asyncio

from genai_service.llm import LLMClient
from genai_service.logos_client import LogosClient
from genai_service.ollama_client import OllamaClient

LOGOS_DEFAULT_URL = "https://logos.aet.cit.tum.de:8080"
LOGOS_DEFAULT_MODEL = "openai/gpt-oss-120b"


def pytest_collection_modifyitems(
    config: pytest.Config, items: list[pytest.Item]
) -> None:
    if os.environ.get("OLLAMA_INTEGRATION_URL") or os.environ.get(
        "LOGOS_INTEGRATION_KEY"
    ):
        return
    skip = pytest.mark.skip(
        reason="no integration provider configured "
        "(set OLLAMA_INTEGRATION_URL or LOGOS_INTEGRATION_KEY)"
    )
    for item in items:
        if "integration" in item.keywords:
            item.add_marker(skip)


# ── Provider sessions ───────────────────────────────────────────────────────
# Async context managers (not fixtures) so both the provider-specific fixtures
# and the parametrized `llm_client` can reuse them without getfixturevalue,
# which cannot set up an async fixture from inside a running event loop.


@asynccontextmanager
async def _ollama_session() -> AsyncIterator[OllamaClient]:
    url = os.environ.get("OLLAMA_INTEGRATION_URL")
    if not url:
        pytest.skip("OLLAMA_INTEGRATION_URL not set")
    model = os.environ.get("OLLAMA_INTEGRATION_MODEL", "qwen2.5:0.5b")
    async with httpx.AsyncClient() as http:
        try:
            resp = await http.get(f"{url.rstrip('/')}/api/tags", timeout=5.0)
        except (httpx.HTTPError, OSError) as exc:
            pytest.skip(f"Ollama at {url} not reachable: {exc}")
        if resp.status_code != 200:
            pytest.skip(f"Ollama at {url} returned {resp.status_code} for /api/tags")
        available = {m.get("name", "") for m in resp.json().get("models", [])}
        if model not in available:
            pytest.skip(
                f"Model {model!r} not pulled on Ollama at {url}. "
                f"Available: {sorted(available)}. "
                f"Pull it (`ollama pull {model}`) or set OLLAMA_INTEGRATION_MODEL "
                f"to one of the available models."
            )
        # Loose timeout: small models on CPU still spend a few seconds per call.
        yield OllamaClient(http, url, model, generate_timeout_seconds=120.0)


@asynccontextmanager
async def _logos_session() -> AsyncIterator[LogosClient]:
    key = os.environ.get("LOGOS_INTEGRATION_KEY")
    if not key:
        pytest.skip("LOGOS_INTEGRATION_KEY not set")
    url = os.environ.get("LOGOS_INTEGRATION_URL", LOGOS_DEFAULT_URL)
    model = os.environ.get("LOGOS_INTEGRATION_MODEL", LOGOS_DEFAULT_MODEL)
    async with httpx.AsyncClient() as http:
        # Preflight only to skip gracefully when off-VPN or the key is bad. A 429 here
        # means the endpoint is up but rate-limited (60 RPM), so proceed: generate()
        # absorbs 429s with backoff. Skipping on 429 would make runs non-deterministic.
        try:
            resp = await http.get(
                f"{url.rstrip('/')}/v1/models",
                headers={"Authorization": f"Bearer {key}"},
                timeout=5.0,
            )
        except (httpx.HTTPError, OSError) as exc:
            pytest.skip(
                f"Logos at {url} not reachable "
                f"(requires the TUM network / eduVPN): {exc}"
            )
        if resp.status_code in (401, 403):
            pytest.skip(
                f"Logos rejected LOGOS_INTEGRATION_KEY (HTTP {resp.status_code})"
            )
        if resp.status_code not in (200, 429):
            pytest.skip(
                f"Logos preflight returned HTTP {resp.status_code}"
            )
        yield LogosClient(http, url, model, api_key=key)


_SESSIONS = {"ollama": _ollama_session, "logos": _logos_session}


# ── Fixtures ────────────────────────────────────────────────────────────────


@pytest_asyncio.fixture()
async def ollama_client() -> AsyncIterator[OllamaClient]:
    async with _ollama_session() as client:
        yield client


@pytest_asyncio.fixture()
async def logos_client() -> AsyncIterator[LogosClient]:
    async with _logos_session() as client:
        yield client


@pytest_asyncio.fixture(params=["ollama", "logos"])
async def llm_client(request: pytest.FixtureRequest) -> AsyncIterator[LLMClient]:
    """Runs once per configured provider; unconfigured ones skip their parametrization."""
    async with _SESSIONS[request.param]() as client:
        yield client
