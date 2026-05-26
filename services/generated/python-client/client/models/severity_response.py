from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.severity import Severity

T = TypeVar("T", bound="SeverityResponse")


@_attrs_define
class SeverityResponse:
    """Latest AI-generated severity recommendation with rationale (GET in a later release; structured LLM output contract
    today).

        Example:
            {'severity': 'SEV2', 'reason': 'Customer-facing checkout degraded for more than 15 minutes with no workaround.'}

        Attributes:
            severity (Severity): Incident severity. SEV1 is highest impact.
            reason (str):  Example: Customer-facing checkout degraded for more than 15 minutes with no workaround..
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

        severity_response = cls(
            severity=severity,
            reason=reason,
        )

        return severity_response
