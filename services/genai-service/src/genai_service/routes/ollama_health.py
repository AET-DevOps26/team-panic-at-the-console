from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

router = APIRouter()


@router.get("/genai/ollama/health")
async def genai_ollama_health(request: Request) -> JSONResponse:
    """Ollama reachability; 503 when Ollama is down (not for liveness probes)."""
    ollama = request.app.state.ollama_client
    reachable = await ollama.reachable()
    status = "ok" if reachable else "degraded"
    body = {"status": status, "ollamaReachable": reachable, "model": ollama.model}
    status_code = 200 if reachable else 503
    return JSONResponse(content=body, status_code=status_code)
