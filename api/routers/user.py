"""
User profile router — Supabase backed, keyed by session_id.
"""
from fastapi import APIRouter, HTTPException, Query
from api.models.user import UserProfile
from api.supabase_client import get_supabase

router = APIRouter()


@router.get("/user", response_model=UserProfile)
def get_user(session_id: str = Query(...)):
    sb = get_supabase()
    res = sb.table("users").select("*").eq("session_id", session_id).maybe_single().execute()
    if not res.data:
        raise HTTPException(status_code=404, detail="User not found")
    row = res.data
    return UserProfile(
        session_id=row["session_id"],
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
def upsert_user(profile: UserProfile):
    sb = get_supabase()
    # NOTE: role, status, plan are intentionally excluded — only admin can change those.
    sb.table("users").upsert({
        "session_id":  profile.session_id,
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
    }, on_conflict="session_id").execute()
    return profile
