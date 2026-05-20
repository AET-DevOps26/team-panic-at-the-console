"""End-to-end integration tests: PromptBuilder -> real Ollama -> typed response.

One test per PromptTask. Each test sends a realistic incident through
PromptBuilder.build(...) and verifies the LLM produces a response that
validates against the task's response model. Output content is unchecked;
this only proves the schema + wiring round-trips.
"""

from __future__ import annotations

from datetime import UTC, datetime

import pytest

from genai_service.ollama_client import OllamaClient
from genai_service.prompts import (
    Event,
    Incident,
    PostmortemResponse,
    PromptBuilder,
    PromptTask,
    SeverityResponse,
    SolutionsResponse,
    SummaryResponse,
)

pytestmark = pytest.mark.integration


def _open_incident() -> Incident:
    return Incident(
        id="018e2c5f-1234-7abc-8def-000000000001",
        title="Checkout service 5xx spike",
        description="Errors on /checkout up sharply since 09:00 UTC.",
        status="open",
        severity="SEV2",
        created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
    )


def _resolved_incident() -> Incident:
    return Incident(
        id="018e2c5f-1234-7abc-8def-000000000002",
        title="Checkout service 5xx spike",
        description="Errors on /checkout up sharply since 09:00 UTC.",
        status="resolved",
        severity="SEV2",
        created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
        resolved_at=datetime(2026, 5, 20, 10, 30, tzinfo=UTC),
    )


def _events() -> list[Event]:
    return [
        Event(
            timestamp=datetime(2026, 5, 20, 9, 2, tzinfo=UTC),
            type="status_changed",
            description="status: open -> investigating",
        ),
        Event(
            timestamp=datetime(2026, 5, 20, 9, 5, tzinfo=UTC),
            type="comment_added",
            description="Recent deploy to checkout-service at 08:55 UTC may be related.",
        ),
        Event(
            timestamp=datetime(2026, 5, 20, 9, 20, tzinfo=UTC),
            type="comment_added",
            description="Rolled back checkout-service; error rate dropping.",
        ),
        Event(
            timestamp=datetime(2026, 5, 20, 10, 30, tzinfo=UTC),
            type="status_changed",
            description="status: investigating -> resolved",
        ),
    ]


async def _run(ollama_client: OllamaClient, incident: Incident, task: PromptTask):
    prompt = PromptBuilder().build(incident, _events(), task)
    return await ollama_client.generate(
        prompt.user, system=prompt.system, response_model=prompt.response_model
    )


@pytest.mark.flaky(reruns=3)
async def test_summary_end_to_end(ollama_client: OllamaClient):
    result = await _run(ollama_client, _open_incident(), PromptTask.SUMMARY)
    assert isinstance(result, SummaryResponse)
    assert result.summary.strip() != ""


@pytest.mark.flaky(reruns=3)
async def test_severity_suggestion_end_to_end(ollama_client: OllamaClient):
    result = await _run(ollama_client, _open_incident(), PromptTask.SEVERITY_SUGGESTION)
    assert isinstance(result, SeverityResponse)
    # severity is constrained to the SEV1-SEV4 Literal via response_model schema.
    assert result.reason.strip() != ""


@pytest.mark.flaky(reruns=3)
async def test_solution_suggestions_end_to_end(ollama_client: OllamaClient):
    result = await _run(
        ollama_client, _open_incident(), PromptTask.SOLUTION_SUGGESTIONS
    )
    assert isinstance(result, SolutionsResponse)
    assert len(result.solutions) >= 1
    assert all(s.strip() != "" for s in result.solutions)


@pytest.mark.flaky(reruns=3)
async def test_postmortem_end_to_end(ollama_client: OllamaClient):
    result = await _run(ollama_client, _resolved_incident(), PromptTask.POSTMORTEM)
    assert isinstance(result, PostmortemResponse)
    assert result.root_cause.strip() != ""
    assert len(result.timeline) >= 1
    assert len(result.action_items) >= 1
