from uuid import UUID

import structlog

from client import Client
from client.api.incidents import (
    get_incident,
    list_incident_events,
    write_incident_postmortem,
    write_incident_solutions,
    write_incident_summary,
)
from client.models import (
    Incident,
    IncidentEvent,
    PostmortemPatch,
    SolutionsPatch,
    SummaryPatch,
)
from genai_service.ollama_client import OllamaClient
from genai_service.prompts import (
    PostmortemResponse,
    PromptBuilder,
    PromptTask,
    SolutionsResponse,
    SummaryResponse,
)

logger = structlog.get_logger(__name__)


class IncidentHandlers:
    """Orchestrates NATS events -> fetch incident -> build prompt -> generate -> patch back.

    Each handler corresponds to one NATS subject. Errors are logged and swallowed so
    a poisoned event does not crash the consumer; we rely on incident-service to
    retry (e.g. by republishing) if a generation fails.

    Talks to incident-service through the generated OpenAPI client
    (services/generated/python-client) instead of a hand-written facade, so the
    HTTP surface stays in lockstep with the spec.
    """

    def __init__(
        self,
        incident_api_client: Client,
        ollama_client: OllamaClient,
        prompt_builder: PromptBuilder,
    ) -> None:
        self._client = incident_api_client
        self._ollama = ollama_client
        self._prompts = prompt_builder

    async def on_incident_created(self, incident_id: str) -> None:
        await self._summary_and_solutions(incident_id, trigger="incident.created")

    async def on_incident_resolved(self, incident_id: str) -> None:
        log = logger.bind(incident_id=incident_id, trigger="incident.resolved")
        try:
            incident, events = await self._fetch(incident_id)
            prompt = self._prompts.build(incident, events, PromptTask.POSTMORTEM)
            result = await self._ollama.generate(
                prompt.user, system=prompt.system, response_model=PostmortemResponse
            )
            await write_incident_postmortem.asyncio_detailed(
                incident_id=_uuid(incident_id),
                client=self._client,
                body=PostmortemPatch(
                    root_cause=result.root_cause,
                    timeline=result.timeline,
                    action_items=result.action_items,
                ),
            )
            log.info("postmortem_generated")
        except Exception as exc:
            log.error("postmortem_failed", error=str(exc))

    async def on_regen_requested(self, incident_id: str) -> None:
        await self._summary_and_solutions(
            incident_id, trigger="incident.regen.requested"
        )

    async def _summary_and_solutions(self, incident_id: str, *, trigger: str) -> None:
        log = logger.bind(incident_id=incident_id, trigger=trigger)
        try:
            incident, events = await self._fetch(incident_id)

            summary_prompt = self._prompts.build(incident, events, PromptTask.SUMMARY)
            summary = await self._ollama.generate(
                summary_prompt.user,
                system=summary_prompt.system,
                response_model=SummaryResponse,
            )
            await write_incident_summary.asyncio_detailed(
                incident_id=_uuid(incident_id),
                client=self._client,
                body=SummaryPatch(summary=summary.summary),
            )

            solutions_prompt = self._prompts.build(
                incident, events, PromptTask.SOLUTION_SUGGESTIONS
            )
            solutions = await self._ollama.generate(
                solutions_prompt.user,
                system=solutions_prompt.system,
                response_model=SolutionsResponse,
            )
            await write_incident_solutions.asyncio_detailed(
                incident_id=_uuid(incident_id),
                client=self._client,
                body=SolutionsPatch(solutions=list(solutions.solutions)),
            )

            log.info("summary_and_solutions_generated")
        except Exception as exc:
            log.error("summary_and_solutions_failed", error=str(exc))

    async def _fetch(self, incident_id: str) -> tuple[Incident, list[IncidentEvent]]:
        incident = await get_incident.asyncio(
            incident_id=_uuid(incident_id), client=self._client
        )
        if not isinstance(incident, Incident):
            raise RuntimeError(
                f"incident-service returned no incident for {incident_id}"
            )
        events = await list_incident_events.asyncio(
            incident_id=_uuid(incident_id), client=self._client
        )
        if not isinstance(events, list):
            raise RuntimeError(f"incident-service returned no events for {incident_id}")
        return incident, events


def _uuid(incident_id: str) -> UUID:
    # NATS payloads carry incidentId as a string; incident-service uses UUIDs.
    # Generated client expects UUID; fail fast if the payload is malformed.
    return UUID(incident_id)
