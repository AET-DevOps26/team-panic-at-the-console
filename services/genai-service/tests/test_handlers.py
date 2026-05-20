from datetime import UTC, datetime
from unittest.mock import AsyncMock

import pytest

from genai_service.handlers import IncidentHandlers
from genai_service.prompts import (
    Event,
    Incident,
    PostmortemResponse,
    PromptBuilder,
    SolutionsResponse,
    SummaryResponse,
)

INCIDENT_ID = "inc-1"


def _incident(status="open", resolved_at=None) -> Incident:
    return Incident(
        id=INCIDENT_ID,
        title="Checkout 5xx",
        description="errors",
        status=status,
        severity="SEV2",
        created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
        resolved_at=resolved_at,
    )


def _events() -> list[Event]:
    return [
        Event(
            timestamp=datetime(2026, 5, 20, 9, 1, tzinfo=UTC),
            type="comment_added",
            description="looking into it",
        )
    ]


@pytest.fixture()
def deps():
    incidents = AsyncMock()
    ollama = AsyncMock()
    handlers = IncidentHandlers(incidents, ollama, PromptBuilder())
    return incidents, ollama, handlers


async def test_on_created_generates_summary_and_solutions_and_patches(deps):
    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident()
    incidents.get_events.return_value = _events()
    ollama.generate.side_effect = [
        SummaryResponse(summary="things are bad"),
        SolutionsResponse(solutions=["restart", "rollback"]),
    ]

    await handlers.on_incident_created(INCIDENT_ID)

    incidents.patch_summary.assert_awaited_once_with(INCIDENT_ID, "things are bad")
    incidents.patch_solutions.assert_awaited_once_with(
        INCIDENT_ID, ["restart", "rollback"]
    )
    assert ollama.generate.await_count == 2


async def test_on_resolved_generates_postmortem(deps):
    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident(
        status="resolved", resolved_at=datetime(2026, 5, 20, 10, 0, tzinfo=UTC)
    )
    incidents.get_events.return_value = _events()
    ollama.generate.return_value = PostmortemResponse(
        root_cause="bad config", timeline=["t1"], action_items=["a1"]
    )

    await handlers.on_incident_resolved(INCIDENT_ID)

    incidents.patch_postmortem.assert_awaited_once_with(
        INCIDENT_ID, root_cause="bad config", timeline=["t1"], action_items=["a1"]
    )


async def test_on_regen_requested_runs_summary_path(deps):
    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident()
    incidents.get_events.return_value = _events()
    ollama.generate.side_effect = [
        SummaryResponse(summary="s"),
        SolutionsResponse(solutions=["a"]),
    ]

    await handlers.on_regen_requested(INCIDENT_ID)

    incidents.patch_summary.assert_awaited_once()
    incidents.patch_solutions.assert_awaited_once()
    incidents.patch_postmortem.assert_not_awaited()


async def test_handler_swallows_errors(deps):
    """A poisoned event should not crash the consumer."""
    incidents, ollama, handlers = deps
    incidents.get_incident.side_effect = RuntimeError("boom")

    await handlers.on_incident_created(INCIDENT_ID)

    incidents.patch_summary.assert_not_awaited()


async def test_on_resolved_skips_when_incident_not_resolved(deps):
    """PromptBuilder rejects postmortem on unresolved incident; handler logs and stops."""
    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident(status="open")
    incidents.get_events.return_value = _events()

    await handlers.on_incident_resolved(INCIDENT_ID)

    ollama.generate.assert_not_awaited()
    incidents.patch_postmortem.assert_not_awaited()


async def test_on_created_aborts_when_get_events_fails(deps):
    """If the event log fetch fails after the incident fetch succeeds, no PATCH happens."""
    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident()
    incidents.get_events.side_effect = RuntimeError("event-service down")

    await handlers.on_incident_created(INCIDENT_ID)

    ollama.generate.assert_not_awaited()
    incidents.patch_summary.assert_not_awaited()
    incidents.patch_solutions.assert_not_awaited()


async def test_on_created_aborts_when_ollama_fails(deps):
    """An Ollama failure mid-handler must not leave half-written results on incident-service."""
    from genai_service.ollama_client import OllamaError

    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident()
    incidents.get_events.return_value = _events()
    ollama.generate.side_effect = OllamaError("ollama down")

    await handlers.on_incident_created(INCIDENT_ID)

    incidents.patch_summary.assert_not_awaited()
    incidents.patch_solutions.assert_not_awaited()


async def test_on_created_does_not_patch_solutions_when_summary_patch_fails(deps):
    """Pins the current behavior: a PATCH failure aborts the rest of the handler."""
    incidents, ollama, handlers = deps
    incidents.get_incident.return_value = _incident()
    incidents.get_events.return_value = _events()
    ollama.generate.side_effect = [
        SummaryResponse(summary="ok"),
        SolutionsResponse(solutions=["restart"]),
    ]
    incidents.patch_summary.side_effect = RuntimeError("incident-service 500")

    await handlers.on_incident_created(INCIDENT_ID)

    incidents.patch_summary.assert_awaited_once()
    incidents.patch_solutions.assert_not_awaited()
