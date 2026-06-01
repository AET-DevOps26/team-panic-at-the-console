from enum import Enum


class UserRole(str, Enum):
    COMMANDER = "COMMANDER"
    MEMBER = "MEMBER"
    RESPONDER = "RESPONDER"

    def __str__(self) -> str:
        return str(self.value)
