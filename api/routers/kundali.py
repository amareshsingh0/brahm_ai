from datetime import datetime
from fastapi import APIRouter, HTTPException
from api.models.kundali import KundaliRequest, KundaliResponse
from api.services.kundali_service import calc_kundali

router = APIRouter()


@router.post("/kundali", response_model=KundaliResponse)
def calculate_kundali(req: KundaliRequest):
    try:
        dt = datetime.strptime(f"{req.date} {req.time}", "%Y-%m-%d %H:%M")
    except ValueError as e:
        raise HTTPException(status_code=422, detail=f"Invalid date/time: {e}")

    data, err = calc_kundali(
        year=dt.year, month=dt.month, day=dt.day,
        hour=dt.hour, minute=dt.minute,
        lat=req.lat, lon=req.lon, tz=req.tz,
        name=req.name, place=req.place,
    )
    if err:
        raise HTTPException(status_code=500, detail=err)
    return data
