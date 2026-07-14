from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

T = TypeVar("T", bound="WebhookReceipt")


@_attrs_define
class WebhookReceipt:
    """Acknowledgement returned by the webhook ingest endpoint.

    Attributes:
        id (UUID): External Event id; the `sourceId` on the resulting NATS event. Example:
            018e2c5f-1234-7abc-8def-0000000000e1.
        source (str):  Example: github.
        event_type (str): Normalised event type Rules are evaluated against. Example: ci_failure.
        received_at (datetime.datetime):
        duplicate (bool): True when a redelivery matched an already-stored event (not re-published).
    """

    id: UUID
    source: str
    event_type: str
    received_at: datetime.datetime
    duplicate: bool

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        source = self.source

        event_type = self.event_type

        received_at = self.received_at.isoformat()

        duplicate = self.duplicate

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "source": source,
                "eventType": event_type,
                "receivedAt": received_at,
                "duplicate": duplicate,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        source = d.pop("source")

        event_type = d.pop("eventType")

        received_at = isoparse(d.pop("receivedAt"))

        duplicate = d.pop("duplicate")

        webhook_receipt = cls(
            id=id,
            source=source,
            event_type=event_type,
            received_at=received_at,
            duplicate=duplicate,
        )

        return webhook_receipt
