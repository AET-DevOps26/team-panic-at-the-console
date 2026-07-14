from enum import Enum


class NotificationType(str, Enum):
    COMMENT_ADDED = "COMMENT_ADDED"
    INCIDENT_ASSIGNED = "INCIDENT_ASSIGNED"
    INCIDENT_CREATED = "INCIDENT_CREATED"
    INCIDENT_RESOLVED = "INCIDENT_RESOLVED"
    SEVERITY_ESCALATED = "SEVERITY_ESCALATED"
    STATUS_CHANGED = "STATUS_CHANGED"

    def __str__(self) -> str:
        return str(self.value)
