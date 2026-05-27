from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="CreateCommentRequest")


@_attrs_define
class CreateCommentRequest:
    """Payload for adding a comment to an incident.

    Attributes:
        content (str):  Example: Possible root cause is the connection pool configuration in v2.4.1.
    """

    content: str

    def to_dict(self) -> dict[str, Any]:
        content = self.content

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "content": content,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        content = d.pop("content")

        create_comment_request = cls(
            content=content,
        )

        return create_comment_request
