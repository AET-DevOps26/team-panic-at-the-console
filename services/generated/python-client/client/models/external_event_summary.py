from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..types import UNSET, Unset

T = TypeVar("T", bound="ExternalEventSummary")


@_attrs_define
class ExternalEventSummary:
    """External event list entry (without the raw payload).

    Attributes:
        id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000e1.
        source (str):  Example: github.
        event_type (str):  Example: ci_failure.
        received_at (datetime.datetime):
        delivery_id (str | Unset): Sender-supplied delivery id (e.g. X-GitHub-Delivery), used for dedup.
        published_at (datetime.datetime | Unset): Set once the NATS publish succeeded; absent while the publish is
            pending.
    """

    id: UUID
    source: str
    event_type: str
    received_at: datetime.datetime
    delivery_id: str | Unset = UNSET
    published_at: datetime.datetime | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        source = self.source

        event_type = self.event_type

        received_at = self.received_at.isoformat()

        delivery_id = self.delivery_id

        published_at: str | Unset = UNSET
        if not isinstance(self.published_at, Unset):
            published_at = self.published_at.isoformat()

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "source": source,
                "eventType": event_type,
                "receivedAt": received_at,
            }
        )
        if delivery_id is not UNSET:
            field_dict["deliveryId"] = delivery_id
        if published_at is not UNSET:
            field_dict["publishedAt"] = published_at

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        source = d.pop("source")

        event_type = d.pop("eventType")

        received_at = isoparse(d.pop("receivedAt"))

        delivery_id = d.pop("deliveryId", UNSET)

        _published_at = d.pop("publishedAt", UNSET)
        published_at: datetime.datetime | Unset
        if isinstance(_published_at, Unset):
            published_at = UNSET
        else:
            published_at = isoparse(_published_at)

        external_event_summary = cls(
            id=id,
            source=source,
            event_type=event_type,
            received_at=received_at,
            delivery_id=delivery_id,
            published_at=published_at,
        )

        return external_event_summary
