from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar, cast
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..models.incident_status import IncidentStatus
from ..models.severity import Severity
from ..types import UNSET, Unset

T = TypeVar("T", bound="Incident")


@_attrs_define
class Incident:
    """An incident as stored by incident-service.

    Attributes:
        id (UUID):
        title (str):  Example: Checkout 5xx spike.
        status (IncidentStatus):
        severity (Severity): Incident severity. SEV1 is highest impact.
        created_at (datetime.datetime):
        description (None | str | Unset):
        resolved_at (datetime.datetime | None | Unset):
        summary (None | str | Unset): AI-generated narrative summary. Regenerable on demand.
        severity_suggestion (None | str | Unset): AI-suggested severity with reasoning, formatted as "SEV<n>: <reason>".
        solutions (None | str | Unset): AI-suggested remediation steps, one per line.
        postmortem (None | str | Unset): AI-drafted postmortem. Only set for resolved incidents.
    """

    id: UUID
    title: str
    status: IncidentStatus
    severity: Severity
    created_at: datetime.datetime
    description: None | str | Unset = UNSET
    resolved_at: datetime.datetime | None | Unset = UNSET
    summary: None | str | Unset = UNSET
    severity_suggestion: None | str | Unset = UNSET
    solutions: None | str | Unset = UNSET
    postmortem: None | str | Unset = UNSET

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

        summary: None | str | Unset
        if isinstance(self.summary, Unset):
            summary = UNSET
        else:
            summary = self.summary

        severity_suggestion: None | str | Unset
        if isinstance(self.severity_suggestion, Unset):
            severity_suggestion = UNSET
        else:
            severity_suggestion = self.severity_suggestion

        solutions: None | str | Unset
        if isinstance(self.solutions, Unset):
            solutions = UNSET
        else:
            solutions = self.solutions

        postmortem: None | str | Unset
        if isinstance(self.postmortem, Unset):
            postmortem = UNSET
        else:
            postmortem = self.postmortem

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
        if summary is not UNSET:
            field_dict["summary"] = summary
        if severity_suggestion is not UNSET:
            field_dict["severitySuggestion"] = severity_suggestion
        if solutions is not UNSET:
            field_dict["solutions"] = solutions
        if postmortem is not UNSET:
            field_dict["postmortem"] = postmortem

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        title = d.pop("title")

        status = IncidentStatus(d.pop("status"))

        severity = Severity(d.pop("severity"))

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

        def _parse_summary(data: object) -> None | str | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            return cast(None | str | Unset, data)

        summary = _parse_summary(d.pop("summary", UNSET))

        def _parse_severity_suggestion(data: object) -> None | str | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            return cast(None | str | Unset, data)

        severity_suggestion = _parse_severity_suggestion(d.pop("severitySuggestion", UNSET))

        def _parse_solutions(data: object) -> None | str | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            return cast(None | str | Unset, data)

        solutions = _parse_solutions(d.pop("solutions", UNSET))

        def _parse_postmortem(data: object) -> None | str | Unset:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            return cast(None | str | Unset, data)

        postmortem = _parse_postmortem(d.pop("postmortem", UNSET))

        incident = cls(
            id=id,
            title=title,
            status=status,
            severity=severity,
            created_at=created_at,
            description=description,
            resolved_at=resolved_at,
            summary=summary,
            severity_suggestion=severity_suggestion,
            solutions=solutions,
            postmortem=postmortem,
        )

        return incident
