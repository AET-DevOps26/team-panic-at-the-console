"""Manual smoke script: runs every PromptTask through PromptBuilder -> OllamaClient
against a live Ollama and prints the generated response.

Not a test (no asserts beyond Pydantic parsing). Use it to eyeball whether the
local model produces reasonable output for our prompts before relying on the
NATS-driven flow.

    OLLAMA_URL=http://localhost:11434 OLLAMA_MODEL=qwen2.5:3b \\
        pixi run --manifest-path services/genai-service/pixi.toml demo
"""

from __future__ import annotations

import asyncio
import os
from datetime import UTC, datetime
from uuid import UUID

import httpx

from client.models import Incident, IncidentEvent, IncidentStatus, Severity
from genai_service.ollama_client import OllamaClient
from genai_service.prompts import (
    PromptBuilder,
    PromptTask,
)

_INCIDENT_OPEN = Incident(
    id=UUID("018e2c5f-1234-7abc-8def-000000000001"),
    title="Checkout service 5xx spike",
    description="Errors on /checkout up sharply since 09:00 UTC.",
    status=IncidentStatus.OPEN,
    severity=Severity.SEV2,
    created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
)

_INCIDENT_RESOLVED = Incident(
    id=UUID("018e2c5f-1234-7abc-8def-000000000002"),
    title="Checkout service 5xx spike",
    description="Errors on /checkout up sharply since 09:00 UTC.",
    status=IncidentStatus.RESOLVED,
    severity=Severity.SEV2,
    created_at=datetime(2026, 5, 20, 9, 0, tzinfo=UTC),
    resolved_at=datetime(2026, 5, 20, 10, 30, tzinfo=UTC),
)

_EVENTS: list[IncidentEvent] = [
    IncidentEvent(
        timestamp=datetime(2026, 5, 20, 9, 2, tzinfo=UTC),
        type_="status_changed",
        description="status: open -> investigating",
    ),
    IncidentEvent(
        timestamp=datetime(2026, 5, 20, 9, 5, tzinfo=UTC),
        type_="comment_added",
        description="Recent deploy to checkout-service at 08:55 UTC may be related.",
    ),
    IncidentEvent(
        timestamp=datetime(2026, 5, 20, 9, 20, tzinfo=UTC),
        type_="comment_added",
        description="Rolled back checkout-service; error rate dropping.",
    ),
    IncidentEvent(
        timestamp=datetime(2026, 5, 20, 10, 30, tzinfo=UTC),
        type_="status_changed",
        description="status: investigating -> resolved",
    ),
]


async def main() -> None:
    url = os.environ.get("OLLAMA_URL", "http://localhost:11434")
    model = os.environ.get("OLLAMA_MODEL", "qwen2.5:3b")

    builder = PromptBuilder()
    async with httpx.AsyncClient() as http:
        ollama = OllamaClient(http, url, model)

        print(f"# Demo against {url} (model={model})\n")
        for task in PromptTask:
            incident = (
                _INCIDENT_RESOLVED if task is PromptTask.POSTMORTEM else _INCIDENT_OPEN
            )
            prompt = builder.build(incident, _EVENTS, task)
            print(f"## {task.value}")
            result = await ollama.generate(
                prompt.user,
                system=prompt.system,
                response_model=prompt.response_model,
            )
            print(result.model_dump_json(indent=2))
            print()


if __name__ == "__main__":
    asyncio.run(main())
