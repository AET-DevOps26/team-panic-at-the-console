from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="ErrorResponse")


@_attrs_define
class ErrorResponse:
    """Simple error payload for auth and validation failures.

    Attributes:
        message (str):  Example: Invalid email or password.
    """

    message: str

    def to_dict(self) -> dict[str, Any]:
        message = self.message

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "message": message,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        message = d.pop("message")

        error_response = cls(
            message=message,
        )

        return error_response
