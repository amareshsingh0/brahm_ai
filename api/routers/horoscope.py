from fastapi import APIRouter, HTTPException, Query
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
        return get_horoscope(rashi, period)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
