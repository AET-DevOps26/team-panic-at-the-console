import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from genai_service.metrics import (
    nats_consumer_connected,
    nats_messages_total,
    set_nats_consumer_connected,
)
from genai_service.nats_consumer import NatsConsumer
from genai_service.regen_task import RegenTask


@pytest.fixture()
def consumer():
    handlers = MagicMock()
    handlers.on_incident_created = AsyncMock()
    handlers.on_incident_resolved = AsyncMock()
    handlers.on_regen_requested = AsyncMock()
    return NatsConsumer("nats://test:4222", handlers), handlers


def _msg(data: dict) -> MagicMock:
    m = MagicMock()
    m.data = json.dumps(data).encode("utf-8")
    return m


async def test_callback_extracts_incident_id_and_dispatches(consumer):
    nc, handlers = consumer

    cb = nc._make_callback("incident.created", handlers.on_incident_created)
    await cb(_msg({"incidentId": "inc-1", "timestamp": "2026-05-20T09:00:00Z"}))

    handlers.on_incident_created.assert_awaited_once_with("inc-1")


async def test_callback_ignores_malformed_json(consumer):
    nc, handlers = consumer
    bad = MagicMock()
    bad.data = b"not json"

    cb = nc._make_callback("incident.created", handlers.on_incident_created)
    await cb(bad)

    handlers.on_incident_created.assert_not_awaited()


async def test_callback_ignores_payload_missing_incident_id(consumer):
    nc, handlers = consumer

    cb = nc._make_callback("incident.created", handlers.on_incident_created)
    await cb(_msg({"timestamp": "2026-05-20T09:00:00Z"}))

    handlers.on_incident_created.assert_not_awaited()


async def test_callback_swallows_handler_exception(consumer):
    nc, handlers = consumer
    handlers.on_incident_created.side_effect = RuntimeError("boom")

    cb = nc._make_callback("incident.created", handlers.on_incident_created)
    await cb(_msg({"incidentId": "inc-1", "timestamp": "now"}))

    handlers.on_incident_created.assert_awaited_once()


async def test_regen_callback_extracts_task(consumer):
    nc, handlers = consumer

    cb = nc._make_regen_callback(
        "incident.regen.requested", handlers.on_regen_requested
    )
    await cb(
        _msg(
            {
                "incidentId": "inc-1",
                "timestamp": "2026-05-20T09:00:00Z",
                "task": "SUMMARY",
            }
        )
    )

    handlers.on_regen_requested.assert_awaited_once_with("inc-1", RegenTask.SUMMARY)


async def test_regen_callback_ignores_payload_missing_task(consumer):
    nc, handlers = consumer

    cb = nc._make_regen_callback(
        "incident.regen.requested", handlers.on_regen_requested
    )
    await cb(_msg({"incidentId": "inc-1", "timestamp": "2026-05-20T09:00:00Z"}))

    handlers.on_regen_requested.assert_not_awaited()


async def test_callback_records_handled_message(consumer):
    nc, handlers = consumer
    before = nats_messages_total.labels(
        subject="incident.created", outcome="handled"
    )._value.get()

    cb = nc._make_callback("incident.created", handlers.on_incident_created)
    await cb(_msg({"incidentId": "inc-1", "timestamp": "now"}))

    assert (
        nats_messages_total.labels(
            subject="incident.created", outcome="handled"
        )._value.get()
        == before + 1
    )


async def test_callback_records_invalid_payload(consumer):
    nc, handlers = consumer
    before = nats_messages_total.labels(
        subject="incident.created", outcome="invalid_payload"
    )._value.get()

    cb = nc._make_callback("incident.created", handlers.on_incident_created)
    await cb(_msg({"timestamp": "now"}))

    assert (
        nats_messages_total.labels(
            subject="incident.created", outcome="invalid_payload"
        )._value.get()
        == before + 1
    )
    handlers.on_incident_created.assert_not_awaited()


async def test_start_sets_nats_consumer_connected_gauge():
    handlers = MagicMock()
    handlers.on_incident_created = AsyncMock()
    handlers.on_incident_resolved = AsyncMock()
    handlers.on_regen_requested = AsyncMock()

    fake_nc = AsyncMock()
    fake_nc.is_closed = False

    with patch("genai_service.nats_consumer.NatsClient", return_value=fake_nc):
        consumer = NatsConsumer("nats://test:4222", handlers)
        await consumer.start()

    assert nats_consumer_connected._value.get() == 1.0
    set_nats_consumer_connected(False)
