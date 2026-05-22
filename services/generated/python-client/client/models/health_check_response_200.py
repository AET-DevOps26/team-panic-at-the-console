from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="HealthCheckResponse200")


@_attrs_define
class HealthCheckResponse200:
    """
    Attributes:
        status (str):  Example: ok.
    """

    status: str

    def to_dict(self) -> dict[str, Any]:
        status = self.status

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "status": status,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        status = d.pop("status")

        health_check_response_200 = cls(
            status=status,
        )

        return health_check_response_200
