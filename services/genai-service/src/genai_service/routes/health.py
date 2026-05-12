from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

router = APIRouter()


@router.get("/genai/health")
async def genai_health(request: Request) -> JSONResponse:
    ollama = request.app.state.ollama_client
    reachable = await ollama.reachable()
    body = {"status": "ok", "ollamaReachable": reachable, "model": ollama.model}
    status_code = 200 if reachable else 503
    return JSONResponse(content=body, status_code=status_code)
