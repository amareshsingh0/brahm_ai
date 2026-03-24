"""
Brahm AI FastAPI — Entry Point
Run: uvicorn api.main:app --host 0.0.0.0 --port 8000 --workers 1
     (--workers 1 is MANDATORY: Qwen LLM cannot be loaded in multiple processes)
"""
import time, threading
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from api.routers import (
    chat, kundali, panchang, compatibility,
    search, planets, muhurta, grahan,
    horoscope, user, cities, festivals, calendar, palmistry, gochar, rectification,
    prashna, varshphal, kp, admin, auth,
)
from api.services.rag_service import load_all
from api.dependencies import G


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load RAG components lazily in a background thread so the server
    # starts instantly and kundali/panchang endpoints work immediately.
    def _bg():
        try:
            load_all()
        except Exception as e:
            print(f"[RAG] Load failed: {e}")
            G["loaded"] = False

    threading.Thread(target=_bg, daemon=True).start()
    yield


app = FastAPI(
    title="Brahm AI API",
    description="Vedic scriptures RAG + Jyotish calculations",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # tighten in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def _check_user_status(request: Request, call_next):
    """Block suspended or banned users from all non-admin API endpoints."""
    from fastapi.responses import JSONResponse
    path = request.url.path
    if path.startswith("/api/") and not path.startswith("/api/admin"):
        session_id = request.query_params.get("session_id", "")
        if session_id:
            try:
                from api.supabase_client import get_supabase
                sb = get_supabase()
                row = sb.table("users").select("status,role").eq("session_id", session_id).maybe_single().execute()
                if row.data:
                    status = row.data.get("status", "active")
                    role   = row.data.get("role", "user")
                    if status == "suspended":
                        return JSONResponse(status_code=403, content={"detail": "Account suspended. Contact support."})
                    if role == "banned":
                        return JSONResponse(status_code=403, content={"detail": "Account banned."})
            except Exception:
                pass  # If Supabase is unreachable, let the request through
    return await call_next(request)


@app.middleware("http")
async def _activity_log(request: Request, call_next):
    """Log every /api/ request (non-admin) to activity_logs.db asynchronously."""
    path = request.url.path
    if path.startswith("/api/") and not path.startswith("/api/admin"):
        session_id = request.query_params.get("session_id", "")
        method = request.method
        start = time.time()
        response = await call_next(request)
        duration_ms = int((time.time() - start) * 1000)
        threading.Thread(
            target=admin.log_request,
            args=(method, path, session_id, response.status_code, duration_ms),
            daemon=True,
        ).start()
        return response
    return await call_next(request)

PREFIX = "/api"

app.include_router(cities.router,        prefix=PREFIX, tags=["Reference"])
app.include_router(kundali.router,       prefix=PREFIX, tags=["Jyotish"])
app.include_router(panchang.router,      prefix=PREFIX, tags=["Jyotish"])
app.include_router(compatibility.router, prefix=PREFIX, tags=["Jyotish"])
app.include_router(planets.router,       prefix=PREFIX, tags=["Jyotish"])
app.include_router(grahan.router,        prefix=PREFIX, tags=["Jyotish"])
app.include_router(festivals.router,     prefix=PREFIX, tags=["Jyotish"])
app.include_router(calendar.router,      prefix=PREFIX, tags=["Jyotish"])
app.include_router(muhurta.router,       prefix=PREFIX, tags=["Jyotish"])
app.include_router(horoscope.router,     prefix=PREFIX, tags=["Jyotish"])
app.include_router(chat.router,          prefix=PREFIX, tags=["RAG"])
app.include_router(search.router,        prefix=PREFIX, tags=["RAG"])
app.include_router(user.router,          prefix=PREFIX, tags=["User"])
app.include_router(palmistry.router,     prefix=PREFIX, tags=["Palmistry"])
app.include_router(gochar.router,        prefix=PREFIX, tags=["Jyotish"])
app.include_router(rectification.router, prefix=PREFIX, tags=["Jyotish"])
app.include_router(prashna.router,      prefix=PREFIX, tags=["Jyotish"])
app.include_router(varshphal.router,    prefix=PREFIX, tags=["Jyotish"])
app.include_router(kp.router,           prefix=PREFIX, tags=["Jyotish"])
app.include_router(admin.router,        prefix=PREFIX, tags=["Admin"])
app.include_router(auth.router,         prefix=PREFIX, tags=["Auth"])


@app.get("/health")
def health():
    return {
        "status": "ok",
        "rag_loaded": bool(G.get("loaded")),
        "docs": len(G.get("docs", {})),
    }
