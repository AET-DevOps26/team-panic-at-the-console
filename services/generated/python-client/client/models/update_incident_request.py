from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.incident_status import IncidentStatus
from ..models.severity import Severity
from ..types import UNSET, Unset

T = TypeVar("T", bound="UpdateIncidentRequest")


@_attrs_define
class UpdateIncidentRequest:
    """Partial update for an incident's mutable fields.

    Attributes:
        status (IncidentStatus | Unset):
        severity (Severity | Unset): Incident severity. SEV1 is highest impact.
    """

    status: IncidentStatus | Unset = UNSET
    severity: Severity | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        status: str | Unset = UNSET
        if not isinstance(self.status, Unset):
            status = self.status.value

        severity: str | Unset = UNSET
        if not isinstance(self.severity, Unset):
            severity = self.severity.value

        field_dict: dict[str, Any] = {}

        field_dict.update({})
        if status is not UNSET:
            field_dict["status"] = status
        if severity is not UNSET:
            field_dict["severity"] = severity

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        _status = d.pop("status", UNSET)
        status: IncidentStatus | Unset
        if isinstance(_status, Unset):
            status = UNSET
        else:
            status = IncidentStatus(_status)

        _severity = d.pop("severity", UNSET)
        severity: Severity | Unset
        if isinstance(_severity, Unset):
            severity = UNSET
        else:
            severity = Severity(_severity)

        update_incident_request = cls(
            status=status,
            severity=severity,
        )

        return update_incident_request
