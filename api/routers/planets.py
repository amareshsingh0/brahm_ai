"""
Current planetary positions (sidereal, Lahiri).
"""
from fastapi import APIRouter, HTTPException, Query
from api.config import EPHE_PATH, RASHI_NAMES, NAKSHATRAS

router = APIRouter()


@router.get("/planets/now")
def planets_now(
    lat: float = Query(default=28.6139),
    lon: float = Query(default=77.209),
    tz: float = Query(default=5.5),
):
    try:
        import swisseph as swe
        from datetime import datetime, timezone
        import math

        swe.set_ephe_path(EPHE_PATH)
        swe.set_sid_mode(swe.SIDM_LAHIRI)

        now = datetime.now(timezone.utc)
        jd = swe.julday(now.year, now.month, now.day,
                         now.hour + now.minute / 60.0 + now.second / 3600.0)

        planet_ids = {
            "Surya": swe.SUN, "Chandra": swe.MOON, "Mangal": swe.MARS,
            "Budh": swe.MERCURY, "Guru": swe.JUPITER, "Shukra": swe.VENUS,
            "Shani": swe.SATURN,
        }
        grahas = {}
        for name, pid in planet_ids.items():
            res = swe.calc_ut(jd, pid, swe.FLG_SWIEPH | swe.FLG_SIDEREAL)
            lon_g = res[0][0]
            grahas[name] = {
                "rashi": RASHI_NAMES[int(lon_g / 30)],
                "degree": round(lon_g % 30, 2),
                "nakshatra": NAKSHATRAS[int(lon_g / (360 / 27))],
            }

        # Rahu/Ketu
        res = swe.calc_ut(jd, swe.MEAN_NODE, swe.FLG_SWIEPH | swe.FLG_SIDEREAL)
        rahu_lon = res[0][0]
        ketu_lon = (rahu_lon + 180) % 360
        grahas["Rahu"] = {"rashi": RASHI_NAMES[int(rahu_lon / 30)], "degree": round(rahu_lon % 30, 2), "nakshatra": NAKSHATRAS[int(rahu_lon / (360 / 27))]}
        grahas["Ketu"] = {"rashi": RASHI_NAMES[int(ketu_lon / 30)], "degree": round(ketu_lon % 30, 2), "nakshatra": NAKSHATRAS[int(ketu_lon / (360 / 27))]}

        return {"grahas": grahas, "computed_at": now.isoformat()}
    except ImportError:
        raise HTTPException(status_code=503, detail="pyswisseph not installed")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
