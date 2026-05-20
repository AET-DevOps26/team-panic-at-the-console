import structlog

from genai_service.incident_client import IncidentServiceClient
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
    """

    def __init__(
        self,
        incident_client: IncidentServiceClient,
        ollama_client: OllamaClient,
        prompt_builder: PromptBuilder,
    ) -> None:
        self._incidents = incident_client
        self._ollama = ollama_client
        self._prompts = prompt_builder

    async def on_incident_created(self, incident_id: str) -> None:
        await self._summary_and_solutions(incident_id, trigger="incident.created")

    async def on_incident_resolved(self, incident_id: str) -> None:
        log = logger.bind(incident_id=incident_id, trigger="incident.resolved")
        try:
            incident = await self._incidents.get_incident(incident_id)
            events = await self._incidents.get_events(incident_id)
            prompt = self._prompts.build(incident, events, PromptTask.POSTMORTEM)
            result = await self._ollama.generate(
                prompt.user, system=prompt.system, response_model=PostmortemResponse
            )
            await self._incidents.patch_postmortem(
                incident_id,
                root_cause=result.root_cause,
                timeline=result.timeline,
                action_items=result.action_items,
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
            incident = await self._incidents.get_incident(incident_id)
            events = await self._incidents.get_events(incident_id)

            summary_prompt = self._prompts.build(incident, events, PromptTask.SUMMARY)
            summary = await self._ollama.generate(
                summary_prompt.user,
                system=summary_prompt.system,
                response_model=SummaryResponse,
            )
            await self._incidents.patch_summary(incident_id, summary.summary)

            solutions_prompt = self._prompts.build(
                incident, events, PromptTask.SOLUTION_SUGGESTIONS
            )
            solutions = await self._ollama.generate(
                solutions_prompt.user,
                system=solutions_prompt.system,
                response_model=SolutionsResponse,
            )
            await self._incidents.patch_solutions(incident_id, solutions.solutions)

            log.info("summary_and_solutions_generated")
        except Exception as exc:
            log.error("summary_and_solutions_failed", error=str(exc))
