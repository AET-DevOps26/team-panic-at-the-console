from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar, cast

from attrs import define as _attrs_define

T = TypeVar("T", bound="SolutionsResponse")


@_attrs_define
class SolutionsResponse:
    """Latest AI-generated remediation suggestions for an incident.

    Example:
        {'solutions': ['Roll back payment-service to v2.3.9', 'Scale checkout-api replicas to 6']}

    Attributes:
        solutions (list[str]):  Example: ['Roll back payment-service to v2.3.9', 'Scale checkout-api replicas to 6'].
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

        solutions_response = cls(
            solutions=solutions,
        )

        return solutions_response
