from __future__ import annotations

from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar

from attrs import define as _attrs_define

if TYPE_CHECKING:
    from ..models.incident import Incident


T = TypeVar("T", bound="IncidentListResponse")


@_attrs_define
class IncidentListResponse:
    """
    Attributes:
        items (list[Incident]):
        total (int):  Example: 5.
        page (int):
        size (int):  Example: 50.
    """

    items: list[Incident]
    total: int
    page: int
    size: int

    def to_dict(self) -> dict[str, Any]:
        items = []
        for items_item_data in self.items:
            items_item = items_item_data.to_dict()
            items.append(items_item)

        total = self.total

        page = self.page

        size = self.size

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "items": items,
                "total": total,
                "page": page,
                "size": size,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.incident import Incident

        d = dict(src_dict)
        items = []
        _items = d.pop("items")
        for items_item_data in _items:
            items_item = Incident.from_dict(items_item_data)

            items.append(items_item)

        total = d.pop("total")

        page = d.pop("page")

        size = d.pop("size")

        incident_list_response = cls(
            items=items,
            total=total,
            page=page,
            size=size,
        )

        return incident_list_response
