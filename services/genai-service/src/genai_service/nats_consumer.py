import asyncio
import json
from collections.abc import Awaitable, Callable

import structlog
from nats.aio.client import Client as NatsClient
from nats.aio.msg import Msg

from genai_service.handlers import IncidentHandlers
from genai_service.regen_task import RegenTask

logger = structlog.get_logger(__name__)

IncidentHandler = Callable[[str], Awaitable[None]]
RegenHandler = Callable[[str, RegenTask], Awaitable[None]]


class NatsConsumer:
    """Subscribes to incident.* subjects and dispatches to IncidentHandlers.

    Uses a queue group so multiple genai-service replicas share work (each message
    is delivered to one subscriber in the group). NATS event payloads are thin
    (`{incidentId, timestamp}` — see api/specs/nats/*.schema.json). Regen events also
    carry `task` (SUMMARY, SEVERITY_SUGGESTION, SOLUTION_SUGGESTIONS, POSTMORTEM).
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
        # nats-py expects an int (seconds); round up so sub-second values are not truncated to 0.
        connect_timeout_s = max(1, round(self._connect_timeout))
        await self._nc.connect(
            servers=[self._url],
            connect_timeout=connect_timeout_s,
            max_reconnect_attempts=-1,
        )

        await self._subscribe("incident.created", self._handlers.on_incident_created)
        await self._subscribe("incident.resolved", self._handlers.on_incident_resolved)
        await self._subscribe_regen(
            "incident.regen.requested", self._handlers.on_regen_requested
        )

        logger.info("nats_consumer_started", url=self._url, queue=self._queue)

    @property
    def nats_client(self) -> NatsClient | None:
        return self._nc

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

    async def _subscribe_regen(self, subject: str, handler: RegenHandler) -> None:
        assert self._nc is not None
        await self._nc.subscribe(
            subject,
            queue=self._queue,
            cb=self._make_regen_callback(subject, handler),
        )

    def _make_regen_callback(
        self, subject: str, handler: RegenHandler
    ) -> Callable[[Msg], Awaitable[None]]:
        async def callback(msg: Msg) -> None:
            try:
                payload = json.loads(msg.data.decode("utf-8"))
                incident_id = payload["incidentId"]
                task = RegenTask(payload["task"])
            except (
                UnicodeDecodeError,
                json.JSONDecodeError,
                KeyError,
                TypeError,
                ValueError,
            ) as exc:
                logger.error("nats_payload_invalid", subject=subject, error=str(exc))
                return

            try:
                await handler(incident_id, task)
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                logger.error(
                    "nats_handler_uncaught",
                    subject=subject,
                    incident_id=incident_id,
                    task=task.value,
                    error=str(exc),
                )

        return callback

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
