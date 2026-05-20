import asyncio
import json
from collections.abc import Awaitable, Callable

import structlog
from nats.aio.client import Client as NatsClient
from nats.aio.msg import Msg

from genai_service.handlers import IncidentHandlers

logger = structlog.get_logger(__name__)

IncidentHandler = Callable[[str], Awaitable[None]]


class NatsConsumer:
    """Subscribes to incident.* subjects and dispatches to IncidentHandlers.

    Uses a queue group so multiple genai-service replicas share work (each message
    is delivered to one subscriber in the group). NATS event payloads are thin
    (`{incidentId, timestamp}` — see api/specs/nats/*.schema.json); we only need
    `incidentId` to fetch the rest from incident-service.
    """

    def __init__(
        self,
        nats_url: str,
        handlers: IncidentHandlers,
        queue_group: str = "genai-service",
        connect_timeout_seconds: float = 5.0,
    ) -> None:
        self._url = nats_url
        self._handlers = handlers
        self._queue = queue_group
        self._connect_timeout = connect_timeout_seconds
        self._nc: NatsClient | None = None

    async def start(self) -> None:
        self._nc = NatsClient()
        await self._nc.connect(
            servers=[self._url],
            connect_timeout=int(self._connect_timeout),
            max_reconnect_attempts=-1,
        )

        await self._subscribe("incident.created", self._handlers.on_incident_created)
        await self._subscribe("incident.resolved", self._handlers.on_incident_resolved)
        await self._subscribe(
            "incident.regen.requested", self._handlers.on_regen_requested
        )

        logger.info("nats_consumer_started", url=self._url, queue=self._queue)

    async def stop(self) -> None:
        if self._nc is not None and not self._nc.is_closed:
            await self._nc.drain()
            self._nc = None
            logger.info("nats_consumer_stopped")

    async def _subscribe(self, subject: str, handler: IncidentHandler) -> None:
        assert self._nc is not None
        await self._nc.subscribe(
            subject,
            queue=self._queue,
            cb=self._make_callback(subject, handler),
        )

    def _make_callback(
        self, subject: str, handler: IncidentHandler
    ) -> Callable[[Msg], Awaitable[None]]:
        async def callback(msg: Msg) -> None:
            try:
                payload = json.loads(msg.data.decode("utf-8"))
                incident_id = payload["incidentId"]
            except (
                UnicodeDecodeError,
                json.JSONDecodeError,
                KeyError,
                TypeError,
            ) as exc:
                logger.error("nats_payload_invalid", subject=subject, error=str(exc))
                return

            try:
                await handler(incident_id)
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                # Handler itself logs its own errors; this is a defensive backstop.
                logger.error(
                    "nats_handler_uncaught",
                    subject=subject,
                    incident_id=incident_id,
                    error=str(exc),
                )

        return callback
