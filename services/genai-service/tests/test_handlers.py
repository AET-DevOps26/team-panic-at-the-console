import json
from unittest.mock import AsyncMock
from uuid import UUID

import httpx
import pytest

from client import Client
from client.models import IncidentStatus, Severity
from genai_service.handlers import IncidentHandlers
from genai_service.prompts import (
    PostmortemResponse,
    PromptBuilder,
    SeverityResponse,
    SolutionsResponse,
    SummaryResponse,
)
from genai_service.regen_task import RegenTask

INCIDENT_ID = UUID("018e2c5f-1234-7abc-8def-000000000001")


def _incident_json(
    *, status: IncidentStatus = IncidentStatus.OPEN, resolved_at: str | None = None
) -> dict:
    body: dict = {
        "id": str(INCIDENT_ID),
        "title": "Checkout 5xx",
        "description": "errors",
        "status": status.value,
        "severity": Severity.SEV2.value,
        "createdAt": "2026-05-20T09:00:00+00:00",
    }
    if resolved_at is not None:
        body["resolvedAt"] = resolved_at
    return body


def _events_json() -> list[dict]:
    return [
        {
            "timestamp": "2026-05-20T09:01:00+00:00",
            "type": "comment_added",
            "description": "looking into it",
        }
    ]


def _client_with(
    *,
    incident: dict,
    events: list[dict] | None = None,
) -> Client:
    """Wires a Client to an httpx MockTransport that serves GET incident/events."""

    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        if request.method == "GET" and path.endswith(f"/incidents/{INCIDENT_ID}"):
            return httpx.Response(200, json=incident)
        if request.method == "GET" and path.endswith(
            f"/incidents/{INCIDENT_ID}/events"
        ):
            return httpx.Response(200, json=events or [])
        return httpx.Response(404)

    return Client(
        base_url="http://incident-service",
        httpx_args={"transport": httpx.MockTransport(handler)},
    )


@pytest.fixture()
def ollama() -> AsyncMock:
    mock = AsyncMock()
    mock.provider = "ollama"
    return mock


@pytest.fixture()
def mock_nats() -> AsyncMock:
    nc = AsyncMock()
    nc.publish = AsyncMock()
    return nc


def _published_payloads(mock_nats: AsyncMock) -> dict[str, dict]:
    """Return {subject: payload_dict} for all publish calls."""
    result = {}
    for call in mock_nats.publish.call_args_list:
        subject = call.args[0]
        payload = json.loads(call.args[1].decode())
        result[subject] = payload
    return result


async def test_on_created_generates_summary_severity_solutions_and_publishes(
    ollama, mock_nats
):
    c = _client_with(incident=_incident_json(), events=_events_json())
    ollama.generate.side_effect = [
        SummaryResponse(summary="things are bad"),
        SeverityResponse(severity=Severity.SEV1, reason="checkout down"),
        SolutionsResponse(solutions=["restart", "rollback"]),
    ]
    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)

    await handlers.on_incident_created(str(INCIDENT_ID))

    assert ollama.generate.await_count == 3
    payloads = _published_payloads(mock_nats)
    assert set(payloads) == {
        "incident.genai.summary.generated",
        "incident.genai.severity.generated",
        "incident.genai.solutions.generated",
    }
    assert payloads["incident.genai.summary.generated"]["summary"] == "things are bad"
    assert payloads["incident.genai.severity.generated"]["severity"] == "SEV1"
    assert payloads["incident.genai.severity.generated"]["reason"] == "checkout down"
    assert payloads["incident.genai.solutions.generated"]["solutions"] == [
        "restart",
        "rollback",
    ]


async def test_on_resolved_generates_postmortem(ollama, mock_nats):
    c = _client_with(
        incident=_incident_json(
            status=IncidentStatus.RESOLVED, resolved_at="2026-05-20T10:00:00+00:00"
        ),
        events=_events_json(),
    )
    ollama.generate.return_value = PostmortemResponse(
        root_cause="bad config", timeline=["t1"], action_items=["a1"]
    )
    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)

    await handlers.on_incident_resolved(str(INCIDENT_ID))

    mock_nats.publish.assert_awaited_once()
    subject, raw = mock_nats.publish.call_args.args
    assert subject == "incident.genai.postmortem.generated"
    payload = json.loads(raw.decode())
    assert payload["rootCause"] == "bad config"
    assert payload["timeline"] == ["t1"]
    assert payload["actionItems"] == ["a1"]


