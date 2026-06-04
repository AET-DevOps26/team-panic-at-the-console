from contextlib import asynccontextmanager

import httpx
import structlog
from fastapi import FastAPI

from client import Client as IncidentApiClient
from genai_service.config import settings
from genai_service.handlers import IncidentHandlers
from genai_service.nats_consumer import NatsConsumer
from genai_service.ollama_client import OllamaClient
from genai_service.prompts import PromptBuilder
from genai_service.routes.debug_generate import router as debug_generate_router
from genai_service.routes.ollama_health import router as ollama_health_router

_log_processors: list[structlog.typing.Processor] = [
    structlog.processors.TimeStamper(fmt="iso"),
    structlog.processors.add_log_level,
]
if settings.log_json:
    _log_processors.append(structlog.processors.JSONRenderer())
else:
    _log_processors.append(structlog.dev.ConsoleRenderer())

structlog.configure(processors=_log_processors)

logger = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    http = httpx.AsyncClient()
    ollama = OllamaClient(
        http,
        settings.ollama_url,
        settings.ollama_model,
        generate_timeout_seconds=settings.ollama_generate_timeout_seconds,
    )
    incident_api_client = IncidentApiClient(
        base_url=settings.incident_service_url,
        timeout=httpx.Timeout(settings.incident_service_timeout_seconds),
    )
    handlers = IncidentHandlers(incident_api_client, ollama, PromptBuilder())

    consumer: NatsConsumer | None = None
    if settings.nats_enabled:
        consumer = NatsConsumer(
            settings.nats_url,
            handlers,
            queue_group=settings.nats_queue_group,
            connect_timeout_seconds=settings.nats_connect_timeout_seconds,
        )
        try:
            await consumer.start()
        except Exception as exc:
            # Don't block /health on a broker outage; log degraded and continue.
            logger.error("nats_consumer_start_failed", error=str(exc))
            consumer = None

    app.state.http = http
    app.state.ollama_client = ollama
    app.state.incident_api_client = incident_api_client
    app.state.handlers = handlers
    app.state.nats_consumer = consumer

    logger.info(
        "genai_service_started",
        ollama_url=settings.ollama_url,
        model=settings.ollama_model,
        nats_enabled=settings.nats_enabled,
        nats_url=settings.nats_url if settings.nats_enabled else None,
        incident_service_url=settings.incident_service_url,
    )

    try:
        async with incident_api_client:
            yield
    finally:
        if consumer is not None:
            await consumer.stop()
        await http.aclose()
        logger.info("genai_service_stopped")


app = FastAPI(title="GenAI Service", version="0.1.0", lifespan=lifespan)


@app.get("/health")
async def health() -> dict[str, str]:
    """Process is up; does not call Ollama (for Docker / Kubernetes probes)."""
    return {"status": "ok"}


app.include_router(ollama_health_router, prefix="/api/v1")

if settings.debug_endpoints:
    app.include_router(debug_generate_router, prefix="/api/v1")
    logger.warning("debug_endpoints_enabled", path="/api/v1/genai/_debug/generate")
