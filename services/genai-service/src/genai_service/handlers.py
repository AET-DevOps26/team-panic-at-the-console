import asyncio
import json
from collections.abc import Callable
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any
from uuid import UUID

import structlog
from nats.aio.client import Client as NatsClient

from client import Client
from client.api.incidents import get_incident, list_incident_events
from client.models import Incident, IncidentEvent, IncidentStatus
from genai_service.llm import LLMClient, provider_name
from genai_service.metrics import time_generation
from genai_service.prompts import PromptBuilder, PromptTask
from genai_service.regen_task import RegenTask

logger = structlog.get_logger(__name__)

_POSTMORTEM_RESOLVE_RETRIES = 3
_POSTMORTEM_RESOLVE_DELAY_SECONDS = 0.5


@dataclass(frozen=True)
class _PublishSpec:
    """Maps a PromptTask to a NATS subject and a payload builder."""

    subject: str
    to_payload: Callable[[Any, str], dict[str, Any]]


_PUBLISH_SPECS: dict[PromptTask, _PublishSpec] = {
    PromptTask.SUMMARY: _PublishSpec(
        subject="incident.genai.summary.generated",
        to_payload=lambda r, incident_id: {
            "incidentId": incident_id,
            "timestamp": _now(),
            "summary": r.summary,
        },
    ),
    PromptTask.SEVERITY_SUGGESTION: _PublishSpec(
        subject="incident.genai.severity.generated",
        to_payload=lambda r, incident_id: {
            "incidentId": incident_id,
            "timestamp": _now(),
            "severity": str(r.severity.value)
            if hasattr(r.severity, "value")
            else str(r.severity),
            "reason": r.reason,
        },
    ),
    PromptTask.SOLUTION_SUGGESTIONS: _PublishSpec(
        subject="incident.genai.solutions.generated",
        to_payload=lambda r, incident_id: {
            "incidentId": incident_id,
            "timestamp": _now(),
            "solutions": list(r.solutions),
        },
    ),
    PromptTask.POSTMORTEM: _PublishSpec(
        subject="incident.genai.postmortem.generated",
        to_payload=lambda r, incident_id: {
            "incidentId": incident_id,
            "timestamp": _now(),
            "rootCause": r.root_cause,
            "timeline": list(r.timeline),
            "actionItems": list(r.action_items),
        },
    ),
}


class IncidentHandlers:
    """Orchestrates NATS events -> fetch incident -> build prompt -> generate -> publish result.

    Results are published back to NATS; incident-service subscribes and persists them.
    """

    def __init__(
        self,
        incident_api_client: Client,
        llm_client: LLMClient,
        prompt_builder: PromptBuilder,
        nats_client: NatsClient | None = None,
    ) -> None:
        self._client = incident_api_client
        self._llm = llm_client
        self._prompts = prompt_builder
        self._nats = nats_client

    def set_nats_client(self, nats_client: NatsClient) -> None:
        self._nats = nats_client

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
                await self._generate_and_publish(
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
                await self._generate_and_publish(incident_id, incident, events, task)
            except Exception as exc:
                log.error("task_failed", task=task.value, error=str(exc))

        log.info("tasks_completed", tasks=[t.value for t in tasks])

    async def _generate_and_publish(
        self,
        incident_id: str,
        incident: Incident,
        events: list[IncidentEvent],
        task: PromptTask,
    ) -> None:
        spec = _PUBLISH_SPECS[task]
        prompt = self._prompts.build(incident, events, task)
        with time_generation(task.value, lambda: provider_name(self._llm)):
            result = await self._llm.generate(
                prompt.user, system=prompt.system, response_model=prompt.response_model
            )
            payload = spec.to_payload(result, incident_id)
            await self._publish(spec.subject, payload)

    async def _publish(self, subject: str, payload: dict[str, Any]) -> None:
        if self._nats is None:
            raise RuntimeError("NATS client not injected; cannot publish genai result")
        await self._nats.publish(subject, json.dumps(payload).encode())

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
    return UUID(incident_id)


def _now() -> str:
    return datetime.now(UTC).isoformat()
