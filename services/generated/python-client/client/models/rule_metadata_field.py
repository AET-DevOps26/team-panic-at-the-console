from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="RuleMetadataField")


@_attrs_define
class RuleMetadataField:
    """A labelled value pulled from the event into the incident description.

    Attributes:
        label (str):  Example: Repository.
        field (str): Dotted path into the event. Example: payload.repository.full_name.
    """

    label: str
    field: str

    def to_dict(self) -> dict[str, Any]:
        label = self.label

        field = self.field

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "label": label,
                "field": field,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        label = d.pop("label")

        field = d.pop("field")

        rule_metadata_field = cls(
            label=label,
            field=field,
        )

        return rule_metadata_field
