from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from api.services.kp_service import calc_kp

router = APIRouter()


class KPRequest(BaseModel):
    date:  str    # YYYY-MM-DD
    time:  str    # HH:MM
    lat:   float
    lon:   float
    tz:    float = 5.5
    name:  Optional[str] = ""


@router.post("/kp")
def kp_chart(req: KPRequest):
    from datetime import datetime
    try:
        d = datetime.strptime(req.date, "%Y-%m-%d")
        t = datetime.strptime(req.time, "%H:%M")
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    result, err = calc_kp(
        d.year, d.month, d.day, t.hour, t.minute,
        req.lat, req.lon, req.tz, req.name or "",
    )
    if err:
        raise HTTPException(status_code=500, detail=err)
    return result
