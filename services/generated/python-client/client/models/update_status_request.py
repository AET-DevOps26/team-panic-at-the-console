from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.incident_status import IncidentStatus

T = TypeVar("T", bound="UpdateStatusRequest")


@_attrs_define
class UpdateStatusRequest:
    """Request to update incident status.

    Attributes:
        status (IncidentStatus):
    """

    status: IncidentStatus

    def to_dict(self) -> dict[str, Any]:
        status = self.status.value

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "status": status,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        status = IncidentStatus(d.pop("status"))

        update_status_request = cls(
            status=status,
        )

        return update_status_request
