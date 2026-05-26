import datetime
from collections.abc import Sequence
from dataclasses import dataclass

from pydantic import BaseModel

from client.models import Incident, IncidentEvent, IncidentStatus
from genai_service.prompts.models import (
    PostmortemResponse,
    SeverityResponse,
    SolutionsResponse,
    SummaryResponse,
)
from genai_service.prompts.tasks import PromptTask


@dataclass(frozen=True)
class Prompt:
    """Fully-formed input for OllamaClient.generate: system + user + response schema."""

    system: str
    user: str
    response_model: type[BaseModel]


_SYSTEM_PROMPTS: dict[PromptTask, str] = {
    PromptTask.SUMMARY: (
        "You are an SRE assistant. Summarize the incident's current state in one or two sentences. "
        "Be specific and factual. Use only the incident metadata and event log provided."
    ),
    PromptTask.SEVERITY_SUGGESTION: (
        "You are an SRE assistant. Suggest a severity (SEV1=highest, SEV4=lowest) for this incident "
        "based on impact and scope visible in the metadata and event log. Justify the choice in one sentence."
    ),
    PromptTask.SOLUTION_SUGGESTIONS: (
        "You are an SRE assistant. Propose concrete next steps an on-call engineer can try to mitigate "
        "or diagnose the incident. Order by likelihood of impact. Keep each step actionable."
    ),
    PromptTask.POSTMORTEM: (
        "You are an SRE assistant. Produce a post-incident review from the metadata and event log: "
        "the root cause, a chronological timeline of what happened, and follow-up action items "
        "to prevent recurrence."
    ),
}


_RESPONSE_MODELS: dict[PromptTask, type[BaseModel]] = {
    PromptTask.SUMMARY: SummaryResponse,
    PromptTask.SEVERITY_SUGGESTION: SeverityResponse,
    PromptTask.SOLUTION_SUGGESTIONS: SolutionsResponse,
    PromptTask.POSTMORTEM: PostmortemResponse,
}


class PromptBuilder:
    """Constructs an Ollama prompt from an Incident, its Event Log, and a PromptTask.

    Consumes the generated client's attrs Incident/IncidentEvent models directly so
    there is one shape of incident data flowing through genai-service. All
    context-length management and structured-output schema selection lives here
    (the "deep module" per CONTEXT.md).
    """

    def __init__(self, max_events: int = 50) -> None:
        if max_events < 1:
            raise ValueError("max_events must be >= 1")
        self._max_events = max_events

    def build(
        self,
        incident: Incident,
        events: Sequence[IncidentEvent],
        task: PromptTask,
    ) -> Prompt:
        if (
            task is PromptTask.POSTMORTEM
            and incident.status is not IncidentStatus.RESOLVED
        ):
            raise ValueError(
                f"Postmortem requires a resolved incident; status is {incident.status.value!r}."
            )

        return Prompt(
            system=_SYSTEM_PROMPTS[task],
            user=self._format_context(incident, events),
            response_model=_RESPONSE_MODELS[task],
        )

    def _format_context(
        self, incident: Incident, events: Sequence[IncidentEvent]
    ) -> str:
        ordered = sorted(events, key=lambda e: e.timestamp)
        kept = self._truncate(ordered)

        header = [
            f"Incident {incident.id}",
            f"Title: {incident.title}",
            f"Status: {incident.status.value}",
            f"Severity: {incident.severity.value}",
            f"Created at: {incident.created_at.isoformat()}",
        ]
        resolved_at = incident.resolved_at
        if isinstance(resolved_at, datetime.datetime):
            header.append(f"Resolved at: {resolved_at.isoformat()}")
        description = (
            incident.description if isinstance(incident.description, str) else None
        )
        header.append(f"Description: {description or '(none)'}")

        if not kept:
            event_section = "Event log: (no events recorded)"
        else:
            truncated_note = (
                f"(showing last {len(kept)} of {len(events)} events)\n"
                if len(events) > len(kept)
                else ""
            )
            event_lines = "\n".join(
                f"- [{e.timestamp.isoformat()}] {e.type_}: {e.description}"
                for e in kept
            )
            event_section = f"Event log:\n{truncated_note}{event_lines}"

        return "\n".join(header) + "\n\n" + event_section

    def _truncate(self, events: Sequence[IncidentEvent]) -> Sequence[IncidentEvent]:
        if len(events) <= self._max_events:
            return events
        return events[-self._max_events :]
