from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import JSONResponse
from api.services.horoscope_service import get_horoscope

router = APIRouter()

VALID_RASHIS = {
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
}


@router.get("/horoscope/{rashi}")
def horoscope(rashi: str, period: str = Query(default="daily")):
    # Normalize capitalization
    rashi = rashi.capitalize() if rashi.lower() not in {r.lower() for r in VALID_RASHIS} else next(
        r for r in VALID_RASHIS if r.lower() == rashi.lower()
    )
    try:
        result = get_horoscope(rashi, period)
        # Horoscope is daily — cache for 1 hour (browser/CDN)
        return JSONResponse(content=result, headers={"Cache-Control": "public, max-age=3600"})
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
