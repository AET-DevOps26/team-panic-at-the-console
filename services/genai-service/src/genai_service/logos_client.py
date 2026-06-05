import asyncio
from typing import TypeVar, overload

import httpx
import structlog
from pydantic import BaseModel, ValidationError

T = TypeVar("T", bound=BaseModel)

logger = structlog.get_logger(__name__)

_RETRY_STATUS = 429


class LogosError(RuntimeError):
    """Raised when Logos returns a non-success status or an unparsable response."""


class LogosClient:
    """Client for the TUM Logos OpenAI-compatible API (`/v1/chat/completions`).

    Structured output is requested via `response_format: json_schema`. Logos enforces
    a 60 RPM / 1M TPM budget, so 429s are retried with exponential backoff before
    surfacing as `LogosError` (which the fallback wrapper turns into an Ollama call).
    """

    def __init__(
        self,
        http: httpx.AsyncClient,
        base_url: str,
        model: str,
        *,
        api_key: str,
        generate_timeout_seconds: float = 120.0,
        max_retries: int = 3,
        retry_backoff_seconds: float = 1.0,
    ) -> None:
        self._http = http
        self._base_url = base_url.rstrip("/")
        self.model = model
        self._api_key = api_key
        self._generate_timeout = generate_timeout_seconds
        self._max_retries = max_retries
        self._retry_backoff = retry_backoff_seconds

    @property
    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }

    async def reachable(self) -> bool:
        try:
            response = await self._http.get(
                f"{self._base_url}/v1/models", headers=self._headers, timeout=2.0
            )
            return response.status_code == 200
        except (httpx.HTTPError, OSError):
            return False

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
        """Single-shot chat completion. Mirrors `OllamaClient.generate`'s contract.

        Pass `response_model` to constrain output to a Pydantic model via the
        OpenAI `json_schema` response format; the content is parsed back into an
        instance. Without it, the raw assistant text is returned.
        """
        messages: list[dict[str, str]] = []
        if system is not None:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})

        body: dict[str, object] = {
            "model": self.model,
            "messages": messages,
            "stream": False,
        }
        if response_model is not None:
            body["response_format"] = {
                "type": "json_schema",
                "json_schema": {
                    "name": response_model.__name__,
                    "schema": response_model.model_json_schema(),
                    "strict": True,
                },
            }

        resp = await self._post_with_retry(body, timeout)

        if resp.status_code != 200:
            raise LogosError(
                f"Logos returned status {resp.status_code}: {resp.text[:200]}"
            )

        try:
            payload = resp.json()
        except ValueError as exc:
            raise LogosError(f"Logos returned invalid JSON: {resp.text[:200]}") from exc
        try:
            text = payload["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError) as exc:
            raise LogosError(
                f"Logos response missing choices[0].message.content: {exc}"
            ) from exc

        if response_model is None:
            return text
        try:
            return response_model.model_validate_json(text)
        except ValidationError as exc:
            raise LogosError(
                f"Logos returned JSON that did not match {response_model.__name__}: {exc}"
            ) from exc

    async def _post_with_retry(
        self, body: dict[str, object], timeout: float | None
    ) -> httpx.Response:
        effective_timeout = timeout if timeout is not None else self._generate_timeout
        last_resp: httpx.Response | None = None
        for attempt in range(self._max_retries):
            try:
                resp = await self._http.post(
                    f"{self._base_url}/v1/chat/completions",
                    json=body,
                    headers=self._headers,
                    timeout=effective_timeout,
                )
            except httpx.HTTPError as exc:
                raise LogosError(f"Logos request failed: {exc}") from exc

            if resp.status_code != _RETRY_STATUS:
                return resp

            last_resp = resp
            if attempt < self._max_retries - 1:
                delay = self._retry_backoff * (2**attempt)
                logger.warning(
                    "logos_rate_limited_retrying", attempt=attempt, delay=delay
                )
                await asyncio.sleep(delay)

        # Exhausted retries on 429: return the last 429 so the caller raises with status.
        assert last_resp is not None
        return last_resp
