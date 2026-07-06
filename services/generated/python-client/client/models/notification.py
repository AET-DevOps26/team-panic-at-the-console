from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar, cast
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..models.notification_type import NotificationType
from ..types import UNSET, Unset

T = TypeVar("T", bound="Notification")


@_attrs_define
class Notification:
    """An in-app notification about an incident event.

    Attributes:
        id (UUID):
        incident_id (UUID):
        type_ (NotificationType): Category of a notification, derived from the incident event that produced it.
        message (str):  Example: You were assigned to an incident..
        read (bool):
        created_at (datetime.datetime):
        recipient_id (None | Unset | UUID): Target user for a personal notification; null for a broadcast visible to
            everyone.
    """

    id: UUID
    incident_id: UUID
    type_: NotificationType
    message: str
    read: bool
    created_at: datetime.datetime
    recipient_id: None | Unset | UUID = UNSET

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        incident_id = str(self.incident_id)

        type_ = self.type_.value

        message = self.message

        read = self.read

        created_at = self.created_at.isoformat()

        recipient_id: None | str | Unset
        if isinstance(self.recipient_id, Unset):
            recipient_id = UNSET
        elif isinstance(self.recipient_id, UUID):
            recipient_id = str(self.recipient_id)
        else:
            recipient_id = self.recipient_id

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "incidentId": incident_id,
                "type": type_,
                "message": message,
                "read": read,
                "createdAt": created_at,
            }
        )
        if recipient_id is not UNSET:
            field_dict["recipientId"] = recipient_id

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        incident_id = UUID(d.pop("incidentId"))

        type_ = NotificationType(d.pop("type"))

        message = d.pop("message")

        read = d.pop("read")

        created_at = isoparse(d.pop("createdAt"))

        def _parse_recipient_id(data: object) -> None | Unset | UUID:
            if data is None:
                return data
            if isinstance(data, Unset):
                return data
            try:
                if not isinstance(data, str):
                    raise TypeError()
                recipient_id_type_0 = UUID(data)

                return recipient_id_type_0
            except (TypeError, ValueError, AttributeError, KeyError):
                pass
            return cast(None | Unset | UUID, data)

        recipient_id = _parse_recipient_id(d.pop("recipientId", UNSET))

        notification = cls(
            id=id,
            incident_id=incident_id,
            type_=type_,
            message=message,
            read=read,
            created_at=created_at,
            recipient_id=recipient_id,
        )

        return notification
