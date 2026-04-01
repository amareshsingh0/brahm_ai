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

_FEATURE_LABELS: dict[str, str] = {
    "ai_chat":       "AI Chat Assistant",
    "kundali":       "Kundali Chart & Analysis",
    "palm_reading":  "Palm Reading AI",
    "live_sky_today":"Live Sky Today",
    "muhurta":       "Muhurta Calculator",
    "horoscope_ai":  "AI Horoscope",
    "compatibility": "Kundali Compatibility",
    "gochar":        "Gochar Transit",
    "kp_system":     "KP System",
    "dosha":         "Dosha Analysis",
    "gemstone":      "Gemstone Recommendations",
    "nakshatra":     "Nakshatra Analysis",
    "prashna":       "Prashna Kundali",
    "varshphal":     "Varshphal",
    "rectification": "Birth Time Rectification",
    "pdf_export":    "PDF Export",
    "chat_history":  "Chat History",
    "panchang":      "Daily Panchang",
}

_NAME_HI: dict[str, str] = {
    "free":  "मुफ्त",
    "basic": "बेसिक",
    "pro":   "प्रो",
}


@router.get("/plans")
async def get_public_plans():
    """Return all active subscription plans for the pricing page (no auth required)."""
    try:
        sb = get_supabase()
        res = sb.table("subscription_plans") \
            .select("*") \
            .eq("is_active", True) \
            .order("sort_order") \
            .execute()
        rows = res.data if res and isinstance(res.data, list) else []
        plans = []
        for p in rows:
            keys: list[str] = p.get("features") or []
            price_monthly = p.get("price_inr") or 0
            price_yearly  = round(price_monthly * 10)   # 2 months free
            display_features: list[str] = []
            if price_monthly == 0:
                display_features = ["Daily Panchang"]
            else:
                display_features = ["Everything in Free:"] + [
                    _FEATURE_LABELS.get(k, k) for k in keys if k != "panchang"
                ]
                if p.get("daily_message_limit"):
                    display_features.insert(1, f"{p['daily_message_limit']} AI messages / day")
            plans.append({
                "id":             p["id"],
                "name":           p["name"],
                "name_hi":        _NAME_HI.get(p["id"], ""),
                "description":    p.get("description", ""),
                "price_monthly":  price_monthly,
                "price_yearly":   price_yearly,
                "badge_text":     p.get("badge_text"),
                "daily_message_limit": p.get("daily_message_limit"),
                "features":       display_features,
            })
        return plans
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Plans lookup failed: {e}")


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
