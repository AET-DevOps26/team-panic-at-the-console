from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define

T = TypeVar("T", bound="AssignIncidentRequest")


@_attrs_define
class AssignIncidentRequest:
    """Request to assign or unassign responders.

    Attributes:
        user_ids (list[UUID]): UUIDs of users to assign. Send empty array to clear all assignments. Example:
            ['018e2c5f-1234-7abc-8def-0000000000aa'].
    """

    user_ids: list[UUID]

    def to_dict(self) -> dict[str, Any]:
        user_ids = []
        for user_ids_item_data in self.user_ids:
            user_ids_item = str(user_ids_item_data)
            user_ids.append(user_ids_item)

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "userIds": user_ids,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        user_ids = []
        _user_ids = d.pop("userIds")
        for user_ids_item_data in _user_ids:
            user_ids_item = UUID(user_ids_item_data)

            user_ids.append(user_ids_item)

        assign_incident_request = cls(
            user_ids=user_ids,
        )

        return assign_incident_request
