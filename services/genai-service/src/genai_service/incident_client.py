from collections.abc import Sequence

import httpx

from genai_service.prompts.models import Event, Incident


class IncidentServiceError(RuntimeError):
    """Raised when incident-service returns a non-2xx response or unparsable body."""


class IncidentServiceClient:
    """Thin REST wrapper around the endpoints genai-service consumes on incident-service.

    incident-service is the source of truth for incident state (see CONTEXT.md /
    ADR-0002). We fetch the incident + its event log to build prompts, then PATCH
    the AI-generated fields back.
    """

    def __init__(
        self,
        http: httpx.AsyncClient,
        base_url: str,
        timeout_seconds: float = 10.0,
    ) -> None:
        self._http = http
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout_seconds

    async def get_incident(self, incident_id: str) -> Incident:
        data = await self._get_json(f"/incidents/{incident_id}")
        try:
            return Incident.model_validate(data)
        except Exception as exc:
            raise IncidentServiceError(
                f"GET /incidents/{incident_id}: response did not match Incident schema: {exc}"
            ) from exc

    async def get_events(self, incident_id: str) -> list[Event]:
        data = await self._get_json(f"/incidents/{incident_id}/events")
        if not isinstance(data, list):
            raise IncidentServiceError(
                f"GET /incidents/{incident_id}/events: expected JSON array, got {type(data).__name__}"
            )
        try:
            return [Event.model_validate(item) for item in data]
        except Exception as exc:
            raise IncidentServiceError(
                f"GET /incidents/{incident_id}/events: event did not match Event schema: {exc}"
            ) from exc

    async def patch_summary(self, incident_id: str, summary: str) -> None:
        await self._patch(
            f"/incidents/{incident_id}/genai/summary/result", {"summary": summary}
        )

    async def patch_severity(
        self, incident_id: str, severity: str, reason: str
    ) -> None:
        await self._patch(
            f"/incidents/{incident_id}/genai/severity/result",
            {"severity": severity, "reason": reason},
        )

    async def patch_solutions(self, incident_id: str, solutions: Sequence[str]) -> None:
        await self._patch(
            f"/incidents/{incident_id}/genai/solutions/result",
            {"solutions": list(solutions)},
        )

    async def patch_postmortem(
        self,
        incident_id: str,
        root_cause: str,
        timeline: Sequence[str],
        action_items: Sequence[str],
    ) -> None:
        await self._patch(
            f"/incidents/{incident_id}/genai/postmortem/result",
            {
                "rootCause": root_cause,
                "timeline": list(timeline),
                "actionItems": list(action_items),
            },
        )

    async def _get_json(self, path: str) -> object:
        try:
            resp = await self._http.get(self._url(path), timeout=self._timeout)
        except httpx.HTTPError as exc:
            raise IncidentServiceError(f"GET {path} failed: {exc}") from exc
        if resp.status_code != 200:
            raise IncidentServiceError(
                f"GET {path} returned {resp.status_code}: {resp.text[:200]}"
            )
        try:
            return resp.json()
        except ValueError as exc:
            raise IncidentServiceError(f"GET {path}: invalid JSON: {exc}") from exc

    async def _patch(self, path: str, body: dict[str, object]) -> None:
        try:
            resp = await self._http.patch(
                self._url(path), json=body, timeout=self._timeout
            )
        except httpx.HTTPError as exc:
            raise IncidentServiceError(f"PATCH {path} failed: {exc}") from exc
        if resp.status_code not in (200, 204):
            raise IncidentServiceError(
                f"PATCH {path} returned {resp.status_code}: {resp.text[:200]}"
            )

    def _url(self, path: str) -> str:
        return f"{self._base_url}{path}"
