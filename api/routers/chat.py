"""
Chat Router — Two-pass reasoning flow:
  Pass 1: Gemini decides intent + what tools to run (query_router.py)
  Memory: Relevant past conversations fetched from Supabase (memory_service.py)
  Tools:  Python services run calculations if needed (tool_executor.py)
  Pass 2: Gemini streams final expert answer (prompt_builder + rag_service)
"""
import asyncio
import json as _json
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from starlette.background import BackgroundTask
from api.models.chat import ChatRequest
from api.dependencies import get_rag_state
from api.services.rag_service import hybrid_search, generate_stream
from api.services.query_router import get_pass1_decision
from api.services.tool_executor import execute_tools, compress_kundali
from api.services.geo_service import get_coords
from api.services.memory_service import get_relevant_memory, format_memory_for_prompt

router = APIRouter()


def _save_chat_messages(user_id: str, session_id: str, page_context: str, user_msg: str, assistant_msg: str):
    """Save user + assistant message pair to Supabase after streaming completes."""
    if not user_id:
        return
    try:
        from api.supabase_client import get_supabase
        sb = get_supabase()
        if sb:
            sb.table("chat_messages").insert([
                {"user_id": user_id, "session_id": session_id, "page_context": page_context,
                 "role": "user", "content": user_msg[:4000]},
                {"user_id": user_id, "session_id": session_id, "page_context": page_context,
                 "role": "assistant", "content": assistant_msg[:8000]},
            ]).execute()
    except Exception:
        pass  # Never break streaming over DB failure


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


def _error_stream(message: str = "Kuch problem ho gayi, thoda baad mein try karo. 🙏"):
    """Yield a graceful error SSE response instead of crashing."""
    yield f"data: {_json.dumps({'type': 'token', 'content': message})}\n\n"
    yield f"data: {_json.dumps({'type': 'done'})}\n\n"


