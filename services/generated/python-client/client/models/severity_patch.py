from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.severity import Severity

T = TypeVar("T", bound="SeverityPatch")


@_attrs_define
class SeverityPatch:
    """
    Attributes:
        severity (Severity): Incident severity. SEV1 is highest impact.
        reason (str):
    """

    severity: Severity
    reason: str

    def to_dict(self) -> dict[str, Any]:
        severity = self.severity.value

        reason = self.reason

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "severity": severity,
                "reason": reason,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        severity = Severity(d.pop("severity"))

        reason = d.pop("reason")

        severity_patch = cls(
            severity=severity,
            reason=reason,
        )

        return severity_patch
