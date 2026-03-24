"""
User profile router — JWT-based (Bearer token), with legacy session_id fallback.
"""
import os
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
        rashi=row.get("rashi", ""),
        nakshatra=row.get("nakshatra", ""),
        language=row.get("language", "english"),
    )


@router.post("/user", response_model=UserProfile)
def upsert_user(profile: UserProfile, request: Request, session_id: str = Query(default="")):
    user_id = _get_user_id(request, session_id)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    sb = get_supabase()
    sb.table("users").update({
        "name":        profile.name,
        "birth_date":  profile.date,
        "birth_time":  profile.time,
        "birth_lat":   profile.lat,
        "birth_lon":   profile.lon,
        "birth_tz":    profile.tz,
        "birth_place": profile.place,
        "rashi":       profile.rashi,
        "nakshatra":   profile.nakshatra,
        "language":    profile.language,
    }).eq("id", user_id).execute()
    return profile
