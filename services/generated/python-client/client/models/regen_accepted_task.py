from enum import Enum


class RegenAcceptedTask(str, Enum):
    POSTMORTEM = "POSTMORTEM"
    SEVERITY_SUGGESTION = "SEVERITY_SUGGESTION"
    SOLUTION_SUGGESTIONS = "SOLUTION_SUGGESTIONS"
    SUMMARY = "SUMMARY"

    def __str__(self) -> str:
        return str(self.value)
