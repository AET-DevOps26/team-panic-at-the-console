from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="ChangePasswordRequest")


@_attrs_define
class ChangePasswordRequest:
    """
    Attributes:
        current_password (str):  Example: change-me-8+.
        new_password (str):  Example: new-secret-9!.
    """

    current_password: str
    new_password: str

    def to_dict(self) -> dict[str, Any]:
        current_password = self.current_password

        new_password = self.new_password

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "currentPassword": current_password,
                "newPassword": new_password,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        current_password = d.pop("currentPassword")

        new_password = d.pop("newPassword")

        change_password_request = cls(
            current_password=current_password,
            new_password=new_password,
        )

        return change_password_request
