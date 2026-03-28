"""
User profile router — JWT-based (Bearer token), with legacy session_id fallback.
"""
import os
from typing import Optional
from fastapi import APIRouter, HTTPException, Query, Request
from pydantic import BaseModel
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
        try:
            sb = get_supabase()
            res = sb.table("users").select("id").eq("session_id", session_id).limit(1).execute()
            rows = res.data if res and isinstance(res.data, list) else []
            if rows:
                return rows[0]["id"]
        except Exception:
            pass
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

def _ensure_meta_table(sb):
    """No-op — table must exist in Supabase. Silently skip if missing."""
    pass


@router.get("/user/chats/sessions")
def get_chat_sessions(
    request: Request,
    session_id: str = Query(default=""),
    include_archived: bool = Query(default=False),
):
    """Return user's chat sessions grouped by session_id + page_context.
    Includes is_pinned, is_archived, custom_name from chat_session_meta.
    Pinned sessions come first; archived sessions excluded unless include_archived=True.
    """
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

        # Load session metadata
        meta_rows = []
        try:
            meta_rows = sb.table("chat_session_meta") \
                .select("session_id, is_pinned, is_archived, custom_name") \
                .eq("user_id", user_id) \
                .execute().data or []
        except Exception:
            pass
        meta_map = {r["session_id"]: r for r in meta_rows}

        # Group by (session_id, page_context)
        sessions: dict = {}
        for r in rows:
            key = (r.get("session_id") or "", r.get("page_context", "general"))
            if key not in sessions:
                meta = meta_map.get(key[0], {})
                sessions[key] = {
                    "session_id":   key[0],
                    "page_context": key[1],
                    "last_at":      r["created_at"],
                    "is_pinned":    meta.get("is_pinned", False),
                    "is_archived":  meta.get("is_archived", False),
                    "custom_name":  meta.get("custom_name"),
                    "messages": [],
                }
            sessions[key]["messages"].append({
                "role":       r["role"],
                "content":    r["content"],
                "created_at": r["created_at"],
            })

        all_sessions = list(sessions.values())

        # Filter archived
        if not include_archived:
            all_sessions = [s for s in all_sessions if not s.get("is_archived")]
        else:
            all_sessions = [s for s in all_sessions if s.get("is_archived")]

        # Sort: pinned first, then by last_at desc
        result = sorted(
            all_sessions,
            key=lambda x: (0 if x.get("is_pinned") else 1, x["last_at"]),
            reverse=False,
        )
        # Fix: pinned first (ascending by pin=0/1), but last_at should be descending within each group
        pinned   = sorted([s for s in result if     s.get("is_pinned")], key=lambda x: x["last_at"], reverse=True)
        unpinned = sorted([s for s in result if not s.get("is_pinned")], key=lambda x: x["last_at"], reverse=True)
        return {"sessions": pinned + unpinned}

    except Exception as e:
        raise HTTPException(500, f"Chat history error: {e}")


class SessionMetaUpdate(BaseModel):
    action: str          # "pin" | "unpin" | "archive" | "unarchive" | "rename"
    name: Optional[str] = None

@router.patch("/user/chats/session/{sess_id}/meta")
def update_session_meta(
    sess_id: str,
    body: SessionMetaUpdate,
    request: Request,
    session_id: str = Query(default=""),
):
    """Pin, unpin, archive, unarchive, or rename a chat session."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    if not sb:
        raise HTTPException(500, "Database unavailable")

    update: dict = {"user_id": user_id, "session_id": sess_id}

    if body.action == "pin":
        update["is_pinned"] = True
    elif body.action == "unpin":
        update["is_pinned"] = False
    elif body.action == "archive":
        update["is_archived"] = True
        update["is_pinned"]   = False   # unpin when archiving
    elif body.action == "unarchive":
        update["is_archived"] = False
    elif body.action == "rename":
        if not body.name:
            raise HTTPException(400, "name required for rename")
        update["custom_name"] = body.name.strip()[:100]
    else:
        raise HTTPException(400, f"Unknown action: {body.action}")

    try:
        # Upsert by (user_id, session_id)
        sb.table("chat_session_meta").upsert(update, on_conflict="user_id,session_id").execute()
        return {"ok": True}
    except Exception as e:
        raise HTTPException(500, f"Meta update error: {e}")


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


# ─── Saved Kundali ────────────────────────────────────────────────────────────

class SaveKundaliRequest(BaseModel):
    name: str = ""
    birth_date: str
    birth_time: str
    birth_lat: float
    birth_lon: float
    birth_tz: float = 5.5
    birth_place: str = ""
    kundali_json: str  # JSON stringified kundali data


@router.get("/user/kundali")
def get_saved_kundali(request: Request, session_id: str = Query(default="")):
    """Fetch the user's primary saved kundali (most recent)."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    try:
        sb = get_supabase()
        res = (
            sb.table("saved_kundalis")
            .select("*")
            .eq("user_id", user_id)
            .order("created_at", desc=True)
            .limit(1)
            .execute()
        )
        rows = res.data if res and isinstance(res.data, list) else []
        if not rows:
            return {"found": False, "kundali": None}
        return {"found": True, "kundali": rows[0]}
    except Exception as e:
        import logging
        logging.getLogger(__name__).warning("get_saved_kundali error user=%s: %s", user_id, e)
        return {"found": False, "kundali": None}


@router.post("/user/kundali")
def save_kundali(body: SaveKundaliRequest, request: Request, session_id: str = Query(default="")):
    """Upsert the user's primary kundali (one per user — overwrites on re-generate)."""
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    # Check if already exists — extract id safely regardless of client version
    existing_id = None
    try:
        resp = (
            sb.table("saved_kundalis")
            .select("id")
            .eq("user_id", user_id)
            .limit(1)
            .maybe_single()
            .execute()
        )
        if resp and resp.data:
            d = resp.data
            existing_id = d.get("id") if isinstance(d, dict) else None
    except Exception:
        existing_id = None

    row = {
        "user_id": user_id,
        "label": body.name or "My Chart",
        "birth_date": body.birth_date,
        "birth_time": body.birth_time,
        "birth_lat": body.birth_lat,
        "birth_lon": body.birth_lon,
        "birth_tz": body.birth_tz,
        "birth_place": body.birth_place,
        "birth_city": body.birth_place,
        "kundali_json": body.kundali_json,
    }

    try:
        if existing_id:
            sb.table("saved_kundalis").update(row).eq("id", existing_id).execute()
        else:
            sb.table("saved_kundalis").insert(row).execute()
        return {"saved": True}
    except Exception as e:
        raise HTTPException(500, f"Failed to save kundali: {e}")
