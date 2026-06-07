import asyncio
from http import HTTPStatus
from uuid import UUID

import structlog

from client import Client
from client.api.incidents import (
    get_incident,
    list_incident_events,
    write_incident_postmortem,
    write_incident_severity_suggestion,
    write_incident_solutions,
    write_incident_summary,
)
from client.models import (
    Incident,
    IncidentEvent,
    IncidentStatus,
    PostmortemPatch,
    SeverityPatch,
    SolutionsPatch,
    SummaryPatch,
)
from client.types import Response
from genai_service.llm import LLMClient
from genai_service.metrics import time_generation
from genai_service.prompts import (
    PostmortemResponse,
    PromptBuilder,
    PromptTask,
    SeverityResponse,
    SolutionsResponse,
    SummaryResponse,
)
from genai_service.regen_task import RegenTask

logger = structlog.get_logger(__name__)

_POSTMORTEM_RESOLVE_RETRIES = 3
_POSTMORTEM_RESOLVE_DELAY_SECONDS = 0.5


class IncidentHandlers:
    """Orchestrates NATS events -> fetch incident -> build prompt -> generate -> patch back.

    Each handler corresponds to one NATS subject. Errors are logged and swallowed per task so
    a poisoned event does not crash the consumer.

    Talks to incident-service through the generated OpenAPI client
    (services/generated/python-client) instead of a hand-written facade, so the
    HTTP surface stays in lockstep with the spec.
    """

    def __init__(
        self,
        incident_api_client: Client,
        llm_client: LLMClient,
        prompt_builder: PromptBuilder,
    ) -> None:
        self._client = incident_api_client
        self._llm = llm_client
        self._prompts = prompt_builder

    async def on_incident_created(self, incident_id: str) -> None:
        await self._run_tasks(
            incident_id,
            (
                PromptTask.SUMMARY,
                PromptTask.SEVERITY_SUGGESTION,
                PromptTask.SOLUTION_SUGGESTIONS,
            ),
            trigger="incident.created",
        )

    async def on_incident_resolved(self, incident_id: str) -> None:
        log = logger.bind(incident_id=incident_id, trigger="incident.resolved")
        for attempt in range(_POSTMORTEM_RESOLVE_RETRIES):
            try:
                incident, events = await self._fetch(incident_id)
                if incident.status is not IncidentStatus.RESOLVED:
                    if attempt < _POSTMORTEM_RESOLVE_RETRIES - 1:
                        await asyncio.sleep(_POSTMORTEM_RESOLVE_DELAY_SECONDS)
                        continue
                    log.warning(
                        "postmortem_skipped_not_resolved",
                        status=incident.status.value,
                    )
                    return
                await self._generate_postmortem(incident_id, incident, events)
                log.info("postmortem_generated")
                return
            except Exception as exc:
                log.error("postmortem_failed", error=str(exc))
                return

    async def on_regen_requested(self, incident_id: str, task: RegenTask) -> None:
        prompt_task = _REGEN_TO_PROMPT[task]
        await self._run_tasks(
            incident_id,
            (prompt_task,),
            trigger="incident.regen.requested",
            regen_task=task.value,
        )

    async def _run_tasks(
        self,
        incident_id: str,
        tasks: tuple[PromptTask, ...],
        *,
        trigger: str,
        regen_task: str | None = None,
    ) -> None:
        log = logger.bind(
            incident_id=incident_id, trigger=trigger, regen_task=regen_task
        )
        try:
            incident, events = await self._fetch(incident_id)
        except Exception as exc:
            log.error("fetch_failed", error=str(exc))
            return

        for task in tasks:
            try:
                await self._generate_and_patch(incident_id, incident, events, task)
            except Exception as exc:
                log.error("task_failed", task=task.value, error=str(exc))

        log.info("tasks_completed", tasks=[t.value for t in tasks])

    async def _generate_and_patch(
        self,
        incident_id: str,
        incident: Incident,
        events: list[IncidentEvent],
        task: PromptTask,
    ) -> None:
        match task:
            case PromptTask.POSTMORTEM:
                await self._generate_postmortem(incident_id, incident, events)
            case PromptTask.SUMMARY:
                await self._patch_summary(incident_id, incident, events)
            case PromptTask.SEVERITY_SUGGESTION:
                await self._patch_severity_suggestion(incident_id, incident, events)
            case PromptTask.SOLUTION_SUGGESTIONS:
                await self._patch_solutions(incident_id, incident, events)
            case _:
                raise ValueError(f"unsupported prompt task: {task}")

    async def _patch_summary(
        self, incident_id: str, incident: Incident, events: list[IncidentEvent]
    ) -> None:
        prompt = self._prompts.build(incident, events, PromptTask.SUMMARY)
        with time_generation(PromptTask.SUMMARY.value):
            result = await self._llm.generate(
                prompt.user, system=prompt.system, response_model=SummaryResponse
            )
        response = await write_incident_summary.asyncio_detailed(
            incident_id=_uuid(incident_id),
            client=self._client,
            body=SummaryPatch(summary=result.summary),
        )
        _expect_no_content(response, operation="write summary")

    async def _patch_severity_suggestion(
        self, incident_id: str, incident: Incident, events: list[IncidentEvent]
    ) -> None:
        prompt = self._prompts.build(incident, events, PromptTask.SEVERITY_SUGGESTION)
        with time_generation(PromptTask.SEVERITY_SUGGESTION.value):
            result = await self._llm.generate(
                prompt.user, system=prompt.system, response_model=SeverityResponse
            )
        response = await write_incident_severity_suggestion.asyncio_detailed(
            incident_id=_uuid(incident_id),
            client=self._client,
            body=SeverityPatch(severity=result.severity, reason=result.reason),
        )
        _expect_no_content(response, operation="write severity suggestion")

    async def _patch_solutions(
        self, incident_id: str, incident: Incident, events: list[IncidentEvent]
    ) -> None:
        prompt = self._prompts.build(incident, events, PromptTask.SOLUTION_SUGGESTIONS)
        with time_generation(PromptTask.SOLUTION_SUGGESTIONS.value):
            result = await self._llm.generate(
                prompt.user, system=prompt.system, response_model=SolutionsResponse
            )
        response = await write_incident_solutions.asyncio_detailed(
            incident_id=_uuid(incident_id),
            client=self._client,
            body=SolutionsPatch(solutions=list(result.solutions)),
        )
        _expect_no_content(response, operation="write solutions")

    async def _generate_postmortem(
        self,
        incident_id: str,
        incident: Incident,
        events: list[IncidentEvent],
    ) -> None:
        prompt = self._prompts.build(incident, events, PromptTask.POSTMORTEM)
        with time_generation(PromptTask.POSTMORTEM.value):
            result = await self._llm.generate(
                prompt.user, system=prompt.system, response_model=PostmortemResponse
            )
        response = await write_incident_postmortem.asyncio_detailed(
            incident_id=_uuid(incident_id),
            client=self._client,
            body=PostmortemPatch(
                root_cause=result.root_cause,
                timeline=result.timeline,
                action_items=result.action_items,
            ),
        )
        _expect_no_content(response, operation="write postmortem")

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


_REGEN_TO_PROMPT: dict[RegenTask, PromptTask] = {
    RegenTask.SUMMARY: PromptTask.SUMMARY,
    RegenTask.SEVERITY_SUGGESTION: PromptTask.SEVERITY_SUGGESTION,
    RegenTask.SOLUTION_SUGGESTIONS: PromptTask.SOLUTION_SUGGESTIONS,
    RegenTask.POSTMORTEM: PromptTask.POSTMORTEM,
}


def _uuid(incident_id: str) -> UUID:
    # NATS payloads carry incidentId as a string; incident-service uses UUIDs.
    # Generated client expects UUID; fail fast if the payload is malformed.
    return UUID(incident_id)


def _expect_no_content(response: Response[object], *, operation: str) -> None:
    if response.status_code == HTTPStatus.NO_CONTENT:
        return
    raise RuntimeError(
        f"{operation} failed for incident-service: HTTP {response.status_code}"
    )
