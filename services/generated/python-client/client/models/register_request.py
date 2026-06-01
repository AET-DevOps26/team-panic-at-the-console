from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="RegisterRequest")


@_attrs_define
class RegisterRequest:
    """
    Attributes:
        email (str):  Example: new.user@example.com.
        password (str):  Example: change-me-8+.
        display_name (str):  Example: New User.
    """

    email: str
    password: str
    display_name: str

    def to_dict(self) -> dict[str, Any]:
        email = self.email

        password = self.password

        display_name = self.display_name

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "email": email,
                "password": password,
                "displayName": display_name,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        email = d.pop("email")

        password = d.pop("password")

        display_name = d.pop("displayName")

        register_request = cls(
            email=email,
            password=password,
            display_name=display_name,
        )

        return register_request
