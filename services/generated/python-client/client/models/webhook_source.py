from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..types import UNSET, Unset

T = TypeVar("T", bound="WebhookSource")


@_attrs_define
class WebhookSource:
    """A registered webhook source; the secret is never included.

    Attributes:
        slug (str): Path segment senders deliver to (`POST /webhooks/{slug}`). Example: github.
        created_at (datetime.datetime):
        secret_rotated_at (datetime.datetime | Unset): Set once the secret has been rotated after creation.
        last_event_at (datetime.datetime | Unset): Receipt time of the newest external event for this slug; absent if
            none arrived yet.
    """

    slug: str
    created_at: datetime.datetime
    secret_rotated_at: datetime.datetime | Unset = UNSET
    last_event_at: datetime.datetime | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        slug = self.slug

        created_at = self.created_at.isoformat()

        secret_rotated_at: str | Unset = UNSET
        if not isinstance(self.secret_rotated_at, Unset):
            secret_rotated_at = self.secret_rotated_at.isoformat()

        last_event_at: str | Unset = UNSET
        if not isinstance(self.last_event_at, Unset):
            last_event_at = self.last_event_at.isoformat()

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "slug": slug,
                "createdAt": created_at,
            }
        )
        if secret_rotated_at is not UNSET:
            field_dict["secretRotatedAt"] = secret_rotated_at
        if last_event_at is not UNSET:
            field_dict["lastEventAt"] = last_event_at

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        slug = d.pop("slug")

        created_at = isoparse(d.pop("createdAt"))

        _secret_rotated_at = d.pop("secretRotatedAt", UNSET)
        secret_rotated_at: datetime.datetime | Unset
        if isinstance(_secret_rotated_at, Unset):
            secret_rotated_at = UNSET
        else:
            secret_rotated_at = isoparse(_secret_rotated_at)

        _last_event_at = d.pop("lastEventAt", UNSET)
        last_event_at: datetime.datetime | Unset
        if isinstance(_last_event_at, Unset):
            last_event_at = UNSET
        else:
            last_event_at = isoparse(_last_event_at)

        webhook_source = cls(
            slug=slug,
            created_at=created_at,
            secret_rotated_at=secret_rotated_at,
            last_event_at=last_event_at,
        )

        return webhook_source
