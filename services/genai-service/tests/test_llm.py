from unittest.mock import AsyncMock

import pytest
from pydantic import BaseModel

from genai_service.llm import FallbackLLMClient


class Summary(BaseModel):
    title: str


def _stub(model: str) -> AsyncMock:
    stub = AsyncMock()
    stub.model = model
    return stub


async def test_uses_primary_when_it_succeeds():
    primary = _stub("logos")
    backup = _stub("ollama")
    primary.generate.return_value = "from-primary"

    out = await FallbackLLMClient(primary, backup).generate("hi", system="s")

    assert out == "from-primary"
    primary.generate.assert_awaited_once_with(
        "hi", system="s", response_model=None, timeout=None
    )
    backup.generate.assert_not_awaited()


async def test_falls_back_to_backup_when_primary_raises():
    primary = _stub("logos")
    backup = _stub("ollama")
    primary.generate.side_effect = RuntimeError("logos down")
    backup.generate.return_value = Summary(title="ok")

    out = await FallbackLLMClient(primary, backup).generate(
        "hi", response_model=Summary
    )

    assert out == Summary(title="ok")
    backup.generate.assert_awaited_once_with(
        "hi", system=None, response_model=Summary, timeout=None
    )


async def test_propagates_when_backup_also_fails():
    primary = _stub("logos")
    backup = _stub("ollama")
    primary.generate.side_effect = RuntimeError("logos down")
    backup.generate.side_effect = RuntimeError("ollama down")

    with pytest.raises(RuntimeError, match="ollama down"):
        await FallbackLLMClient(primary, backup).generate("hi")


async def test_model_reports_primary():
    assert FallbackLLMClient(_stub("logos"), _stub("ollama")).model == "logos"


async def test_reachable_true_when_either_reachable():
    primary = _stub("logos")
    backup = _stub("ollama")
    primary.reachable.return_value = False
    backup.reachable.return_value = True

    assert await FallbackLLMClient(primary, backup).reachable() is True
