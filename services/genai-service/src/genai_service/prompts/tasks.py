from enum import StrEnum


class PromptTask(StrEnum):
    """What the PromptBuilder is asked to produce. Each task has its own system prompt and response schema."""

    SUMMARY = "summary"
    SEVERITY_SUGGESTION = "severity_suggestion"
    SOLUTION_SUGGESTIONS = "solution_suggestions"
    POSTMORTEM = "postmortem"
