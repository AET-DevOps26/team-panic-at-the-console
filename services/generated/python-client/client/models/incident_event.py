from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..types import UNSET, Unset

T = TypeVar("T", bound="IncidentEvent")


@_attrs_define
class IncidentEvent:
    """One entry from an incident's append-only Event Log.

    Attributes:
        timestamp (datetime.datetime):
        type_ (str):  Example: status_changed.
        description (str):  Example: status: open -> investigating.
        new_value (str | Unset): New value after the change: the new status for status_changed entries and the new
            severity for severity_changed entries. Lets clients color-code timeline entries without parsing the description.
            Absent for other entry types and for events stored before this field existed. Example: investigating.
    """

    timestamp: datetime.datetime
    type_: str
    description: str
    new_value: str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        timestamp = self.timestamp.isoformat()

        type_ = self.type_

        description = self.description

        new_value = self.new_value

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "timestamp": timestamp,
                "type": type_,
                "description": description,
            }
        )
        if new_value is not UNSET:
            field_dict["newValue"] = new_value

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        timestamp = isoparse(d.pop("timestamp"))

        type_ = d.pop("type")

        description = d.pop("description")

        new_value = d.pop("newValue", UNSET)

        incident_event = cls(
            timestamp=timestamp,
            type_=type_,
            description=description,
            new_value=new_value,
        )

        return incident_event
