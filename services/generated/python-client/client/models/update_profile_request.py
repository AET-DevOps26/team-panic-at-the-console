from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..types import UNSET, Unset

T = TypeVar("T", bound="UpdateProfileRequest")


@_attrs_define
class UpdateProfileRequest:
    """Partial profile update; at least one of `email` or `displayName` must be present. `currentPassword` is required when
    `email` is present.

        Attributes:
            email (str | Unset):  Example: new.address@example.com.
            display_name (str | Unset):  Example: Alex Responder.
            current_password (str | Unset):  Example: change-me-8+.
    """

    email: str | Unset = UNSET
    display_name: str | Unset = UNSET
    current_password: str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        email = self.email

        display_name = self.display_name

        current_password = self.current_password

        field_dict: dict[str, Any] = {}

        field_dict.update({})
        if email is not UNSET:
            field_dict["email"] = email
        if display_name is not UNSET:
            field_dict["displayName"] = display_name
        if current_password is not UNSET:
            field_dict["currentPassword"] = current_password

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        email = d.pop("email", UNSET)

        display_name = d.pop("displayName", UNSET)

        current_password = d.pop("currentPassword", UNSET)

        update_profile_request = cls(
            email=email,
            display_name=display_name,
            current_password=current_password,
        )

        return update_profile_request
