import httpx


class OllamaClient:
    def __init__(self, http: httpx.AsyncClient, base_url: str, model: str) -> None:
        self._http = http
        self._base_url = base_url.rstrip("/")
        self.model = model

    async def reachable(self) -> bool:
        try:
            response = await self._http.get(f"{self._base_url}/api/tags", timeout=2.0)
            return response.status_code == 200
        except (httpx.HTTPError, OSError):
            return False
