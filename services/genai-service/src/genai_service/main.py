from contextlib import asynccontextmanager

import httpx
import structlog
from fastapi import FastAPI

from genai_service.config import settings
from genai_service.ollama_client import OllamaClient
from genai_service.routes.ollama_health import router as ollama_health_router

_log_processors: list = [
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
    ollama = OllamaClient(http, settings.ollama_url, settings.ollama_model)

    app.state.http = http
    app.state.ollama_client = ollama

    logger.info(
        "genai_service_started",
        ollama_url=settings.ollama_url,
        model=settings.ollama_model,
    )

    yield

    await http.aclose()
    logger.info("genai_service_stopped")


app = FastAPI(title="GenAI Service", version="0.1.0", lifespan=lifespan)


@app.get("/health/live")
async def health_live() -> dict[str, str]:
    """Process is up; does not call Ollama (for Docker / Kubernetes probes)."""
    return {"status": "ok"}


app.include_router(ollama_health_router, prefix="/api/v1")
