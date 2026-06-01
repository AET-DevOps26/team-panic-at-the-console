from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..models.user_role import UserRole

T = TypeVar("T", bound="User")


@_attrs_define
class User:
    """Public user profile (no credentials).

    Attributes:
        id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000aa.
        email (str):  Example: responder@example.com.
        display_name (str):  Example: Alex Responder.
        role (UserRole): Platform role aligned with the analysis object model.
            `MEMBER` maps to TeamMember, `RESPONDER` to IncidentResponder, `COMMANDER` to IncidentCommander.
             Example: MEMBER.
        created_at (datetime.datetime):  Example: 2026-05-08T10:00:00Z.
    """

    id: UUID
    email: str
    display_name: str
    role: UserRole
    created_at: datetime.datetime

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        email = self.email

        display_name = self.display_name

        role = self.role.value

        created_at = self.created_at.isoformat()

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "email": email,
                "displayName": display_name,
                "role": role,
                "createdAt": created_at,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        email = d.pop("email")

        display_name = d.pop("displayName")

        role = UserRole(d.pop("role"))

        created_at = isoparse(d.pop("createdAt"))

        user = cls(
            id=id,
            email=email,
            display_name=display_name,
            role=role,
            created_at=created_at,
        )

        return user
