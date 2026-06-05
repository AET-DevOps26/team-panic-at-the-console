"""End-to-end integration tests: PromptBuilder -> real LLM -> typed response.

One test per PromptTask, run against each configured provider (Ollama and/or
Logos) via the parametrized `llm_client` fixture. Each test sends a realistic
incident through PromptBuilder.build(...) and verifies the provider produces a
response that validates against the task's response model. Output content is
unchecked; this only proves the schema + wiring round-trips.
"""

from __future__ import annotations

from datetime import UTC, datetime
from uuid import UUID

import pytest

from client.models import Incident, IncidentEvent, IncidentStatus, Severity
from genai_service.llm import LLMClient
from genai_service.prompts import (
    PostmortemResponse,
    PromptBuilder,
    PromptTask,
    SeverityResponse,
    SolutionsResponse,
    SummaryResponse,
)


def _open_incident() -> Incident:
    return Incident(
        id=UUID("018e2c5f-1234-7abc-8def-000000000001"),
        title="Checkout service 5xx spike",
        description="Errors on /checkout up sharply since 09:00 UTC.",
        status=IncidentStatus.OPEN,
        severity=Severity.SEV2,
        created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
    )


def _resolved_incident() -> Incident:
    return Incident(
        id=UUID("018e2c5f-1234-7abc-8def-000000000002"),
        title="Checkout service 5xx spike",
        description="Errors on /checkout up sharply since 09:00 UTC.",
        status=IncidentStatus.RESOLVED,
        severity=Severity.SEV2,
        created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
        resolved_at=datetime(2026, 5, 20, 10, 30, tzinfo=UTC),
    )


def _events() -> list[IncidentEvent]:
    return [
        IncidentEvent(
            timestamp=datetime(2026, 5, 20, 9, 2, tzinfo=UTC),
            type_="status_changed",
            description="status: open -> investigating",
        ),
        IncidentEvent(
            timestamp=datetime(2026, 5, 20, 9, 5, tzinfo=UTC),
            type_="comment_added",
            description="Recent deploy to checkout-service at 08:55 UTC may be related.",
        ),
        IncidentEvent(
            timestamp=datetime(2026, 5, 20, 9, 20, tzinfo=UTC),
            type_="comment_added",
            description="Rolled back checkout-service; error rate dropping.",
        ),
        IncidentEvent(
            timestamp=datetime(2026, 5, 20, 10, 30, tzinfo=UTC),
            type_="status_changed",
            description="status: investigating -> resolved",
        ),
    ]


async def _run(llm_client: LLMClient, incident: Incident, task: PromptTask):
    prompt = PromptBuilder().build(incident, _events(), task)
    return await llm_client.generate(
        prompt.user, system=prompt.system, response_model=prompt.response_model
    )


@pytest.mark.integration
@pytest.mark.flaky(reruns=3)
async def test_summary_end_to_end(llm_client: LLMClient):
    result = await _run(llm_client, _open_incident(), PromptTask.SUMMARY)
    assert isinstance(result, SummaryResponse)
    assert result.summary.strip() != ""


@pytest.mark.integration
@pytest.mark.flaky(reruns=3)
async def test_severity_suggestion_end_to_end(llm_client: LLMClient):
    result = await _run(llm_client, _open_incident(), PromptTask.SEVERITY_SUGGESTION)
    assert isinstance(result, SeverityResponse)
    # severity is constrained to the SEV1-SEV4 Literal via response_model schema.
    assert result.reason.strip() != ""


@pytest.mark.integration
@pytest.mark.flaky(reruns=3)
async def test_solution_suggestions_end_to_end(llm_client: LLMClient):
    result = await _run(llm_client, _open_incident(), PromptTask.SOLUTION_SUGGESTIONS)
    assert isinstance(result, SolutionsResponse)
    assert len(result.solutions) >= 1
    assert all(s.strip() != "" for s in result.solutions)


@pytest.mark.integration
@pytest.mark.flaky(reruns=3)
async def test_postmortem_end_to_end(llm_client: LLMClient):
    result = await _run(llm_client, _resolved_incident(), PromptTask.POSTMORTEM)
    assert isinstance(result, PostmortemResponse)
    assert result.root_cause.strip() != ""
    assert len(result.timeline) >= 1
    assert len(result.action_items) >= 1
