from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar, cast

from attrs import define as _attrs_define

T = TypeVar("T", bound="SolutionsPatch")


@_attrs_define
class SolutionsPatch:
    """
    Attributes:
        solutions (list[str]):
    """

    solutions: list[str]

    def to_dict(self) -> dict[str, Any]:
        solutions = self.solutions

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "solutions": solutions,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        solutions = cast(list[str], d.pop("solutions"))

        solutions_patch = cls(
            solutions=solutions,
        )

        return solutions_patch
