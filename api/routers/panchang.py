from datetime import date as _date
from fastapi import APIRouter, HTTPException, Query
from api.models.panchang import PanchangResponse
from api.services.panchang_service import get_panchang

router = APIRouter()


@router.get("/panchang", response_model=PanchangResponse)
def panchang(
    date: str = Query(default=None, description="YYYY-MM-DD, defaults to today"),
    lat: float = Query(default=23.1765),   # Ujjain — traditional Vedic meridian
    lon: float = Query(default=75.7885),
    tz: float = Query(default=5.5),
):
    if date is None:
        d = _date.today()
        year, month, day = d.year, d.month, d.day
    else:
        try:
            parts = date.split("-")
            year, month, day = int(parts[0]), int(parts[1]), int(parts[2])
        except Exception:
            raise HTTPException(status_code=422, detail="date must be YYYY-MM-DD")

    try:
        return get_panchang(year, month, day, lat, lon, tz)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
