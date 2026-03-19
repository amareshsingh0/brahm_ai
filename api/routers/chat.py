import asyncio
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from api.models.chat import ChatRequest
from api.dependencies import get_rag_state
from api.services.rag_service import hybrid_search, generate_stream

router = APIRouter()


@router.post("/chat")
async def chat(req: ChatRequest, state: dict = Depends(get_rag_state)):
    history = [{"role": m.role, "content": m.content} for m in req.history]
    retrieved = await asyncio.get_event_loop().run_in_executor(
        None, lambda: hybrid_search(req.message, req.language)
    )

    def event_generator():
        for chunk in generate_stream(req.message, retrieved, history):
            yield f"data: {chunk}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
