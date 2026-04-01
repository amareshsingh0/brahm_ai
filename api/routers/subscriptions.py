"""
Subscriptions router — Brahm AI
Exposes user-facing subscription endpoints.
Auth: JWT Bearer token (same pattern as user.py), with legacy session_id fallback.
"""
import os
from fastapi import APIRouter, HTTPException, Query, Request
from api.supabase_client import get_supabase

router = APIRouter()


# ─── Auth helper (copied from user.py) ───────────────────────────────────────

def _get_user_id(request: Request, session_id: str = "") -> str | None:
    """Extract user_id from JWT Bearer token or fallback to session_id lookup."""
    auth = request.headers.get("Authorization", "")
    if auth.startswith("Bearer "):
        try:
            from jose import jwt, JWTError
            secret = os.getenv("JWT_SECRET", "")
            payload = jwt.decode(auth[7:], secret, algorithms=["HS256"])
            return payload.get("sub")
        except Exception:
            pass
    # Legacy fallback
    if session_id:
        try:
            sb = get_supabase()
            res = sb.table("users").select("id").eq("session_id", session_id).limit(1).execute()
            rows = res.data if res and isinstance(res.data, list) else []
            if rows:
                return rows[0]["id"]
        except Exception:
            pass
    return None


# ─── Endpoints ────────────────────────────────────────────────────────────────

@router.get("/subscription")
async def get_my_subscription(
    request: Request,
    session_id: str = Query(default=""),
):
    """
    Return the authenticated user's current plan, usage stats, and features.

    Response shape:
    {
        plan_id, plan_name, price_inr,
        daily_message_limit, daily_token_limit,
        badge_text, status, expires_at, days_remaining,
        messages_used_today, tokens_used_today,
        features: list[str],
        is_free: bool
    }
    """
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    from api.services.subscription_service import get_user_subscription
    try:
        data = await get_user_subscription(user_id)
        return data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Subscription lookup failed: {e}")
