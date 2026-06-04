from enum import StrEnum


class RegenTask(StrEnum):
    """Matches RegenAccepted.task in api/openapi.yaml and incident.regen.requested NATS payloads."""

    SUMMARY = "SUMMARY"
    SEVERITY_SUGGESTION = "SEVERITY_SUGGESTION"
    SOLUTION_SUGGESTIONS = "SOLUTION_SUGGESTIONS"
    POSTMORTEM = "POSTMORTEM"
