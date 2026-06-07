from typing import Protocol, TypeVar, overload, runtime_checkable

import structlog
from pydantic import BaseModel

from genai_service.metrics import llm_fallback_total

T = TypeVar("T", bound=BaseModel)

logger = structlog.get_logger(__name__)


@runtime_checkable
class LLMClient(Protocol):
    """Provider-neutral completion contract.

    Both `OllamaClient` and `LogosClient` satisfy this structurally, so handlers
    depend on the behaviour (`generate` + `reachable`) rather than a concrete provider.
    """

    model: str

    async def reachable(self) -> bool: ...

    @overload
    async def generate(
        self,
        prompt: str,
        *,
        system: str | None = None,
        response_model: None = None,
        timeout: float | None = None,
    ) -> str: ...

    @overload
    async def generate(
        self,
        prompt: str,
        *,
        system: str | None = None,
        response_model: type[T],
        timeout: float | None = None,
    ) -> T: ...

    async def generate(
        self,
        prompt: str,
        *,
        system: str | None = None,
        response_model: type[T] | None = None,
        timeout: float | None = None,
    ) -> str | T: ...


class FallbackLLMClient:
    """Tries `primary`, falls back to `backup` if the primary call raises.

    Used to keep Ollama as a safety net when the primary (Logos) is unreachable
    (e.g. TUM network hiccup) or rate-limited beyond its retry budget.
    """

    def __init__(self, primary: LLMClient, backup: LLMClient) -> None:
        self._primary = primary
        self._backup = backup
        # Expose the primary's model id for health/debug surfaces.
        self.model = primary.model
        self.last_provider_used = "unknown"

    async def reachable(self) -> bool:
        return await self._primary.reachable() or await self._backup.reachable()

    @overload
    async def generate(
        self,
        prompt: str,
        *,
        system: str | None = None,
        response_model: None = None,
        timeout: float | None = None,
    ) -> str: ...

    @overload
    async def generate(
        self,
        prompt: str,
        *,
        system: str | None = None,
        response_model: type[T],
        timeout: float | None = None,
    ) -> T: ...

    async def generate(
        self,
        prompt: str,
        *,
        system: str | None = None,
        response_model: type[T] | None = None,
        timeout: float | None = None,
    ) -> str | T:
        primary_provider = provider_name(self._primary)
        backup_provider = provider_name(self._backup)
        try:
            result = await self._primary.generate(
                prompt,
                system=system,
                response_model=response_model,
                timeout=timeout,
            )
            self.last_provider_used = primary_provider
            return result
        except Exception as exc:
            logger.warning(
                "llm_primary_failed_falling_back",
                error=str(exc),
                primary_model=self._primary.model,
                backup_model=self._backup.model,
            )
            llm_fallback_total.labels(
                from_provider=primary_provider, to_provider=backup_provider
            ).inc()
            self.last_provider_used = backup_provider
            return await self._backup.generate(
                prompt,
                system=system,
                response_model=response_model,
                timeout=timeout,
            )


def provider_name(client: LLMClient) -> str:
    """Return a stable provider label for Prometheus metrics."""
    if isinstance(client, FallbackLLMClient):
        return client.last_provider_used
    explicit = getattr(client, "provider", None)
    if isinstance(explicit, str):
        return explicit
    return "unknown"
