"""
Subscription Service — Brahm AI
Handles plan lookups, feature flag checks, and daily usage tracking.

All functions are synchronous (matching existing project pattern) but
named with `async def` for FastAPI compatibility.
"""
from datetime import datetime, timezone, timedelta
from typing import Optional


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _get_sb():
    from api.supabase_client import get_supabase
    return get_supabase()


def _today_ist() -> str:
    """Return today's date string in IST (UTC+5:30)."""
    ist_offset = timedelta(hours=5, minutes=30)
    return (datetime.now(timezone.utc) + ist_offset).date().isoformat()


def _tomorrow_ist_midnight() -> str:
    """Return ISO timestamp of tomorrow's midnight in IST."""
    ist_offset = timedelta(hours=5, minutes=30)
    now_ist = datetime.now(timezone.utc) + ist_offset
    tomorrow = (now_ist + timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)
    return tomorrow.strftime("%Y-%m-%dT00:00:00+05:30")


# ─── Plan helpers ─────────────────────────────────────────────────────────────

async def get_plan(plan_id: str) -> Optional[dict]:
    """Fetch a single plan from subscription_plans table by id."""
    try:
        sb = _get_sb()
        if not sb:
            return None
        res = sb.table("subscription_plans").select("*").eq("id", plan_id).maybe_single().execute()
        return res.data if res and res.data else None
    except Exception:
        return None


async def get_all_plans() -> list:
    """All active plans ordered by sort_order."""
    try:
        sb = _get_sb()
        if not sb:
            return []
        res = sb.table("subscription_plans").select("*").eq("is_active", True).order("sort_order").execute()
        return res.data if res and res.data else []
    except Exception:
        return []


async def get_all_feature_flags() -> list:
    """All feature flags."""
    try:
        sb = _get_sb()
        if not sb:
            return []
        res = sb.table("feature_flags").select("*").order("category").order("key").execute()
        return res.data if res and res.data else []
    except Exception:
        return []


# ─── Usage ────────────────────────────────────────────────────────────────────

async def get_usage_today(user_id: str) -> dict:
    """Return { messages_used, tokens_used, date } for today (IST)."""
    today = _today_ist()
    try:
        sb = _get_sb()
        if not sb:
            return {"messages_used": 0, "tokens_used": 0, "date": today}
        res = (
            sb.table("daily_usage")
            .select("messages_used, tokens_used, usage_date")
            .eq("user_id", user_id)
            .eq("usage_date", today)
            .maybe_single()
            .execute()
        )
        if res and res.data:
            return {
                "messages_used": res.data.get("messages_used", 0),
                "tokens_used":   res.data.get("tokens_used", 0),
                "date":          res.data.get("usage_date", today),
            }
    except Exception:
        pass
    return {"messages_used": 0, "tokens_used": 0, "date": today}


# ─── Core: get_user_subscription ──────────────────────────────────────────────

async def get_user_subscription(user_id: str) -> dict:
    """
    Return full subscription info for a user.

    Shape:
    {
        plan_id, plan_name, price_inr, daily_message_limit, daily_token_limit,
        badge_text, status, expires_at, days_remaining,
        messages_used_today, tokens_used_today, features: list[str], is_free: bool
    }
    """
    sb = _get_sb()

    # ── 1. Look up active subscription ────────────────────────────────────────
    active_sub = None
    plan_id_from_sub = None
    expires_at = None
    days_remaining = None
    sub_status = "none"

    if sb:
        try:
            res = (
                sb.table("subscriptions")
                .select("plan, status, expires_at")
                .eq("user_id", user_id)
                .eq("status", "active")
                .order("started_at", desc=True)
                .limit(1)
                .execute()
            )
            rows = res.data if res and isinstance(res.data, list) else []
            if rows:
                active_sub = rows[0]
                plan_id_from_sub = active_sub.get("plan")
                sub_status = active_sub.get("status", "active")
                expires_at = active_sub.get("expires_at")
                if expires_at:
                    try:
                        exp_dt = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
                        days_remaining = max(0, (exp_dt - datetime.now(timezone.utc)).days)
                    except Exception:
                        days_remaining = None
        except Exception:
            pass

    # ── 2. Look up the plan details ────────────────────────────────────────────
    plan_id = plan_id_from_sub or "free"
    plan = await get_plan(plan_id)

    # If the plan doesn't exist or is inactive, fall back to free
    if not plan or not plan.get("is_active", True):
        plan_id = "free"
        plan = await get_plan("free")
        sub_status = "none"
        expires_at = None
        days_remaining = None

    # Ultimate fallback if DB is down
    if not plan:
        plan = {
            "id": "free",
            "name": "Free",
            "price_inr": 0,
            "duration_days": 0,
            "daily_message_limit": 0,
            "daily_token_limit": 0,
            "features": ["panchang"],
            "badge_text": None,
        }

    features = plan.get("features") or []
    if isinstance(features, str):
        import json as _json
        try:
            features = _json.loads(features)
        except Exception:
            features = []

    # ── 3. Usage today ─────────────────────────────────────────────────────────
    usage = await get_usage_today(user_id)

    return {
        "plan_id":               plan.get("id", plan_id),
        "plan_name":             plan.get("name", "Free"),
        "price_inr":             plan.get("price_inr", 0),
        "daily_message_limit":   plan.get("daily_message_limit", 0),
        "daily_token_limit":     plan.get("daily_token_limit", 0),
        "badge_text":            plan.get("badge_text"),
        "status":                sub_status,
        "expires_at":            expires_at,
        "days_remaining":        days_remaining,
        "messages_used_today":   usage["messages_used"],
        "tokens_used_today":     usage["tokens_used"],
        "features":              features,
        "is_free":               plan_id == "free",
    }