async def test_on_regen_requested_runs_only_requested_task(ollama, mock_nats):
    c = _client_with(incident=_incident_json(), events=_events_json())
    ollama.generate.return_value = SummaryResponse(summary="s")
    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)

    await handlers.on_regen_requested(str(INCIDENT_ID), RegenTask.SUMMARY)

    assert ollama.generate.await_count == 1
    mock_nats.publish.assert_awaited_once()
    subject, _ = mock_nats.publish.call_args.args
    assert subject == "incident.genai.summary.generated"


async def test_on_regen_postmortem_runs_postmortem_only(ollama, mock_nats):
    c = _client_with(
        incident=_incident_json(
            status=IncidentStatus.RESOLVED, resolved_at="2026-05-20T10:00:00+00:00"
        ),
        events=_events_json(),
    )
    ollama.generate.return_value = PostmortemResponse(
        root_cause="x", timeline=["t"], action_items=["a"]
    )
    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)

    await handlers.on_regen_requested(str(INCIDENT_ID), RegenTask.POSTMORTEM)

    mock_nats.publish.assert_awaited_once()
    subject, _ = mock_nats.publish.call_args.args
    assert subject == "incident.genai.postmortem.generated"


async def test_handler_swallows_errors(ollama):
    """A poisoned event should not crash the consumer."""

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500)

    c = Client(
        base_url="http://incident-service",
        httpx_args={"transport": httpx.MockTransport(handler)},
    )
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_created(str(INCIDENT_ID))

    ollama.generate.assert_not_awaited()


async def test_on_resolved_skips_when_incident_not_resolved(ollama, mock_nats):
    """Handler skips postmortem generation when incident is still open."""
    c = _client_with(
        incident=_incident_json(status=IncidentStatus.OPEN),
        events=_events_json(),
    )
    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)

    await handlers.on_incident_resolved(str(INCIDENT_ID))

    ollama.generate.assert_not_awaited()
    mock_nats.publish.assert_not_awaited()


async def test_handler_rejects_non_uuid_incident_id(ollama):
    c = _client_with(incident=_incident_json(), events=_events_json())
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_created("not-a-uuid")

    ollama.generate.assert_not_awaited()


async def test_on_created_continues_after_first_publish_fails(ollama, mock_nats):
    """A failed publish for one task should not prevent the others from running."""
    c = _client_with(incident=_incident_json(), events=_events_json())
    ollama.generate.side_effect = [
        SummaryResponse(summary="s"),
        SeverityResponse(severity=Severity.SEV2, reason="r"),
        SolutionsResponse(solutions=["a"]),
    ]
    mock_nats.publish.side_effect = [RuntimeError("publish failed"), None, None]
    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)

    await handlers.on_incident_created(str(INCIDENT_ID))

    assert ollama.generate.await_count == 3
    assert mock_nats.publish.await_count == 3


async def test_on_created_records_generation_metrics(ollama, mock_nats):
    from genai_service.metrics import ai_generations_total

    c = _client_with(incident=_incident_json(), events=_events_json())
    ollama.generate.side_effect = [
        SummaryResponse(summary="ok"),
        SeverityResponse(severity=Severity.SEV2, reason="r"),
        SolutionsResponse(solutions=["restart"]),
    ]

    provider = "ollama"
    before_summary = ai_generations_total.labels(
        type="summary", provider=provider, outcome="success"
    )._value.get()
    before_severity = ai_generations_total.labels(
        type="severity_suggestion", provider=provider, outcome="success"
    )._value.get()
    before_solutions = ai_generations_total.labels(
        type="solution_suggestions", provider=provider, outcome="success"
    )._value.get()

    handlers = IncidentHandlers(c, ollama, PromptBuilder(), nats_client=mock_nats)
    await handlers.on_incident_created(str(INCIDENT_ID))

    assert (
        ai_generations_total.labels(
            type="summary", provider=provider, outcome="success"
        )._value.get()
        == before_summary + 1
    )
    assert (
        ai_generations_total.labels(
            type="severity_suggestion", provider=provider, outcome="success"
        )._value.get()
        == before_severity + 1
    )
    assert (
        ai_generations_total.labels(
            type="solution_suggestions", provider=provider, outcome="success"
        )._value.get()
        == before_solutions + 1
    )


