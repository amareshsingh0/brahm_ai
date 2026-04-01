"""
Brahm AI FastAPI — Entry Point
Run: uvicorn api.main:app --host 0.0.0.0 --port 8000 --workers 1
     (--workers 1 is MANDATORY: Qwen LLM cannot be loaded in multiple processes)

API Versioning:
  /api/v1/...  — current stable version (all clients should migrate here)
  /api/...     — legacy aliases (kept for backward compat — website + Android v1.x)
  /api/admin   — admin panel (unversioned — internal tool only)

To add v2: import new routers, mount at /api/v2 only. v1 stays unchanged.
"""
import time, threading
from contextlib import asynccontextmanager
import sentry_sdk
from sentry_sdk.integrations.fastapi import FastApiIntegration
from sentry_sdk.integrations.starlette import StarletteIntegration

sentry_sdk.init(
    dsn="https://235c04d3ea8600588af7bc44fc0eec82@o4511104916389888.ingest.us.sentry.io/4511105029242880",
    integrations=[StarletteIntegration(), FastApiIntegration()],
    traces_sample_rate=0.2,   # 20% of requests tracked for performance
    send_default_pii=False,   # no personal data sent
    environment="production",
)

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from api.routers import (
    chat, kundali, panchang, compatibility,
    search, planets, muhurta, grahan,
    horoscope, user, cities, festivals, calendar, palmistry, gochar, rectification,
    prashna, varshphal, kp, admin, auth, subscriptions, marriage,
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
    docs_url="/api/docs",
    redoc_url="/api/redoc",
    openapi_url="/api/openapi.json",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # tighten in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def _supabase_connection_guard(request: Request, call_next):
    """Reset Supabase client on HTTP/2 RemoteProtocolError and return 503 so client retries."""
    try:
        return await call_next(request)
    except Exception as exc:
        if "RemoteProtocolError" in type(exc).__name__ or "ConnectionTerminated" in str(exc):
            from api.supabase_client import reset_supabase
            reset_supabase()
            from fastapi.responses import JSONResponse
            return JSONResponse(status_code=503, content={"detail": "Connection reset, please retry"})
        raise


@app.middleware("http")
async def _check_user_status(request: Request, call_next):
    """Block suspended or banned users. Supports both JWT (Bearer) and legacy session_id."""
    from fastapi.responses import JSONResponse
    path = request.url.path
    # Skip non-API, admin, and auth routes
    is_api = path.startswith("/api/")
    is_admin = path.startswith("/api/admin") or path.startswith("/api/v1/admin")
    is_auth  = path.startswith("/api/auth")  or path.startswith("/api/v1/auth")
    if not is_api or is_admin or is_auth:
        return await call_next(request)

    user_id = None
    try:
        from api.supabase_client import get_supabase
        import os
        from jose import jwt as _jwt, JWTError

        # Try JWT Bearer token first
        auth_header = request.headers.get("Authorization", "")
        if auth_header.startswith("Bearer "):
            token = auth_header[7:]
            secret = os.getenv("JWT_SECRET", "")
            if secret:
                try:
                    payload = _jwt.decode(token, secret, algorithms=["HS256"])
                    user_id = payload.get("sub")
                except JWTError:
                    pass

        # Fallback: legacy session_id query param
        if not user_id:
            sid = request.query_params.get("session_id", "")
            if sid:
                sb = get_supabase()
                row = sb.table("users").select("id,status,role").eq("session_id", sid).maybe_single().execute()
                if row.data:
                    user_id = row.data.get("id")
                    status  = row.data.get("status", "active")
                    role    = row.data.get("role", "user")
                    if status == "suspended":
                        return JSONResponse(status_code=403, content={"detail": "Account suspended. Contact support."})
                    if role == "banned":
                        return JSONResponse(status_code=403, content={"detail": "Account banned."})

        # Check JWT user status
        if user_id:
            sb = get_supabase()
            row = sb.table("users").select("status,role").eq("id", user_id).maybe_single().execute()
            if row.data:
                status = row.data.get("status", "active")
                role   = row.data.get("role", "user")
                if status == "suspended":
                    return JSONResponse(status_code=403, content={"detail": "Account suspended. Contact support."})
                if role == "banned":
                    return JSONResponse(status_code=403, content={"detail": "Account banned."})
    except Exception:
        pass

    return await call_next(request)


@app.middleware("http")
async def _activity_log(request: Request, call_next):
    """Log every /api/ request (non-admin) asynchronously."""
    path = request.url.path
    if path.startswith("/api/") and not path.startswith("/api/admin") and not path.startswith("/api/v1/admin"):
        # Try JWT first, fallback to session_id
        user_id = request.query_params.get("session_id", "")
        try:
            import os
            from jose import jwt as _jwt, JWTError
            auth_header = request.headers.get("Authorization", "")
            if auth_header.startswith("Bearer "):
                token = auth_header[7:]
                secret = os.getenv("JWT_SECRET", "")
                if secret:
                    payload = _jwt.decode(token, secret, algorithms=["HS256"])
                    user_id = payload.get("sub", user_id)
        except Exception:
            pass

        method = request.method
        client = request.headers.get("X-Client", "unknown")
        start  = time.time()
        response = await call_next(request)
        duration_ms = int((time.time() - start) * 1000)
        threading.Thread(
            target=admin.log_request,
            args=(method, path, user_id, response.status_code, duration_ms, client),
            daemon=True,
        ).start()
        return response
    return await call_next(request)

# ── Versioned routers (current stable: v1) ───────────────────────────────────
# All new clients (website v2+, Android v2+) should call /api/v1/...
# Admin panel and auth are unversioned (internal / auth flows don't need versioning)

V1 = "/api/v1"

_JYOTISH_ROUTERS = [
    (cities.router,        "Reference"),
    (kundali.router,       "Jyotish"),
    (panchang.router,      "Jyotish"),
    (compatibility.router, "Jyotish"),
    (planets.router,       "Jyotish"),
    (grahan.router,        "Jyotish"),
    (festivals.router,     "Jyotish"),
    (calendar.router,      "Jyotish"),
    (muhurta.router,       "Jyotish"),
    (horoscope.router,     "Jyotish"),
    (gochar.router,        "Jyotish"),
    (rectification.router, "Jyotish"),
    (prashna.router,       "Jyotish"),
    (varshphal.router,     "Jyotish"),
    (kp.router,            "Jyotish"),
    (marriage.router,      "Jyotish"),
    (chat.router,          "RAG"),
    (search.router,        "RAG"),
    (user.router,          "User"),
    (palmistry.router,     "Palmistry"),
    (subscriptions.router, "Subscription"),
]

# Mount at /api/v1 (current version)
for _router, _tag in _JYOTISH_ROUTERS:
    app.include_router(_router, prefix=V1, tags=[_tag])

# ── Legacy aliases /api/... — kept for backward compat ───────────────────────
# Existing website + Android v1.x clients call /api/... — keep working forever.
# Do NOT remove these. When a future Android/web version ships with /api/v1,
# these aliases can be deprecated (log a warning) and eventually removed.

LEGACY = "/api"

for _router, _tag in _JYOTISH_ROUTERS:
    app.include_router(_router, prefix=LEGACY, tags=[f"{_tag} (legacy)"])

# ── Admin + Auth — always unversioned ────────────────────────────────────────
app.include_router(admin.router, prefix="/api", tags=["Admin"])
app.include_router(auth.router,  prefix="/api", tags=["Auth"])


@app.get("/health")
def health():
    return {
        "status": "ok",
        "rag_loaded": bool(G.get("loaded")),
        "docs": len(G.get("docs", {})),
    }