# ─── Feature access check ─────────────────────────────────────────────────────

async def check_feature_access(user_id: str, feature_key: str) -> tuple:
    """
    Check whether a user can access a given feature.

    Returns (allowed: bool, error_payload: dict).
    Checks:
      1. Feature is globally enabled in feature_flags table.
      2. User has an active subscription whose plan includes the feature.
    """
    sb = _get_sb()
    if not sb:
        # If DB is down, allow access (fail open)
        return True, {}

    # ── Check global feature flag ─────────────────────────────────────────────
    try:
        flag_res = (
            sb.table("feature_flags")
            .select("is_globally_enabled")
            .eq("key", feature_key)
            .maybe_single()
            .execute()
        )
        if flag_res and flag_res.data:
            if not flag_res.data.get("is_globally_enabled", True):
                return False, {
                    "error": "feature_disabled",
                    "feature": feature_key,
                    "message": "This feature is currently unavailable.",
                }
    except Exception:
        pass  # Fail open on flag lookup error

    # ── Check subscription + plan features ────────────────────────────────────
    sub_info = await get_user_subscription(user_id)
    features = sub_info.get("features", [])

    if feature_key not in features:
        return False, {
            "error": "feature_not_in_plan",
            "feature": feature_key,
            "plan_id": sub_info.get("plan_id"),
            "plan_name": sub_info.get("plan_name"),
            "message": f"'{feature_key}' is not included in your {sub_info.get('plan_name', 'current')} plan.",
        }

    return True, {}


# ─── Daily usage check + increment ───────────────────────────────────────────

async def check_and_increment_usage(user_id: str, tokens_used: int = 0) -> tuple:
    """
    Check daily message and token limits; if within limits, increment counters.

    Returns (allowed: bool, error_payload: dict).
    """
    sub_info = await get_user_subscription(user_id)
    msg_limit   = sub_info.get("daily_message_limit", 0)
    token_limit = sub_info.get("daily_token_limit", 0)
    msgs_used   = sub_info.get("messages_used_today", 0)
    tokens_used_today = sub_info.get("tokens_used_today", 0)

    # 0 means unlimited (e.g. admin-granted uncapped plan)
    if msg_limit > 0 and msgs_used >= msg_limit:
        return False, {
            "error":          "daily_limit_reached",
            "messages_used":  msgs_used,
            "messages_limit": msg_limit,
            "reset_at":       _tomorrow_ist_midnight(),
        }

    if token_limit > 0 and tokens_used_today >= token_limit:
        return False, {
            "error":         "daily_token_limit_reached",
            "tokens_used":   tokens_used_today,
            "tokens_limit":  token_limit,
            "reset_at":      _tomorrow_ist_midnight(),
        }

    # ── Increment usage ────────────────────────────────────────────────────────
    sb = _get_sb()
    if sb:
        today = _today_ist()
        try:
            # Upsert: increment messages_used by 1, tokens_used by provided amount
            existing = (
                sb.table("daily_usage")
                .select("id, messages_used, tokens_used")
                .eq("user_id", user_id)
                .eq("usage_date", today)
                .maybe_single()
                .execute()
            )
            if existing and existing.data:
                row_id = existing.data["id"]
                new_msgs   = existing.data.get("messages_used", 0) + 1
                new_tokens = existing.data.get("tokens_used", 0) + tokens_used
                sb.table("daily_usage").update({
                    "messages_used": new_msgs,
                    "tokens_used":   new_tokens,
                }).eq("id", row_id).execute()
            else:
                sb.table("daily_usage").insert({
                    "user_id":       user_id,
                    "usage_date":    today,
                    "messages_used": 1,
                    "tokens_used":   max(0, tokens_used),
                    "plan_id":       sub_info.get("plan_id", "free"),
                }).execute()
        except Exception:
            pass  # Fail open on usage write error

    return True, {}
