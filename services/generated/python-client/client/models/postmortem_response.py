from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar, cast

from attrs import define as _attrs_define

T = TypeVar("T", bound="PostmortemResponse")


@_attrs_define
class PostmortemResponse:
    """Latest AI-generated postmortem draft for a resolved incident (GET in a later release; structured LLM output contract
    today).

        Example:
            {'rootCause': 'Connection pool misconfiguration in payment-service v2.4.1', 'timeline': ['14:02 Deploy v2.4.1
                completed', '14:18 Checkout error rate crossed 5%'], 'actionItems': ['Add pool-size validation to deploy
                checklist', 'Alert on checkout latency SLO burn']}

        Attributes:
            root_cause (str):  Example: Connection pool misconfiguration in payment-service v2.4.1.
            timeline (list[str]):  Example: ['14:02 Deploy v2.4.1 completed', '14:18 Checkout error rate crossed 5%'].
            action_items (list[str]):  Example: ['Add pool-size validation to deploy checklist', 'Alert on checkout latency
                SLO burn'].
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

        postmortem_response = cls(
            root_cause=root_cause,
            timeline=timeline,
            action_items=action_items,
        )

        return postmortem_response
