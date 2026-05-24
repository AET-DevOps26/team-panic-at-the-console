from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar, cast
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..models.incident_severity import IncidentSeverity
from ..models.incident_status import IncidentStatus
from ..types import UNSET, Unset

T = TypeVar("T", bound="Incident")


@_attrs_define
class Incident:
    """Subset of incident representation used by genai prompts.

    Attributes:
        id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        title (str):  Example: Checkout API latency spike.
        status (IncidentStatus):  Example: investigating.
        severity (IncidentSeverity):  Example: SEV2.
        created_at (datetime.datetime):  Example: 2026-05-24T14:02:00Z.
        description (None | str | Unset):  Example: Elevated p99 latency on checkout-api after deploy v2.4.1.
        resolved_at (datetime.datetime | None | Unset):
    """

    id: UUID
    title: str
    status: IncidentStatus
    severity: IncidentSeverity
    created_at: datetime.datetime
    description: None | str | Unset = UNSET
    resolved_at: datetime.datetime | None | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        title = self.title

        status = self.status.value

        severity = self.severity.value

        created_at = self.created_at.isoformat()

        description: None | str | Unset
        if isinstance(self.description, Unset):
            description = UNSET
        else:
            description = self.description

        resolved_at: None | str | Unset
        if isinstance(self.resolved_at, Unset):
            resolved_at = UNSET
        elif isinstance(self.resolved_at, datetime.datetime):
            resolved_at = self.resolved_at.isoformat()
        else:
            resolved_at = self.resolved_at

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "title": title,
                "status": status,
                "severity": severity,
                "createdAt": created_at,
            }
        )
        if description is not UNSET:
            field_dict["description"] = description
        if resolved_at is not UNSET:
            field_dict["resolvedAt"] = resolved_at

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        title = d.pop("title")

        status = IncidentStatus(d.pop("status"))

        severity = IncidentSeverity(d.pop("severity"))

        created_at = isoparse(d.pop("createdAt"))

        def _parse_description(data: object) -> None | str | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            return cast(None | str | Unset, data)

        description = _parse_description(d.pop("description", UNSET))

        def _parse_resolved_at(data: object) -> datetime.datetime | None | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            try:
                if not isinstance(data, str):
                    raise TypeError()
                resolved_at_type_0 = isoparse(data)

                return resolved_at_type_0
            except (TypeError, ValueError, AttributeError, KeyError):
                pass
            return cast(datetime.datetime | None | Unset, data)

        resolved_at = _parse_resolved_at(d.pop("resolvedAt", UNSET))

        incident = cls(
            id=id,
            title=title,
            status=status,
            severity=severity,
            created_at=created_at,
            description=description,
            resolved_at=resolved_at,
        )

        return incident
