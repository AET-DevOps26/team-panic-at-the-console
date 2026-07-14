from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..types import UNSET, Unset

T = TypeVar("T", bound="WebhookSourceWithSecret")


@_attrs_define
class WebhookSourceWithSecret:
    """Create/rotate response carrying the generated HMAC secret. The secret is not retrievable afterwards; the server
    keeps it only for signature verification.

        Attributes:
            slug (str):  Example: grafana.
            secret (str): Hex-encoded 256-bit secret for HMAC-SHA256 signing (`X-Hub-Signature-256`). Example:
                6bc1bee22e409f96e93d7e117393172aad4c8f10b0e6371b2b647a2f45c7c463.
            created_at (datetime.datetime):
            secret_rotated_at (datetime.datetime | Unset): Set when this response comes from a rotation.
    """

    slug: str
    secret: str
    created_at: datetime.datetime
    secret_rotated_at: datetime.datetime | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        slug = self.slug

        secret = self.secret

        created_at = self.created_at.isoformat()

        secret_rotated_at: str | Unset = UNSET
        if not isinstance(self.secret_rotated_at, Unset):
            secret_rotated_at = self.secret_rotated_at.isoformat()

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "slug": slug,
                "secret": secret,
                "createdAt": created_at,
            }
        )
        if secret_rotated_at is not UNSET:
            field_dict["secretRotatedAt"] = secret_rotated_at

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        slug = d.pop("slug")

        secret = d.pop("secret")

        created_at = isoparse(d.pop("createdAt"))

        _secret_rotated_at = d.pop("secretRotatedAt", UNSET)
        secret_rotated_at: datetime.datetime | Unset
        if isinstance(_secret_rotated_at, Unset):
            secret_rotated_at = UNSET
        else:
            secret_rotated_at = isoparse(_secret_rotated_at)

        webhook_source_with_secret = cls(
            slug=slug,
            secret=secret,
            created_at=created_at,
            secret_rotated_at=secret_rotated_at,
        )

        return webhook_source_with_secret