@router.post("/chat")
async def chat(req: ChatRequest, state: dict = Depends(get_rag_state)):
    history = [{"role": m.role, "content": m.content} for m in req.history]

    # ── Load saved kundali from frontend store (auto-attached by useChat) ──
    user_kundali_raw = req.page_data.get("kundali_raw")
    kundali_summary = compress_kundali(user_kundali_raw) if user_kundali_raw else None

    # ── PASS 1: Gemini decides what to do ──────────────────────────────
    try:
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
    except Exception:
        # Nuclear fallback — should never reach here since query_router never raises
        decision = {
            "intent": req.message[:80],
            "query_type": "CONVERSATIONAL",
            "needs_calculation": False,
            "needs_birth_form": False,
            "calc_services": [],
            "needs_rag": False,
            "response_depth": "basic",
            "response_lang": "hi",
            "user_language_style": "hinglish",
        }

    # ── Birth form needed — user wants kundali but no birth data ────────
    if decision.get("needs_birth_form"):
        def _birth_form_event():
            yield f"data: {_json.dumps({'type': 'birth_form'})}\n\n"
        return StreamingResponse(
            _birth_form_event(),
            media_type="text/event-stream",
            headers={"X-Accel-Buffering": "no", "Cache-Control": "no-cache", "Connection": "keep-alive"},
        )

    # ── LONG-TERM MEMORY: Fetch relevant past exchanges ─────────────────
    # Skip memory for pure greetings/small talk (not useful, wastes context)
    query_type = decision.get("query_type", "CONVERSATIONAL")
    memory_exchanges = []
    if query_type not in {"CONVERSATIONAL", "SMALL_TALK"} and req.user_id:
        try:
            memory_exchanges = await asyncio.get_event_loop().run_in_executor(
                None,
                lambda: get_relevant_memory(
                    user_id=req.user_id,
                    query=req.message,
                    current_session_id=req.session_id or "",
                )
            )
        except Exception:
            memory_exchanges = []

    # ── TOOL EXECUTION ──────────────────────────────────────────────────
    # Priority: 1) Gemini-extracted birth_data  2) page_data.user_birth_data
    #           3) page_data.birth_data          4) page_data itself (kundali page)
    extracted_bd = decision.get("birth_data") or {}
    if extracted_bd.get("date") and extracted_bd.get("place"):
        user_birth_data = _normalize_birth_data(extracted_bd)
    elif req.page_data.get("user_birth_data"):
        bd = req.page_data["user_birth_data"]
        # Resolve lat/lon from place name if missing (profile saved without geocoding)
        if (not bd.get("birth_lat") or bd.get("birth_lat") == 0) and bd.get("place"):
            lat, lon, tz = get_coords(bd["place"])
            bd = {**bd, "birth_lat": lat, "birth_lon": lon, "birth_tz": tz}
        user_birth_data = bd
    elif req.page_data.get("birth_data"):
        user_birth_data = req.page_data["birth_data"]
    elif req.page_context in {"kundali", "timeline"} and req.page_data:
        user_birth_data = req.page_data
    else:
        user_birth_data = None

    # If kundali_summary already exists (saved kundali), skip kundali recalculation
    if kundali_summary and decision.get("calc_services"):
        decision["calc_services"] = [s for s in decision["calc_services"] if s != "kundali"]

    kundali_freshly_calculated = False
    tool_results = {}
    if decision.get("needs_calculation") and decision.get("calc_services"):
        try:
            tool_results = await execute_tools(
                decision=decision,
                user_birth_data=user_birth_data,
                page_data=req.page_data,
            )
        except Exception:
            tool_results = {}
        if not kundali_summary and tool_results.get("kundali"):
            kundali_summary = tool_results["kundali"]
            kundali_freshly_calculated = True

    # ── RAG SEARCH ──────────────────────────────────────────────────────
    retrieved = []
    if decision.get("needs_rag"):
        try:
            rag_query = decision.get("rag_query") or req.message
            retrieved = await asyncio.get_event_loop().run_in_executor(
                None, lambda: hybrid_search(rag_query, req.language)
            )
            retrieved = retrieved[:3]
        except Exception:
            retrieved = []

    # ── PASS 2: Stream final answer ──────────────────────────────────────
    assistant_tokens: list[str] = []

    # Inject memory into page_data for prompt_builder
    memory_section = format_memory_for_prompt(memory_exchanges)

    # If kundali was freshly calculated from form input, include birth_data for save prompt
    save_prompt_data = None
    if kundali_freshly_calculated and user_birth_data:
        save_prompt_data = {
            "birth_date": user_birth_data.get("birth_date", ""),
            "birth_time": user_birth_data.get("birth_time", ""),
            "birth_lat":  user_birth_data.get("birth_lat", 28.61),
            "birth_lon":  user_birth_data.get("birth_lon", 77.20),
            "birth_tz":   user_birth_data.get("birth_tz", 5.5),
            "name":       user_birth_data.get("name", ""),
            "place":      user_birth_data.get("place", ""),
            "gender":     req.page_data.get("birth_gender", ""),
        }

    def event_generator():
        try:
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
                memory_section=memory_section,
            ):
                yield f"data: {chunk}\n\n"
                try:
                    parsed = _json.loads(chunk)
                    if parsed.get("type") == "token":
                        assistant_tokens.append(parsed.get("content", ""))
                except Exception:
                    pass
        except Exception:
            yield from _error_stream()
            return

        # After streaming: send save prompt if kundali was freshly calculated
        if save_prompt_data and not req.page_data.get("kundali_raw"):
            yield f"data: {_json.dumps({'type': 'save_kundali_prompt', 'birth_data': save_prompt_data})}\n\n"

    def on_complete():
        _save_chat_messages(
            user_id=req.user_id,
            session_id=req.session_id or "",
            page_context=req.page_context,
            user_msg=req.message,
            assistant_msg="".join(assistant_tokens),
        )

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"X-Accel-Buffering": "no", "Cache-Control": "no-cache", "Connection": "keep-alive"},
        background=BackgroundTask(on_complete),
    )
