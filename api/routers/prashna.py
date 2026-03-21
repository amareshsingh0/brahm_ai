from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from api.services.prashna_service import calc_prashna

router = APIRouter()


class PrashnaRequest(BaseModel):
    lat:           float
    lon:           float
    tz:            float = 5.5
    question:      Optional[str] = ""
    question_type: Optional[str] = "general"   # wealth|career|relationship|health|property|travel|education|spiritual|children|general
    ayanamsha:     Optional[str] = "lahiri"


@router.post("/prashna")
def prashna(req: PrashnaRequest):
    chart, err = calc_prashna(
        lat=req.lat, lon=req.lon, tz=req.tz,
        question=req.question or "",
        question_type=req.question_type or "general",
        ayanamsha_mode=req.ayanamsha or "lahiri",
    )
    if err:
        raise HTTPException(status_code=500, detail=err)
    return chart
