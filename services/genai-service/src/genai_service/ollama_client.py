from typing import TypeVar, overload

import httpx
from pydantic import BaseModel, ValidationError

T = TypeVar("T", bound=BaseModel)


class OllamaError(RuntimeError):
    """Raised when Ollama returns a non-200 status or unparsable response."""


class OllamaClient:
    def __init__(
        self,
        http: httpx.AsyncClient,
        base_url: str,
        model: str,
        generate_timeout_seconds: float = 120.0,
    ) -> None:
        self._http = http
        self._base_url = base_url.rstrip("/")
        self.model = model
        self.provider = "ollama"
        self._generate_timeout = generate_timeout_seconds

    async def reachable(self) -> bool:
        try:
            response = await self._http.get(f"{self._base_url}/api/tags", timeout=2.0)
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
        """Single-shot completion against Ollama's `/api/generate` (stream=false).

        Pass `response_model` to constrain output to a Pydantic model: the model's
        JSON schema is sent as `format`, and the response is parsed back into an
        instance. Without it, the raw `response` text is returned.
        """
        body: dict[str, object] = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
        }
        if system is not None:
            body["system"] = system
        if response_model is not None:
            body["format"] = response_model.model_json_schema()

        try:
            resp = await self._http.post(
                f"{self._base_url}/api/generate",
                json=body,
                timeout=timeout if timeout is not None else self._generate_timeout,
            )
        except httpx.HTTPError as exc:
            raise OllamaError(f"Ollama request failed: {exc}") from exc

        if resp.status_code != 200:
            raise OllamaError(
                f"Ollama returned status {resp.status_code}: {resp.text[:200]}"
            )

        try:
            payload = resp.json()
        except ValueError as exc:
            raise OllamaError(
                f"Ollama returned invalid JSON: {resp.text[:200]}"
            ) from exc
        try:
            text = payload["response"]
        except KeyError as exc:
            raise OllamaError(
                f"Ollama response missing 'response' field: {exc}"
            ) from exc

        if response_model is None:
            return text
        try:
            return response_model.model_validate_json(text)
        except ValidationError as exc:
            raise OllamaError(
                f"Ollama returned JSON that did not match {response_model.__name__}: {exc}"
            ) from exc
