from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

T = TypeVar("T", bound="CreateWebhookSourceRequest")


@_attrs_define
class CreateWebhookSourceRequest:
    """
    Attributes:
        slug (str): Lowercase slug identifying the sending system; becomes the `/webhooks/{slug}` path segment. Example:
            grafana.
    """

    slug: str

    def to_dict(self) -> dict[str, Any]:
        slug = self.slug

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "slug": slug,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        slug = d.pop("slug")

        create_webhook_source_request = cls(
            slug=slug,
        )

        return create_webhook_source_request
