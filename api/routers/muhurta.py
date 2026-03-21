from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import Optional
from api.services.muhurta_service import get_muhurta, find_activity_muhurta

router = APIRouter()


@router.get("/muhurta")
def muhurta(
    event: str = Query(default="general"),
    date: str = Query(default=None),
    lat: float = Query(default=28.6139),
    lon: float = Query(default=77.209),
    tz: float = Query(default=5.5),
):
    from datetime import date as _d
    if date is None:
        date = _d.today().strftime("%Y-%m-%d")
    return get_muhurta(event, date, lat, lon, tz)


class ActivityMuhurtaRequest(BaseModel):
    activity:   str            # marriage|travel|business|medical|education|property|general
    start_date: Optional[str] = None   # YYYY-MM-DD, default today
    days:       Optional[int] = 7
    lat:        float
    lon:        float
    tz:         float = 5.5


@router.post("/muhurta/activity")
def activity_muhurta(req: ActivityMuhurtaRequest):
    from datetime import date as _d
    start = req.start_date or _d.today().strftime("%Y-%m-%d")
    slots = find_activity_muhurta(
        req.activity, start, min(req.days or 7, 14), req.lat, req.lon, req.tz
    )
    return {"activity": req.activity, "start_date": start, "days": req.days, "slots": slots}
