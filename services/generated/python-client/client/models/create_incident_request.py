from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar, cast

from attrs import define as _attrs_define

from ..models.severity import Severity
from ..types import UNSET, Unset

T = TypeVar("T", bound="CreateIncidentRequest")


@_attrs_define
class CreateIncidentRequest:
    """Payload for manually creating a new incident.

    Attributes:
        title (str):  Example: Checkout 5xx spike.
        severity (Severity): Incident severity. SEV1 is highest impact.
        description (None | str | Unset):  Example: High error rate on checkout API after deploy v2.4.1.
    """

    title: str
    severity: Severity
    description: None | str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        title = self.title

        severity = self.severity.value

        description: None | str | Unset
        if isinstance(self.description, Unset):
            description = UNSET
        else:
            description = self.description

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "title": title,
                "severity": severity,
            }
        )
        if description is not UNSET:
            field_dict["description"] = description

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        title = d.pop("title")

        severity = Severity(d.pop("severity"))

        def _parse_description(data: object) -> None | str | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            return cast(None | str | Unset, data)

        description = _parse_description(d.pop("description", UNSET))

        create_incident_request = cls(
            title=title,
            severity=severity,
            description=description,
        )

        return create_incident_request
