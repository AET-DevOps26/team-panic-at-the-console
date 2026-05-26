from datetime import UTC, datetime
from uuid import UUID

import pytest

from client.models import Incident, IncidentEvent, IncidentStatus, Severity
from genai_service.prompts import (
    PostmortemResponse,
    Prompt,
    PromptBuilder,
    PromptTask,
    SeverityResponse,
    SolutionsResponse,
    SummaryResponse,
)

INCIDENT_ID = UUID("018e2c5f-1234-7abc-8def-000000000001")


def _incident(
    status: IncidentStatus = IncidentStatus.OPEN,
    resolved_at: datetime | None = None,
) -> Incident:
    return Incident(
        id=INCIDENT_ID,
        title="Checkout service 5xx spike",
        description="Errors on /checkout up sharply.",
        status=status,
        severity=Severity.SEV2,
        created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
        resolved_at=resolved_at,
    )


def _event(
    minute: int, type_: str = "comment_added", desc: str = "looking into it"
) -> IncidentEvent:
    return IncidentEvent(
        timestamp=datetime(2026, 5, 20, 9, minute, tzinfo=UTC),
        type_=type_,
        description=desc,
    )


@pytest.mark.parametrize(
    "task,model",
    [
        (PromptTask.SUMMARY, SummaryResponse),
        (PromptTask.SEVERITY_SUGGESTION, SeverityResponse),
        (PromptTask.SOLUTION_SUGGESTIONS, SolutionsResponse),
    ],
)
def test_build_picks_response_model_per_task(task, model):
    prompt = PromptBuilder().build(_incident(), [_event(1)], task)

    assert isinstance(prompt, Prompt)
    assert prompt.response_model is model
    assert prompt.system.strip() != ""


def test_build_includes_incident_metadata_in_user_prompt():
    prompt = PromptBuilder().build(_incident(), [_event(1)], PromptTask.SUMMARY)

    assert f"Incident {INCIDENT_ID}" in prompt.user
    assert "Checkout service 5xx spike" in prompt.user
    assert "Status: open" in prompt.user
    assert "Severity: SEV2" in prompt.user
    assert "Errors on /checkout" in prompt.user


def test_build_renders_events_chronologically():
    events = [
        _event(1, desc="EVT-alpha"),
        _event(5, desc="EVT-bravo"),
        _event(10, desc="EVT-charlie"),
    ]

    prompt = PromptBuilder().build(_incident(), events, PromptTask.SUMMARY)

    a, b, c = (prompt.user.index(s) for s in ("EVT-alpha", "EVT-bravo", "EVT-charlie"))
    assert a < b < c


def test_build_sorts_unordered_events_by_timestamp():
    events = [
        _event(10, desc="EVT-charlie"),
        _event(1, desc="EVT-alpha"),
        _event(5, desc="EVT-bravo"),
    ]

    prompt = PromptBuilder().build(_incident(), events, PromptTask.SUMMARY)

    a, b, c = (prompt.user.index(s) for s in ("EVT-alpha", "EVT-bravo", "EVT-charlie"))
    assert a < b < c


def test_build_truncates_keeps_newest_when_input_unsorted():
    events = [_event(i, desc=f"evt-{i:02d}") for i in (3, 10, 1, 7, 5)]

    prompt = PromptBuilder(max_events=2).build(_incident(), events, PromptTask.SUMMARY)

    assert "evt-07" in prompt.user
    assert "evt-10" in prompt.user
    assert "evt-01" not in prompt.user
    assert "evt-03" not in prompt.user
    assert "evt-05" not in prompt.user


def test_build_truncates_to_last_n_events_with_note():
    events = [_event(i, desc=f"evt-{i:02d}") for i in range(1, 11)]

    prompt = PromptBuilder(max_events=3).build(_incident(), events, PromptTask.SUMMARY)

    assert "showing last 3 of 10 events" in prompt.user
    assert "evt-08" in prompt.user
    assert "evt-09" in prompt.user
    assert "evt-10" in prompt.user
    assert "evt-01" not in prompt.user
    assert "evt-07" not in prompt.user


def test_build_handles_empty_event_log():
    prompt = PromptBuilder().build(_incident(), [], PromptTask.SUMMARY)
    assert "no events recorded" in prompt.user


def test_build_postmortem_includes_resolved_at_and_uses_postmortem_model():
    resolved = datetime(2026, 5, 20, 10, 0, tzinfo=UTC)
    incident = _incident(status=IncidentStatus.RESOLVED, resolved_at=resolved)

    prompt = PromptBuilder().build(incident, [_event(1)], PromptTask.POSTMORTEM)

    assert prompt.response_model is PostmortemResponse
    assert "Resolved at: 2026-05-20T10:00:00+00:00" in prompt.user


def test_build_postmortem_rejects_unresolved_incident():
    with pytest.raises(ValueError, match="resolved incident"):
        PromptBuilder().build(
            _incident(status=IncidentStatus.OPEN), [_event(1)], PromptTask.POSTMORTEM
        )


def test_max_events_must_be_positive():
    with pytest.raises(ValueError):
        PromptBuilder(max_events=0)


def test_each_task_uses_distinct_system_prompt():
    builder = PromptBuilder()
    incident = _incident(
        status=IncidentStatus.RESOLVED,
        resolved_at=datetime(2026, 5, 20, 10, 0, tzinfo=UTC),
    )
    events = [_event(1)]

    prompts = {
        task: builder.build(incident, events, task).system for task in PromptTask
    }
    assert len(set(prompts.values())) == len(PromptTask)
