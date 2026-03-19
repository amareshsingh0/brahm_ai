"""
Chat Router — Two-pass reasoning flow:
  Pass 1: Gemini decides intent + what tools to run (query_router.py)
  Tools:  Python services run calculations if needed (tool_executor.py)
  Pass 2: Gemini streams final expert answer (prompt_builder + rag_service)
"""
import asyncio
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from api.models.chat import ChatRequest
from api.dependencies import get_rag_state
from api.services.rag_service import hybrid_search, generate_stream
from api.services.query_router import get_pass1_decision
from api.services.tool_executor import execute_tools, compress_kundali
from api.services.geo_service import get_coords

router = APIRouter()


def _normalize_birth_data(bd: dict) -> dict:
    """Convert Gemini-extracted birth_data to tool_executor format using geo_service."""
    place = bd.get("place") or ""
    lat, lon, tz = get_coords(place)
    return {
        "birth_date": bd.get("date", ""),
        "birth_time": bd.get("time", ""),
        "birth_lat": lat,
        "birth_lon": lon,
        "birth_tz": tz,
        "name": bd.get("name", ""),
        "place": place,
    }


@router.post("/chat")
async def chat(req: ChatRequest, state: dict = Depends(get_rag_state)):
    history = [{"role": m.role, "content": m.content} for m in req.history]

    # ── Load user kundali from store (agar frontend ne bheja) ──────────
    # For now: kundali comes from req.page_data["kundali"] if page_context is kundali
    # When DB auth is wired: load from DB using current_user["sub"]
    # kundali_raw can come from page_data on any page (auto-sent by useChat hook)
    user_kundali_raw = req.page_data.get("kundali_raw")

    kundali_summary = compress_kundali(user_kundali_raw) if user_kundali_raw else None

    # ── PASS 1: Gemini decides what to do ──────────────────────────────
    decision = await asyncio.get_event_loop().run_in_executor(
        None,
        lambda: get_pass1_decision(
            query=req.message,
            page_context=req.page_context,
            kundali_summary=kundali_summary,
            page_data=req.page_data,
            history=history,
        )
    )

    # ── TOOL EXECUTION: Run Python calculation services ─────────────────
    # Priority: 1) Gemini-extracted birth_data from message/history
    #           2) page_data.birth_data (sent by frontend)
    #           3) page_data itself (if on kundali/timeline page)
    extracted_bd = decision.get("birth_data") or {}
    if extracted_bd.get("date") and extracted_bd.get("place"):
        user_birth_data = _normalize_birth_data(extracted_bd)
    elif req.page_data.get("user_birth_data"):
        # Sent automatically from frontend store (profile data)
        user_birth_data = req.page_data["user_birth_data"]
    elif req.page_data.get("birth_data"):
        user_birth_data = req.page_data["birth_data"]
    elif req.page_context in {"kundali", "timeline"} and req.page_data:
        user_birth_data = req.page_data
    else:
        user_birth_data = None

    tool_results = {}
    if decision.get("needs_calculation") and decision.get("calc_services"):
        tool_results = await execute_tools(
            decision=decision,
            user_birth_data=user_birth_data,
            page_data=req.page_data,
        )
        # If kundali was freshly calculated, use it as kundali_summary for Pass 2
        if not kundali_summary and tool_results.get("kundali"):
            kundali_summary = tool_results["kundali"]

    # ── RAG SEARCH: Only when needs_rag=true ────────────────────────────
    retrieved = []
    if decision.get("needs_rag"):
        rag_query = decision.get("rag_query") or req.message
        retrieved = await asyncio.get_event_loop().run_in_executor(
            None, lambda: hybrid_search(rag_query, req.language)
        )
        retrieved = retrieved[:3]  # max 3 docs — keep context clean

    # ── PASS 2: Stream final answer ──────────────────────────────────────
    def event_generator():
        for chunk in generate_stream(
            query=req.message,
            retrieved=retrieved,
            history=history,
            decision=decision,
            tool_results=tool_results,
            kundali_summary=kundali_summary,
            page_context=req.page_context,
            page_data=req.page_data,
            language=req.language,
        ):
            yield f"data: {chunk}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
