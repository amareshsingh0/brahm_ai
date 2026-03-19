from fastapi import APIRouter, Query
from api.services.festival_service import get_festival_calendar, TRADITION_RULES

router = APIRouter()

_VALID_TRADITIONS    = list(TRADITION_RULES.keys())
_VALID_LUNAR_SYSTEMS = ["amanta", "purnimanta"]


@router.get("/festivals")
async def festivals(
    year:         int   = Query(default=2026),
    lat:          float = Query(default=28.6139),
    lon:          float = Query(default=77.209),
    tz:           float = Query(default=5.5),
    tradition:    str   = Query(default="smarta",  description=f"Tradition: {', '.join(list(TRADITION_RULES.keys()))}"),
    lunar_system: str   = Query(default="amanta",  description="lunar_system: amanta | purnimanta"),
):
    if tradition not in _VALID_TRADITIONS:
        tradition = "smarta"
    if lunar_system not in _VALID_LUNAR_SYSTEMS:
        lunar_system = "amanta"
    try:
        items = get_festival_calendar(year, lat, lon, tz, tradition=tradition, lunar_system=lunar_system)
        return {
            "year": year,
            "tradition": tradition,
            "tradition_desc": TRADITION_RULES[tradition]["description"],
            "lunar_system": lunar_system,
            "festivals": items,
        }
    except Exception as exc:
        import traceback
        return {"year": year, "festivals": [], "error": str(exc),
                "detail": traceback.format_exc()}


@router.get("/festivals/traditions")
async def list_traditions():
    """List all supported traditions with descriptions."""
    return {
        "traditions": [
            {"key": k, "description": v["description"]}
            for k, v in TRADITION_RULES.items()
        ],
        "lunar_systems": [
            {"key": "amanta",     "description": "Month ends at Amavasya (South India, default)"},
            {"key": "purnimanta", "description": "Month ends at Purnima (North India: UP, Bihar, Rajasthan)"},
        ],
    }
