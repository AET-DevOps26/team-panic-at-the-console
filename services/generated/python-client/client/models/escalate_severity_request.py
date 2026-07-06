from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.severity import Severity

T = TypeVar("T", bound="EscalateSeverityRequest")


@_attrs_define
class EscalateSeverityRequest:
    """Request to set incident severity (any level, higher or lower).

    Attributes:
        severity (Severity): Incident severity. SEV1 is highest impact.
    """

    severity: Severity

    def to_dict(self) -> dict[str, Any]:
        severity = self.severity.value

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "severity": severity,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        severity = Severity(d.pop("severity"))

        escalate_severity_request = cls(
            severity=severity,
        )

        return escalate_severity_request
