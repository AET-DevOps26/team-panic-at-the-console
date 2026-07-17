from enum import Enum


class RuleOperator(str, Enum):
    CONTAINS = "contains"
    EQUALS = "equals"
    EXISTS = "exists"
    IN = "in"
    MATCHES = "matches"
    NOT_CONTAINS = "not_contains"
    NOT_EQUALS = "not_equals"
    NOT_EXISTS = "not_exists"

    def __str__(self) -> str:
        return str(self.value)
