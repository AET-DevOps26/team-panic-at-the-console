from enum import Enum


class IncidentStatus(str, Enum):
    INVESTIGATING = "investigating"
    OPEN = "open"
    RESOLVED = "resolved"

    def __str__(self) -> str:
        return str(self.value)
