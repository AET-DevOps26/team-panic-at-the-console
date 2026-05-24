from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="SummaryResponse")


@_attrs_define
class SummaryResponse:
    """Latest AI-generated narrative summary for an incident.

    Example:
        {'summary': 'Checkout API latency spiked after deploy v2.4.1; error rate on payment-service rose to 12%.'}

    Attributes:
        summary (str):  Example: Checkout API latency spiked after deploy v2.4.1; error rate on payment-service rose to
            12%..
    """

    summary: str

    def to_dict(self) -> dict[str, Any]:
        summary = self.summary

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "summary": summary,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        summary = d.pop("summary")

        summary_response = cls(
            summary=summary,
        )

        return summary_response
