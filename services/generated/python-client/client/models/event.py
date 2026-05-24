from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define
from dateutil.parser import isoparse

T = TypeVar("T", bound="Event")


@_attrs_define
class Event:
    """One entry from an incident's Event Log (chronological, immutable).

    Attributes:
        timestamp (datetime.datetime):  Example: 2026-05-24T14:18:00Z.
        type_ (str):  Example: severity_changed.
        description (str):  Example: Severity escalated from SEV3 to SEV2.
    """

    timestamp: datetime.datetime
    type_: str
    description: str

    def to_dict(self) -> dict[str, Any]:
        timestamp = self.timestamp.isoformat()

        type_ = self.type_

        description = self.description

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "timestamp": timestamp,
                "type": type_,
                "description": description,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        timestamp = isoparse(d.pop("timestamp"))

        type_ = d.pop("type")

        description = d.pop("description")

        event = cls(
            timestamp=timestamp,
            type_=type_,
            description=description,
        )

        return event
