import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from genai_service.nats_consumer import NatsConsumer


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


async def test_start_subscribes_to_all_three_subjects_in_queue_group():
    handlers = MagicMock()
    handlers.on_incident_created = AsyncMock()
    handlers.on_incident_resolved = AsyncMock()
    handlers.on_regen_requested = AsyncMock()

    fake_nc = AsyncMock()
    fake_nc.is_closed = False

    with patch("genai_service.nats_consumer.NatsClient", return_value=fake_nc):
        consumer = NatsConsumer(
            "nats://test:4222", handlers, queue_group="genai-service"
        )
        await consumer.start()

    fake_nc.connect.assert_awaited_once()
    connect_kwargs = fake_nc.connect.await_args.kwargs
    assert connect_kwargs["servers"] == ["nats://test:4222"]

    subscribed = {
        call.kwargs["subject"] if "subject" in call.kwargs else call.args[0]: call
        for call in fake_nc.subscribe.await_args_list
    }
    assert set(subscribed) == {
        "incident.created",
        "incident.resolved",
        "incident.regen.requested",
    }
    for call in subscribed.values():
        assert call.kwargs["queue"] == "genai-service"


async def test_stop_drains_connection():
    handlers = MagicMock()
    handlers.on_incident_created = AsyncMock()
    handlers.on_incident_resolved = AsyncMock()
    handlers.on_regen_requested = AsyncMock()

    fake_nc = AsyncMock()
    fake_nc.is_closed = False

    with patch("genai_service.nats_consumer.NatsClient", return_value=fake_nc):
        consumer = NatsConsumer("nats://test:4222", handlers)
        await consumer.start()
        await consumer.stop()

    fake_nc.drain.assert_awaited_once()


async def test_stop_is_noop_when_never_started():
    handlers = MagicMock()
    consumer = NatsConsumer("nats://test:4222", handlers)
    await consumer.stop()  # must not raise
