from __future__ import annotations

import sys
from datetime import datetime
from pathlib import Path


def _add_generated_client_to_path() -> None:
    # repository root -> services/generated/python-client
    repo_root = Path(__file__).resolve().parents[4]
    gen_client = repo_root / "services" / "generated" / "python-client"
    sys.path.insert(0, str(gen_client))


def test_incident_and_event_adapters_with_generated_client() -> None:
    _add_generated_client_to_path()

    from client.models.event import Event as ClientEvent
    from client.models.incident import Incident as ClientIncident
    from genai_service.prompts.adapters import events_from_client, incident_from_client

    inc_dict = {
        "id": "018e2c5f-1234-7abc-8def-000000000001",
        "title": "Example",
        "status": "open",
        "severity": "SEV2",
        "createdAt": "2026-05-01T12:00:00Z",
        "description": "an example incident",
    }

    ev_dict = {
        "timestamp": "2026-05-01T12:01:00Z",
        "type": "note",
        "description": "Something happened",
    }

    client_inc = ClientIncident.from_dict(inc_dict)
    client_ev = ClientEvent.from_dict(ev_dict)

    inc = incident_from_client(client_inc)
    evs = events_from_client([client_ev])

    # Basic shape checks
    assert inc.id == inc_dict["id"]
    assert inc.title == inc_dict["title"]
    assert inc.status == inc_dict["status"]
    assert inc.severity == inc_dict["severity"]
    assert inc.description == inc_dict["description"]

    assert len(evs) == 1
    assert evs[0].description == ev_dict["description"]
    # timestamp parsed to datetime
    assert hasattr(evs[0], "timestamp")
    assert isinstance(evs[0].timestamp, datetime)
