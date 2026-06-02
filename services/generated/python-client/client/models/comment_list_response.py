from __future__ import annotations

from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar

from attrs import define as _attrs_define

if TYPE_CHECKING:
    from ..models.comment import Comment


T = TypeVar("T", bound="CommentListResponse")


@_attrs_define
class CommentListResponse:
    """
    Attributes:
        items (list[Comment]):
        total (int):  Example: 3.
        limit (int):  Example: 50.
        offset (int):
    """

    items: list[Comment]
    total: int
    limit: int
    offset: int

    def to_dict(self) -> dict[str, Any]:
        items = []
        for items_item_data in self.items:
            items_item = items_item_data.to_dict()
            items.append(items_item)

        total = self.total

        limit = self.limit

        offset = self.offset

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "items": items,
                "total": total,
                "limit": limit,
                "offset": offset,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.comment import Comment

        d = dict(src_dict)
        items = []
        _items = d.pop("items")
        for items_item_data in _items:
            items_item = Comment.from_dict(items_item_data)

            items.append(items_item)

        total = d.pop("total")

        limit = d.pop("limit")

        offset = d.pop("offset")

        comment_list_response = cls(
            items=items,
            total=total,
            limit=limit,
            offset=offset,
        )

        return comment_list_response
