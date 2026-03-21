from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime
from api.services.rectification_service import rectify_birth_time

router = APIRouter()


class LifeEvent(BaseModel):
    date: str           # YYYY-MM-DD
    type: str           # marriage, child, career_start, etc.
    description: Optional[str] = ""


class RectificationRequest(BaseModel):
    date: str           # YYYY-MM-DD birth date
    approx_time: str    # HH:MM
    uncertainty_minutes: int = 60
    lat: float
    lon: float
    tz: float = 5.5
    ayanamsha: str = "lahiri"
    events: List[LifeEvent] = []


class RectificationResponse(BaseModel):
    candidates: List[Dict[str, Any]]
    approx_time: str
    uncertainty_minutes: int
    event_count: int


@router.post("/rectification", response_model=RectificationResponse)
def rectify(req: RectificationRequest):
    try:
        dt = datetime.strptime(f"{req.date} {req.approx_time}", "%Y-%m-%d %H:%M")
    except ValueError as e:
        raise HTTPException(status_code=422, detail=f"Invalid date/time: {e}")

    try:
        candidates = rectify_birth_time(
            year=dt.year, month=dt.month, day=dt.day,
            approx_hour=dt.hour, approx_minute=dt.minute,
            lat=req.lat, lon=req.lon, tz=req.tz,
            uncertainty_minutes=min(req.uncertainty_minutes, 180),
            events=[e.dict() for e in req.events],
            ayanamsha_mode=req.ayanamsha,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    return {
        "candidates": candidates,
        "approx_time": req.approx_time,
        "uncertainty_minutes": req.uncertainty_minutes,
        "event_count": len(req.events),
    }
