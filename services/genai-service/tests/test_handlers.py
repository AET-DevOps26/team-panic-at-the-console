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
    SolutionsResponse,
    SummaryResponse,
)

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
    patches: list[dict] | None = None,
) -> Client:
    """Wires a `Client` to an httpx MockTransport that serves GET incident/events
    and records PATCH bodies into the supplied `patches` list."""

    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        if request.method == "GET" and path.endswith(f"/incidents/{INCIDENT_ID}"):
            return httpx.Response(200, json=incident)
        if request.method == "GET" and path.endswith(
            f"/incidents/{INCIDENT_ID}/events"
        ):
            return httpx.Response(200, json=events or [])
        if request.method == "PATCH" and "/genai/" in path:
            if patches is not None:
                patches.append({"path": path, "body": json.loads(request.content)})
            return httpx.Response(204)
        return httpx.Response(404)

    return Client(
        base_url="http://incident-service",
        httpx_args={"transport": httpx.MockTransport(handler)},
    )


@pytest.fixture()
def ollama() -> AsyncMock:
    return AsyncMock()


async def test_on_created_generates_summary_and_solutions_and_patches(ollama):
    patches: list[dict] = []
    c = _client_with(incident=_incident_json(), events=_events_json(), patches=patches)
    ollama.generate.side_effect = [
        SummaryResponse(summary="things are bad"),
        SolutionsResponse(solutions=["restart", "rollback"]),
    ]
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_created(str(INCIDENT_ID))

    paths = [p["path"] for p in patches]
    assert any(p.endswith("/genai/summary/result") for p in paths)
    assert any(p.endswith("/genai/solutions/result") for p in paths)
    by_path = {p["path"].rsplit("/", 2)[-2]: p["body"] for p in patches}
    assert by_path["summary"] == {"summary": "things are bad"}
    assert by_path["solutions"] == {"solutions": ["restart", "rollback"]}
    assert ollama.generate.await_count == 2


async def test_on_resolved_generates_postmortem(ollama):
    patches: list[dict] = []
    c = _client_with(
        incident=_incident_json(
            status=IncidentStatus.RESOLVED, resolved_at="2026-05-20T10:00:00+00:00"
        ),
        events=_events_json(),
        patches=patches,
    )
    ollama.generate.return_value = PostmortemResponse(
        root_cause="bad config", timeline=["t1"], action_items=["a1"]
    )
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_resolved(str(INCIDENT_ID))

    assert len(patches) == 1
    assert patches[0]["path"].endswith("/genai/postmortem/result")
    assert patches[0]["body"] == {
        "rootCause": "bad config",
        "timeline": ["t1"],
        "actionItems": ["a1"],
    }


async def test_on_regen_requested_runs_summary_path(ollama):
    patches: list[dict] = []
    c = _client_with(incident=_incident_json(), events=_events_json(), patches=patches)
    ollama.generate.side_effect = [
        SummaryResponse(summary="s"),
        SolutionsResponse(solutions=["a"]),
    ]
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_regen_requested(str(INCIDENT_ID))

    suffixes = {p["path"].rsplit("/", 2)[-2] for p in patches}
    assert suffixes == {"summary", "solutions"}


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


async def test_on_resolved_skips_when_incident_not_resolved(ollama):
    """PromptBuilder rejects postmortem on unresolved incident; handler logs and stops."""
    patches: list[dict] = []
    c = _client_with(
        incident=_incident_json(status=IncidentStatus.OPEN),
        events=_events_json(),
        patches=patches,
    )
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_resolved(str(INCIDENT_ID))

    ollama.generate.assert_not_awaited()
    assert patches == []


async def test_handler_rejects_non_uuid_incident_id(ollama):
    c = _client_with(incident=_incident_json(), events=_events_json())
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_created("not-a-uuid")

    ollama.generate.assert_not_awaited()


async def test_on_created_does_not_log_success_when_patch_returns_404(ollama):
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        if request.method == "GET" and path.endswith(f"/incidents/{INCIDENT_ID}"):
            return httpx.Response(200, json=_incident_json())
        if request.method == "GET" and path.endswith("/events"):
            return httpx.Response(200, json=_events_json())
        if request.method == "PATCH":
            return httpx.Response(404)
        return httpx.Response(404)

    c = Client(
        base_url="http://incident-service",
        httpx_args={"transport": httpx.MockTransport(handler)},
    )
    ollama.generate.return_value = SummaryResponse(summary="s")
    handlers = IncidentHandlers(c, ollama, PromptBuilder())

    await handlers.on_incident_created(str(INCIDENT_ID))

    assert ollama.generate.await_count == 1
