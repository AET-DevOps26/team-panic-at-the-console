from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="UpdateDescriptionRequest")


@_attrs_define
class UpdateDescriptionRequest:
    """Request to set or clear the incident description. An empty string clears it.

    Attributes:
        description (str):  Example: Checkout error rate crossed 5% after deploy v2.4.1..
    """

    description: str

    def to_dict(self) -> dict[str, Any]:
        description = self.description

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "description": description,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        description = d.pop("description")

        update_description_request = cls(
            description=description,
        )

        return update_description_request
