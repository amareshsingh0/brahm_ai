from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from api.services.varshphal_service import calc_varshphal

router = APIRouter()


class VarshphalRequest(BaseModel):
    # Birth data
    birth_date:   str    # YYYY-MM-DD
    birth_time:   str    # HH:MM
    birth_lat:    float
    birth_lon:    float
    birth_tz:     float = 5.5

    # Target year for solar return
    target_year:  int

    # Current location (optional — use birth location if not provided)
    current_lat:  Optional[float] = None
    current_lon:  Optional[float] = None
    current_tz:   Optional[float] = None

    ayanamsha:    Optional[str] = "lahiri"


@router.post("/varshphal")
def varshphal(req: VarshphalRequest):
    from datetime import datetime
    try:
        bd = datetime.strptime(req.birth_date, "%Y-%m-%d")
        bt = datetime.strptime(req.birth_time, "%H:%M")
    except ValueError as e:
        raise HTTPException(status_code=422, detail=f"Invalid date/time: {e}")

    chart, err = calc_varshphal(
        birth_year=bd.year, birth_month=bd.month, birth_day=bd.day,
        birth_hour=bt.hour, birth_minute=bt.minute,
        birth_lat=req.birth_lat, birth_lon=req.birth_lon, birth_tz=req.birth_tz,
        target_year=req.target_year,
        current_lat=req.current_lat,
        current_lon=req.current_lon,
        current_tz=req.current_tz,
        ayanamsha_mode=req.ayanamsha or "lahiri",
    )
    if err:
        raise HTTPException(status_code=500, detail=err)
    return chart
