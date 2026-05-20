import json
from datetime import UTC, datetime

import httpx
import pytest

from genai_service.incident_client import IncidentServiceClient, IncidentServiceError

INCIDENT_ID = "018e2c5f-1234-7abc-8def-000000000001"

_INCIDENT_JSON = {
    "id": INCIDENT_ID,
    "title": "Checkout 5xx",
    "description": "errors on /checkout",
    "status": "open",
    "severity": "SEV2",
    "createdAt": "2026-05-20T09:00:00+00:00",
    "resolvedAt": None,
}

_EVENTS_JSON = [
    {
        "timestamp": "2026-05-20T09:01:00+00:00",
        "type": "comment_added",
        "description": "investigating",
    }
]


def _client(handler) -> IncidentServiceClient:
    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    return IncidentServiceClient(http, "http://incident-service:8081")


async def test_get_incident_parses_response():
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.method == "GET"
        assert (
            str(request.url) == f"http://incident-service:8081/incidents/{INCIDENT_ID}"
        )
        return httpx.Response(200, json=_INCIDENT_JSON)

    incident = await _client(handler).get_incident(INCIDENT_ID)

    assert incident.id == INCIDENT_ID
    assert incident.severity == "SEV2"
    assert incident.created_at == datetime(2026, 5, 20, 9, 0, tzinfo=UTC)


async def test_get_events_parses_response():
    def handler(request: httpx.Request) -> httpx.Response:
        assert str(request.url).endswith(f"/incidents/{INCIDENT_ID}/events")
        return httpx.Response(200, json=_EVENTS_JSON)

    events = await _client(handler).get_events(INCIDENT_ID)

    assert len(events) == 1
    assert events[0].type == "comment_added"


async def test_get_events_rejects_non_array():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"oops": "not an array"})

    with pytest.raises(IncidentServiceError, match="expected JSON array"):
        await _client(handler).get_events(INCIDENT_ID)


async def test_get_incident_raises_on_404():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, text="not found")

    with pytest.raises(IncidentServiceError, match="returned 404"):
        await _client(handler).get_incident(INCIDENT_ID)


async def test_patch_summary_sends_expected_body():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["method"] = request.method
        captured["url"] = str(request.url)
        captured["body"] = json.loads(request.content)
        return httpx.Response(204)

    await _client(handler).patch_summary(INCIDENT_ID, "the summary")

    assert captured["method"] == "PATCH"
    assert captured["url"].endswith(f"/incidents/{INCIDENT_ID}/genai/summary/result")
    assert captured["body"] == {"summary": "the summary"}


async def test_patch_postmortem_uses_camel_case_keys():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = json.loads(request.content)
        return httpx.Response(204)

    await _client(handler).patch_postmortem(
        INCIDENT_ID,
        root_cause="bad config",
        timeline=["a", "b"],
        action_items=["fix it"],
    )

    assert captured["body"] == {
        "rootCause": "bad config",
        "timeline": ["a", "b"],
        "actionItems": ["fix it"],
    }


async def test_patch_raises_on_non_2xx():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, text="boom")

    with pytest.raises(IncidentServiceError, match="returned 500"):
        await _client(handler).patch_summary(INCIDENT_ID, "x")
