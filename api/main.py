"""
Brahm AI FastAPI — Entry Point
Run: uvicorn api.main:app --host 0.0.0.0 --port 8000 --workers 1
     (--workers 1 is MANDATORY: Qwen LLM cannot be loaded in multiple processes)
"""
import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routers import (
    chat, kundali, panchang, compatibility,
    search, planets, muhurta, grahan,
    horoscope, user, cities, festivals, calendar, palmistry,
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


@app.get("/health")
def health():
    return {
        "status": "ok",
        "rag_loaded": bool(G.get("loaded")),
        "docs": len(G.get("docs", {})),
    }
