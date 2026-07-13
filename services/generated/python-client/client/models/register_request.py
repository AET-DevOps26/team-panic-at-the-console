from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..types import UNSET, Unset

T = TypeVar("T", bound="RegisterRequest")


@_attrs_define
class RegisterRequest:
    """
    Attributes:
        email (str):  Example: new.user@example.com.
        password (str):  Example: change-me-8+.
        display_name (str):  Example: New User.
        invite_code (str | Unset): Instance invitation code. Required when the deployment is configured with one (public
            instances); ignored when the instance leaves registration open. Example: let-me-in.
    """

    email: str
    password: str
    display_name: str
    invite_code: str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        email = self.email

        password = self.password

        display_name = self.display_name

        invite_code = self.invite_code

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "email": email,
                "password": password,
                "displayName": display_name,
            }
        )
        if invite_code is not UNSET:
            field_dict["inviteCode"] = invite_code

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        email = d.pop("email")

        password = d.pop("password")

        display_name = d.pop("displayName")

        invite_code = d.pop("inviteCode", UNSET)

        register_request = cls(
            email=email,
            password=password,
            display_name=display_name,
            invite_code=invite_code,
        )

        return register_request
