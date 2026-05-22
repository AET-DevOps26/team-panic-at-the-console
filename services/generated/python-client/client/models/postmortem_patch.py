from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar, cast

from attrs import define as _attrs_define

T = TypeVar("T", bound="PostmortemPatch")


@_attrs_define
class PostmortemPatch:
    """
    Attributes:
        root_cause (str):
        timeline (list[str]):
        action_items (list[str]):
    """

    root_cause: str
    timeline: list[str]
    action_items: list[str]

    def to_dict(self) -> dict[str, Any]:
        root_cause = self.root_cause

        timeline = self.timeline

        action_items = self.action_items

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "rootCause": root_cause,
                "timeline": timeline,
                "actionItems": action_items,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        root_cause = d.pop("rootCause")

        timeline = cast(list[str], d.pop("timeline"))

        action_items = cast(list[str], d.pop("actionItems"))

        postmortem_patch = cls(
            root_cause=root_cause,
            timeline=timeline,
            action_items=action_items,
        )

        return postmortem_patch
