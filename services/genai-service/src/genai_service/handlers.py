import asyncio
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from http import HTTPStatus
from typing import Any
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
from genai_service.llm import LLMClient, provider_name
from genai_service.metrics import time_generation
from genai_service.prompts import PromptBuilder, PromptTask
from genai_service.regen_task import RegenTask

logger = structlog.get_logger(__name__)

_POSTMORTEM_RESOLVE_RETRIES = 3
_POSTMORTEM_RESOLVE_DELAY_SECONDS = 0.5


@dataclass(frozen=True)
class _PatchSpec:
    """How to turn an Ollama response for one PromptTask into an incident-service PATCH.

    `to_body` adapts the validated response model into the matching Patch body;
    `write` is the generated client call that PATCHes it back. The response model
    itself is not named here: it lives on the Prompt produced by PromptBuilder, so
    there is a single source of truth for which schema each task uses.
    """

    to_body: Callable[[Any], Any]
    write: Callable[..., Awaitable[Response[Any]]]


_PATCH_SPECS: dict[PromptTask, _PatchSpec] = {
    PromptTask.SUMMARY: _PatchSpec(
        to_body=lambda r: SummaryPatch(summary=r.summary),
        write=write_incident_summary.asyncio_detailed,
    ),
    PromptTask.SEVERITY_SUGGESTION: _PatchSpec(
        to_body=lambda r: SeverityPatch(severity=r.severity, reason=r.reason),
        write=write_incident_severity_suggestion.asyncio_detailed,
    ),
    PromptTask.SOLUTION_SUGGESTIONS: _PatchSpec(
        to_body=lambda r: SolutionsPatch(solutions=list(r.solutions)),
        write=write_incident_solutions.asyncio_detailed,
    ),
    PromptTask.POSTMORTEM: _PatchSpec(
        to_body=lambda r: PostmortemPatch(
            root_cause=r.root_cause,
            timeline=r.timeline,
            action_items=r.action_items,
        ),
        write=write_incident_postmortem.asyncio_detailed,
    ),
}


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
                await self._generate_and_patch(
                    incident_id, incident, events, PromptTask.POSTMORTEM
                )
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
        spec = _PATCH_SPECS[task]
        prompt = self._prompts.build(incident, events, task)
        captured_provider = provider_name(self._llm)
        with time_generation(task.value, provider=lambda: captured_provider):
            try:
                result = await self._llm.generate(
                    prompt.user,
                    system=prompt.system,
                    response_model=prompt.response_model,
                )
            finally:
                captured_provider = provider_name(self._llm)
            response = await spec.write(
                incident_id=_uuid(incident_id),
                client=self._client,
                body=spec.to_body(result),
            )
            _expect_no_content(response, operation=f"write {task.value}")

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
