from enum import Enum


class SeverityResponseSeverity(str, Enum):
    SEV1 = "SEV1"
    SEV2 = "SEV2"
    SEV3 = "SEV3"
    SEV4 = "SEV4"

    def __str__(self) -> str:
        return str(self.value)
