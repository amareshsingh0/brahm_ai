"""
Gochar Router — Current planetary transits.

GET  /api/gochar          → current positions of all 9 grahas (no auth needed)
POST /api/gochar/analyze  → personal transit analysis over natal chart
"""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from api.services.gochar_service import get_current_planet_positions, get_transits

router = APIRouter()


class GocharRequest(BaseModel):
    lagna_rashi: str        # e.g. "Kanya"
    moon_rashi:  str        # e.g. "Mithun"
    name:        Optional[str] = ""


@router.get("/gochar")
def current_positions():
    """
    Current sidereal positions of all 9 grahas.
    No birth data required.
    """
    try:
        positions = get_current_planet_positions()
        # Format for frontend display
        formatted = {}
        for planet, data in positions.items():
            formatted[planet] = {
                "rashi":  data["rashi"],
                "degree": data["degree"],
            }
        return {"status": "ok", "positions": formatted}
    except Exception as e:
        return {"status": "error", "detail": str(e)}


@router.post("/gochar/analyze")
def analyze_transits(req: GocharRequest):
    """
    Personal transit analysis — which natal house each planet is transiting,
    opportunities, cautions, sade sati.
    """
    try:
        result = get_transits(req.lagna_rashi, req.moon_rashi)
        return {"status": "ok", **result}
    except Exception as e:
        return {"status": "error", "detail": str(e)}
