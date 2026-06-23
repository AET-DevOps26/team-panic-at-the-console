from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="CreateCommentRequest")


@_attrs_define
class CreateCommentRequest:
    """Request to add a comment to an incident.

    Attributes:
        text (str):  Example: Rolled back deployment v2.4.1; monitoring error rate now..
    """

    text: str

    def to_dict(self) -> dict[str, Any]:
        text = self.text

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "text": text,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        text = d.pop("text")

        create_comment_request = cls(
            text=text,
        )

        return create_comment_request
