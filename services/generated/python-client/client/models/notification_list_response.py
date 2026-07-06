from __future__ import annotations

from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar

from attrs import define as _attrs_define

if TYPE_CHECKING:
    from ..models.notification import Notification


T = TypeVar("T", bound="NotificationListResponse")


@_attrs_define
class NotificationListResponse:
    """
    Attributes:
        items (list[Notification]):
        total (int):  Example: 3.
        page (int):
        size (int):  Example: 50.
        unread_count (int): Number of unread notifications in the same scope as this query. Example: 2.
    """

    items: list[Notification]
    total: int
    page: int
    size: int
    unread_count: int

    def to_dict(self) -> dict[str, Any]:
        items = []
        for items_item_data in self.items:
            items_item = items_item_data.to_dict()
            items.append(items_item)

        total = self.total

        page = self.page

        size = self.size

        unread_count = self.unread_count

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "items": items,
                "total": total,
                "page": page,
                "size": size,
                "unreadCount": unread_count,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.notification import Notification

        d = dict(src_dict)
        items = []
        _items = d.pop("items")
        for items_item_data in _items:
            items_item = Notification.from_dict(items_item_data)

            items.append(items_item)

        total = d.pop("total")

        page = d.pop("page")

        size = d.pop("size")

        unread_count = d.pop("unreadCount")

        notification_list_response = cls(
            items=items,
            total=total,
            page=page,
            size=size,
            unread_count=unread_count,
        )

        return notification_list_response
