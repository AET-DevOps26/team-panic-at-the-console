from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.severity import Severity

T = TypeVar("T", bound="CreateIncidentRequest")


@_attrs_define
class CreateIncidentRequest:
    """Request to create a new incident manually.

    Attributes:
        title (str):  Example: Database migration rollback needed.
        severity (Severity): Incident severity. SEV1 is highest impact.
    """

    title: str
    severity: Severity

    def to_dict(self) -> dict[str, Any]:
        title = self.title

        severity = self.severity.value

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "title": title,
                "severity": severity,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        title = d.pop("title")

        severity = Severity(d.pop("severity"))

        create_incident_request = cls(
            title=title,
            severity=severity,
        )

        return create_incident_request
