from fastapi import APIRouter, Query
from api.services.muhurta_service import get_muhurta

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
