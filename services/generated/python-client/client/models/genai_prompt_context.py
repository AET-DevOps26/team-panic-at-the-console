from __future__ import annotations

from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar

from attrs import define as _attrs_define

if TYPE_CHECKING:
    from ..models.event import Event
    from ..models.incident import Incident


T = TypeVar("T", bound="GenaiPromptContext")


@_attrs_define
class GenaiPromptContext:
    """Incident snapshot and event log used when assembling GenAI prompts.

    Example:
        {'incident': {'id': '018e2c5f-1234-7abc-8def-000000000001', 'title': 'Checkout API latency spike',
            'description': 'Elevated p99 latency on checkout-api after deploy v2.4.1', 'status': 'investigating',
            'severity': 'SEV2', 'createdAt': '2026-05-24T14:02:00Z', 'resolvedAt': None}, 'events': [{'timestamp':
            '2026-05-24T14:18:00Z', 'type': 'severity_changed', 'description': 'Severity escalated from SEV3 to SEV2'}]}

    Attributes:
        incident (Incident): Subset of incident representation used by genai prompts.
        events (list[Event]):  Example: [{'timestamp': '2026-05-24T14:18:00Z', 'type': 'severity_changed',
            'description': 'Severity escalated from SEV3 to SEV2'}].
    """

    incident: Incident
    events: list[Event]

    def to_dict(self) -> dict[str, Any]:
        incident = self.incident.to_dict()

        events = []
        for events_item_data in self.events:
            events_item = events_item_data.to_dict()
            events.append(events_item)

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "incident": incident,
                "events": events,
            }
        )

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.event import Event
        from ..models.incident import Incident

        d = dict(src_dict)
        incident = Incident.from_dict(d.pop("incident"))

        events = []
        _events = d.pop("events")
        for events_item_data in _events:
            events_item = Event.from_dict(events_item_data)

            events.append(events_item)

        genai_prompt_context = cls(
            incident=incident,
            events=events,
        )

        return genai_prompt_context
