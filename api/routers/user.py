"""
User profile router — JWT-based (Bearer token), with legacy session_id fallback.
"""
import os
from typing import Optional
from fastapi import APIRouter, HTTPException, Query, Request
from api.models.user import UserProfile
from api.supabase_client import get_supabase

router = APIRouter()


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
        sb = get_supabase()
        row = sb.table("users").select("id").eq("session_id", session_id).maybe_single().execute()
        if row.data:
            return row.data["id"]
    return None


@router.get("/user", response_model=UserProfile)
def get_user(request: Request, session_id: str = Query(default="")):
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    res = sb.table("users").select("*").eq("id", user_id).maybe_single().execute()
    if not res.data:
        raise HTTPException(status_code=404, detail="User not found")

    row = res.data
    return UserProfile(
        session_id=row.get("session_id") or row["id"],
        name=row.get("name", ""),
        date=row.get("birth_date", ""),
        time=row.get("birth_time", ""),
        lat=row.get("birth_lat", 0.0),
        lon=row.get("birth_lon", 0.0),
        tz=row.get("birth_tz", 5.5),
        place=row.get("birth_place", ""),
        gender=row.get("gender", ""),
        rashi=row.get("rashi", ""),
        nakshatra=row.get("nakshatra", ""),
        language=row.get("language", "english"),
        plan=row.get("plan", "free"),
        phone=row.get("phone"),
        email=row.get("email"),
    )


@router.post("/user", response_model=UserProfile)
def upsert_user(profile: UserProfile, request: Request, session_id: str = Query(default="")):
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    try:
        sb.table("users").update({
            "name":        profile.name,
            "birth_date":  profile.date,
            "birth_time":  profile.time,
            "birth_lat":   profile.lat,
            "birth_lon":   profile.lon,
            "birth_tz":    profile.tz,
            "birth_place": profile.place,
            "gender":      profile.gender,
            "rashi":       profile.rashi,
            "nakshatra":   profile.nakshatra,
            "language":    profile.language,
        }).eq("id", user_id).execute()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Profile update failed: {e}")
    return profile


# ── Chat History ──────────────────────────────────────────────────────────────

@router.get("/user/chats/sessions")
def get_chat_sessions(request: Request, session_id: str = Query(default="")):
    """Return user's chat sessions grouped by session_id + page_context."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    if not sb:
        return {"sessions": []}
    try:
        rows = sb.table("chat_messages") \
            .select("session_id, page_context, role, content, created_at") \
            .eq("user_id", user_id) \
            .order("created_at", desc=True) \
            .limit(500) \
            .execute().data or []

        # Group by (session_id, page_context)
        sessions: dict = {}
        for r in rows:
            key = (r.get("session_id") or "", r.get("page_context", "general"))
            if key not in sessions:
                sessions[key] = {
                    "session_id": key[0],
                    "page_context": key[1],
                    "last_at": r["created_at"],
                    "messages": [],
                }
            sessions[key]["messages"].append({
                "role": r["role"],
                "content": r["content"],
                "created_at": r["created_at"],
            })

        result = sorted(sessions.values(), key=lambda x: x["last_at"], reverse=True)
        return {"sessions": result}
    except Exception as e:
        raise HTTPException(500, f"Chat history error: {e}")


@router.delete("/user/chats/session/{sess_id}")
def delete_chat_session(sess_id: str, request: Request, session_id: str = Query(default="")):
    """Delete all messages in a specific session."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    if sb:
        sb.table("chat_messages") \
            .delete() \
            .eq("user_id", user_id) \
            .eq("session_id", sess_id) \
            .execute()
    return {"deleted": True}


@router.delete("/user/chats")
def delete_all_chats(request: Request, session_id: str = Query(default=""),
                     page_context: Optional[str] = Query(default=None)):
    """Delete all (or page-specific) chat history for the authenticated user."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    if sb:
        q = sb.table("chat_messages").delete().eq("user_id", user_id)
        if page_context:
            q = q.eq("page_context", page_context)
        q.execute()
    return {"deleted": True}


@router.delete("/user/account")
def delete_account(request: Request, session_id: str = Query(default=""),
                   reason: str = Query(default="user_request")):
    """Soft-delete user account. Visible in admin for 30 days, then purged."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    if not sb:
        raise HTTPException(500, "Database unavailable")
    try:
        u = sb.table("users").select("name,phone,plan").eq("id", user_id).maybe_single().execute().data or {}
        total_chats = sb.table("chat_messages").select("id", count="exact").eq("user_id", user_id).execute().count or 0
        pay_r = sb.table("payment_log").select("id", count="exact").eq("user_id", user_id).eq("status", "SUCCESS").execute()
        sb.table("deleted_accounts").insert({
            "user_id": user_id, "phone": u.get("phone"), "name": u.get("name"),
            "plan_at_delete": u.get("plan"), "total_chats": total_chats,
            "total_payments": pay_r.count or 0, "delete_reason": reason, "deleted_by": "user",
        }).execute()
        sb.table("users").update({"status": "deleted"}).eq("id", user_id).execute()
        return {"deleted": True}
    except Exception as e:
        raise HTTPException(500, f"Account deletion failed: {e}")
