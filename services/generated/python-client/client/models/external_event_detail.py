from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..types import UNSET, Unset

if TYPE_CHECKING:
    from ..models.external_event_detail_raw_payload import ExternalEventDetailRawPayload


T = TypeVar("T", bound="ExternalEventDetail")


@_attrs_define
class ExternalEventDetail:
    """Single external event including the verbatim raw payload.

    Attributes:
        id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000e1.
        source (str):  Example: github.
        event_type (str):  Example: ci_failure.
        received_at (datetime.datetime):
        raw_payload (ExternalEventDetailRawPayload): The original webhook payload, preserved verbatim.
        delivery_id (str | Unset): Sender-supplied delivery id (e.g. X-GitHub-Delivery), used for dedup.
        published_at (datetime.datetime | Unset): Set once the NATS publish succeeded; absent while the publish is
            pending.
    """

    id: UUID
    source: str
    event_type: str
    received_at: datetime.datetime
    raw_payload: ExternalEventDetailRawPayload
    delivery_id: str | Unset = UNSET
    published_at: datetime.datetime | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        source = self.source

        event_type = self.event_type

        received_at = self.received_at.isoformat()

        raw_payload = self.raw_payload.to_dict()

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
                "rawPayload": raw_payload,
            }
        )
        if delivery_id is not UNSET:
            field_dict["deliveryId"] = delivery_id
        if published_at is not UNSET:
            field_dict["publishedAt"] = published_at

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.external_event_detail_raw_payload import ExternalEventDetailRawPayload

        d = dict(src_dict)
        id = UUID(d.pop("id"))

        source = d.pop("source")

        event_type = d.pop("eventType")

        received_at = isoparse(d.pop("receivedAt"))

        raw_payload = ExternalEventDetailRawPayload.from_dict(d.pop("rawPayload"))

        delivery_id = d.pop("deliveryId", UNSET)

        _published_at = d.pop("publishedAt", UNSET)
        published_at: datetime.datetime | Unset
        if isinstance(_published_at, Unset):
            published_at = UNSET
        else:
            published_at = isoparse(_published_at)

        external_event_detail = cls(
            id=id,
            source=source,
            event_type=event_type,
            received_at=received_at,
            raw_payload=raw_payload,
            delivery_id=delivery_id,
            published_at=published_at,
        )

        return external_event_detail
