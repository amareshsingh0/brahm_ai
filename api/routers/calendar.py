from fastapi import APIRouter, Query
from api.services.calendar_service import get_monthly_calendar
from api.services.festival_service import TRADITION_RULES
import datetime

router = APIRouter()

_VALID_TRADITIONS = list(TRADITION_RULES.keys())
_VALID_LUNAR      = ["amanta", "purnimanta"]


@router.get("/calendar/month")
async def monthly_calendar(
    year:         int   = Query(default=2026,    ge=1800, le=2200),
    month:        int   = Query(default=None),
    lat:          float = Query(default=28.6139),
    lon:          float = Query(default=77.209),
    tz:           float = Query(default=5.5),
    tradition:    str   = Query(default="smarta"),
    lunar_system: str   = Query(default="amanta"),
):
    if month is None:
        month = datetime.date.today().month
    month = max(1, min(12, month))
    if tradition not in _VALID_TRADITIONS:
        tradition = "smarta"
    if lunar_system not in _VALID_LUNAR:
        lunar_system = "amanta"
    try:
        data = get_monthly_calendar(
            year, month, lat, lon, tz,
            tradition=tradition, lunar_system=lunar_system,
        )
        return data
    except Exception as exc:
        import traceback
        return {
            "error": str(exc),
            "detail": traceback.format_exc(),
            "year": year, "month": month,
            "month_name": "", "days_in_month": 0,
            "first_weekday": 0, "days": [],
        }
