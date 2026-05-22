from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="GenaiHealthResponse")


@_attrs_define
class GenaiHealthResponse:
    """GenAI service health status including Ollama reachability.

    Attributes:
        status (str):  Example: ok.
        ollama_reachable (bool):  Example: True.
        model (str):  Example: qwen2.5:3b.
    """

    status: str
    ollama_reachable: bool
    model: str

    def to_dict(self) -> dict[str, Any]:
        status = self.status

        ollama_reachable = self.ollama_reachable

        model = self.model

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "status": status,
                "ollamaReachable": ollama_reachable,
                "model": model,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        status = d.pop("status")

        ollama_reachable = d.pop("ollamaReachable")

        model = d.pop("model")

        genai_health_response = cls(
            status=status,
            ollama_reachable=ollama_reachable,
            model=model,
        )

        return genai_health_response
