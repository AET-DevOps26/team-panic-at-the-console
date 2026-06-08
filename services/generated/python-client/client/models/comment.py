from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

T = TypeVar("T", bound="Comment")


@_attrs_define
class Comment:
    """An immutable comment left on an incident.

    Attributes:
        id (UUID):
        incident_id (UUID):
        author_id (UUID):
        content (str):  Example: Possible root cause is the connection pool configuration in v2.4.1.
        created_at (datetime.datetime):
    """

    id: UUID
    incident_id: UUID
    author_id: UUID
    content: str
    created_at: datetime.datetime

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        incident_id = str(self.incident_id)

        author_id = str(self.author_id)

        content = self.content

        created_at = self.created_at.isoformat()

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "incidentId": incident_id,
                "authorId": author_id,
                "content": content,
                "createdAt": created_at,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        incident_id = UUID(d.pop("incidentId"))

        author_id = UUID(d.pop("authorId"))

        content = d.pop("content")

        created_at = isoparse(d.pop("createdAt"))

        comment = cls(
            id=id,
            incident_id=incident_id,
            author_id=author_id,
            content=content,
            created_at=created_at,
        )

        return comment
