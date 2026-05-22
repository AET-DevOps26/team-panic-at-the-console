from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.regen_accepted_task import RegenAcceptedTask

T = TypeVar("T", bound="RegenAccepted")


@_attrs_define
class RegenAccepted:
    """Confirmation that an AI generation task was accepted for async processing.

    Attributes:
        accepted (bool):  Example: True.
        task (RegenAcceptedTask):  Example: SUMMARY.
    """

    accepted: bool
    task: RegenAcceptedTask

    def to_dict(self) -> dict[str, Any]:
        accepted = self.accepted

        task = self.task.value

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "accepted": accepted,
                "task": task,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        accepted = d.pop("accepted")

        task = RegenAcceptedTask(d.pop("task"))

        regen_accepted = cls(
            accepted=accepted,
            task=task,
        )

        return regen_accepted