async def test_on_created_records_error_outcome_when_llm_fails(ollama):
    from genai_service.metrics import ai_generations_total
    from genai_service.ollama_client import OllamaError

    c = _client_with(incident=_incident_json(), events=_events_json())
    ollama.generate.side_effect = OllamaError("ollama down")

    before = ai_generations_total.labels(
        type="summary", provider="ollama", outcome="error"
    )._value.get()

    handlers = IncidentHandlers(c, ollama, PromptBuilder())
    await handlers.on_incident_created(str(INCIDENT_ID))

    assert (
        ai_generations_total.labels(
            type="summary", provider="ollama", outcome="error"
        )._value.get()
        == before + 1
    )


async def test_on_created_records_logos_provider_via_fallback_client(mock_nats):
    from genai_service.llm import FallbackLLMClient
    from genai_service.metrics import ai_generations_total

    primary = AsyncMock()
    primary.model = "logos-model"
    primary.provider = "logos"
    primary.generate.return_value = SummaryResponse(summary="ok")

    backup = AsyncMock()
    backup.model = "ollama-model"
    backup.provider = "ollama"

    llm = FallbackLLMClient(primary, backup)
    c = _client_with(incident=_incident_json(), events=_events_json())
    primary.generate.side_effect = [
        SummaryResponse(summary="ok"),
        SeverityResponse(severity=Severity.SEV2, reason="r"),
        SolutionsResponse(solutions=["restart"]),
    ]

    before = ai_generations_total.labels(
        type="summary", provider="logos", outcome="success"
    )._value.get()

    handlers = IncidentHandlers(c, llm, PromptBuilder(), nats_client=mock_nats)
    await handlers.on_incident_created(str(INCIDENT_ID))

    assert (
        ai_generations_total.labels(
            type="summary", provider="logos", outcome="success"
        )._value.get()
        == before + 1
    )


async def test_on_created_records_ollama_provider_after_fallback(mock_nats):
    from genai_service.llm import FallbackLLMClient
    from genai_service.metrics import ai_generations_total

    primary = AsyncMock()
    primary.model = "logos-model"
    primary.provider = "logos"
    primary.generate.side_effect = RuntimeError("logos down")

    backup = AsyncMock()
    backup.model = "ollama-model"
    backup.provider = "ollama"
    backup.generate.return_value = SummaryResponse(summary="ok")

    llm = FallbackLLMClient(primary, backup)
    c = _client_with(incident=_incident_json(), events=_events_json())
    backup.generate.side_effect = [
        SummaryResponse(summary="ok"),
        SeverityResponse(severity=Severity.SEV2, reason="r"),
        SolutionsResponse(solutions=["restart"]),
    ]

    before = ai_generations_total.labels(
        type="summary", provider="ollama", outcome="success"
    )._value.get()

    handlers = IncidentHandlers(c, llm, PromptBuilder(), nats_client=mock_nats)
    await handlers.on_incident_created(str(INCIDENT_ID))

    assert (
        ai_generations_total.labels(
            type="summary", provider="ollama", outcome="success"
        )._value.get()
        == before + 1
    )


async def test_on_created_records_error_when_patch_fails(ollama):
    from genai_service.metrics import ai_generations_total

    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        if request.method == "GET" and path.endswith(f"/incidents/{INCIDENT_ID}"):
            return httpx.Response(200, json=_incident_json())
        if request.method == "GET" and path.endswith(
            f"/incidents/{INCIDENT_ID}/events"
        ):
            return httpx.Response(200, json=_events_json())
        if request.method == "PATCH" and path.endswith("/genai/summary"):
            return httpx.Response(500, json={"error": "db down"})
        return httpx.Response(404)

    c = Client(
        base_url="http://incident-service",
        httpx_args={"transport": httpx.MockTransport(handler)},
    )
    ollama.generate.return_value = SummaryResponse(summary="ok")

    before = ai_generations_total.labels(
        type="summary", provider="ollama", outcome="error"
    )._value.get()

    handlers = IncidentHandlers(c, ollama, PromptBuilder())
    await handlers.on_incident_created(str(INCIDENT_ID))

    assert (
        ai_generations_total.labels(
            type="summary", provider="ollama", outcome="error"
        )._value.get()
        == before + 1
    )
