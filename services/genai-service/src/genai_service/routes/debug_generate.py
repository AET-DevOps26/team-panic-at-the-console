from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from genai_service.ollama_client import OllamaError

router = APIRouter()


class DebugGenerateRequest(BaseModel):
    prompt: str = Field(min_length=1)
    system: str | None = None


class DebugGenerateResponse(BaseModel):
    model: str
    response: str


@router.post("/genai/_debug/generate", response_model=DebugGenerateResponse)
async def debug_generate(
    body: DebugGenerateRequest, request: Request
) -> DebugGenerateResponse:
    """Manual smoke test for the Ollama wiring. Disabled unless GENAI_DEBUG_ENDPOINTS=true."""
    ollama = request.app.state.ollama_client
    try:
        text = await ollama.generate(body.prompt, system=body.system)
    except OllamaError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    return DebugGenerateResponse(model=ollama.model, response=text)
