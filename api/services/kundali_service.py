"""
Kundali calculation service.
Extracted from scripts/08_gradio_kundali.py — single source of truth.
"""
from datetime import datetime, timedelta
from typing import Tuple, Optional

try:
    import swisseph as swe
    SWE_AVAILABLE = True
except ImportError:
    SWE_AVAILABLE = False

from api.config import (
    EPHE_PATH, RASHI_NAMES, NAKSHATRAS, NAKSHATRA_LORDS,
    DASHA_YEARS, DASHA_SEQ, RASHI_LORDS, EXALTATION, DEBILITATION,
    GRAHA_HINDI, RASHI_HINDI, NAKSHATRA_HINDI
)

_GRAHAS_MAP = None


def _init_grahas():
    global _GRAHAS_MAP
    if SWE_AVAILABLE and _GRAHAS_MAP is None:
        _GRAHAS_MAP = {
            swe.SUN: "Surya", swe.MOON: "Chandra", swe.MARS: "Mangal",
            swe.MERCURY: "Budh", swe.JUPITER: "Guru", swe.VENUS: "Shukra",
            swe.SATURN: "Shani", swe.MEAN_NODE: "Rahu",
        }


def calc_kundali(
    year: int, month: int, day: int,
    hour: int, minute: int,
    lat: float, lon: float, tz: float,
    name: str = "", place: str = ""
) -> Tuple[Optional[dict], Optional[str]]:
    """Calculate full Vedic birth chart. Returns (data_dict, error_str)."""
    if not SWE_AVAILABLE:
        return None, "pyswisseph not installed"

    _init_grahas()
    swe.set_ephe_path(EPHE_PATH)
    swe.set_sid_mode(swe.SIDM_LAHIRI)

    # Convert local time → UTC, handling date rollover (e.g. 1 AM IST = prev day 19:30 UTC)
    dt_utc = datetime(year, month, day, hour, minute) - timedelta(hours=tz)
    jd = swe.julday(dt_utc.year, dt_utc.month, dt_utc.day,
                    dt_utc.hour + dt_utc.minute / 60.0)
    ayanamsha = swe.get_ayanamsa(jd)

    # Grahas
    grahas = {}
    for pid, gname in _GRAHAS_MAP.items():
        flags = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
        res = swe.calc_ut(jd, pid, flags)
        lon_g = res[0][0]
        speed = res[0][3]
        rashi_i = int(lon_g / 30)
        nak_i = int(lon_g / (360 / 27))
        grahas[gname] = {
            "longitude": lon_g, "speed": speed,
            "rashi": rashi_i, "rashi_name": RASHI_NAMES[rashi_i],
            "degree": round(lon_g % 30, 2),
            "nakshatra": NAKSHATRAS[nak_i], "nakshatra_idx": nak_i,
            "pada": int((lon_g % (360 / 27)) / (360 / 108)) + 1,
            "retro": speed < 0, "house": 0,
        }

    # Ketu (180° opposite Rahu)
    rahu_lon = grahas["Rahu"]["longitude"]
    ketu_lon = (rahu_lon + 180) % 360
    ketu_rashi = int(ketu_lon / 30)
    ketu_nak = int(ketu_lon / (360 / 27))
    grahas["Ketu"] = {
        "longitude": ketu_lon, "speed": 0,
        "rashi": ketu_rashi, "rashi_name": RASHI_NAMES[ketu_rashi],
        "degree": round(ketu_lon % 30, 2),
        "nakshatra": NAKSHATRAS[ketu_nak], "nakshatra_idx": ketu_nak,
        "pada": int((ketu_lon % (360 / 27)) / (360 / 108)) + 1,
        "retro": True, "house": 0,
    }

    # Houses (Placidus sidereal)
    cusps, ascmc = swe.houses_ex(jd, lat, lon, b'P', swe.FLG_SIDEREAL)
    lagna = ascmc[0]
    houses = []
    for i in range(12):
        c = cusps[i]
        ri = int(c / 30)
        houses.append({
            "house": i + 1, "cusp": c,
            "rashi": ri, "rashi_name": RASHI_NAMES[ri],
            "degree": round(c % 30, 2), "lord": RASHI_LORDS[ri],
        })

    # Assign house to each graha
    def get_house(lon_val: float) -> int:
        for i in range(12):
            start = cusps[i]
            end = cusps[(i + 1) % 12]
            if end < start:
                if lon_val >= start or lon_val < end:
                    return i + 1
            else:
                if start <= lon_val < end:
                    return i + 1
        return 1

    for g in grahas.values():
        g["house"] = get_house(g["longitude"])

    # Graha status
    def graha_status(gname: str) -> str:
        r = grahas[gname]["rashi"]
        if EXALTATION.get(gname) == r: return "Uchcha (Exalted)"
        if DEBILITATION.get(gname) == r: return "Neecha (Debilitated)"
        if RASHI_LORDS.get(r) == gname: return "Svakshetra (Own)"
        return "Normal"

    # Vimshottari Dasha
    moon = grahas["Chandra"]
    nak_i = moon["nakshatra_idx"]
    nak_lord = NAKSHATRA_LORDS[nak_i]
    nak_span = 360 / 27
    nak_start = nak_i * nak_span
    elapsed = moon["longitude"] - nak_start
    remaining = 1 - (elapsed / nak_span)
    start_idx = DASHA_SEQ.index(nak_lord)
    birth_dt = datetime(year, month, day, hour, minute)
    dashas = []
    cur = birth_dt
    lord = DASHA_SEQ[start_idx]
    days = int(DASHA_YEARS[lord] * remaining * 365.25)
    end = datetime.fromordinal(cur.toordinal() + days)
    dashas.append({
        "lord": lord, "years": round(DASHA_YEARS[lord] * remaining, 2),
        "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d")
    })
    cur = end
    for i in range(1, 10):
        lord = DASHA_SEQ[(start_idx + i) % 9]
        days = int(DASHA_YEARS[lord] * 365.25)
        end = datetime.fromordinal(cur.toordinal() + days)
        dashas.append({
            "lord": lord, "years": DASHA_YEARS[lord],
            "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d")
        })
        cur = end

    # Yoga detection
    yogas = []

    def rashi_of(n): return grahas[n]["rashi"]
    def house_of(n): return grahas[n]["house"]

    diff = (rashi_of("Guru") - rashi_of("Chandra")) % 12
    if diff in [0, 3, 6, 9]:
        yogas.append({"name": "Gajakesari Yoga", "desc": "Jupiter-Moon kendra — wisdom, fame, prosperity", "strength": "Strong"})
    if rashi_of("Surya") == rashi_of("Budh"):
        yogas.append({"name": "Budhaditya Yoga", "desc": "Sun-Mercury conjunction — sharp intellect, eloquence", "strength": "Strong"})
    if rashi_of("Chandra") == rashi_of("Mangal"):
        yogas.append({"name": "Chandra-Mangal Yoga", "desc": "Moon-Mars conjunction — courage brings wealth", "strength": "Strong"})
    for g, (yn, yd) in [
        ("Mangal", ("Ruchaka Yoga", "Courage and leadership")),
        ("Budh",   ("Bhadra Yoga",  "Intellect and commerce")),
        ("Guru",   ("Hamsa Yoga",   "Spirituality and wisdom")),
        ("Shukra", ("Malavya Yoga", "Beauty and arts")),
        ("Shani",  ("Sasa Yoga",    "Power and discipline")),
    ]:
        h = house_of(g); r = rashi_of(g)
        if h in [1, 4, 7, 10] and (RASHI_LORDS.get(r) == g or EXALTATION.get(g) == r):
            yogas.append({"name": yn, "desc": yd, "strength": "Very Strong"})

    mh = house_of("Mangal")
    if mh in [1, 2, 4, 7, 8, 12]:
        yogas.append({"name": "Mangal Dosha", "desc": f"Mars in house {mh} — caution in marriage", "strength": "Applicable"})

    kendra_lords = set(); trikona_lords = set()
    for h in houses:
        if h["house"] in [1, 4, 7, 10]: kendra_lords.add(h["lord"])
        if h["house"] in [1, 5, 9]: trikona_lords.add(h["lord"])
    ry = kendra_lords & trikona_lords
    if ry:
        yogas.append({"name": "Raj Yoga", "desc": f"Kendra-trikona lords ({', '.join(ry)}) — success and fame", "strength": "Strong"})

    # Build clean API response
    graha_order = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu"]
    grahas_out = {}
    for gn in graha_order:
        g = grahas[gn]
        grahas_out[gn] = {
            "rashi": g["rashi_name"],
            "house": g["house"],
            "degree": g["degree"],
            "nakshatra": g["nakshatra"],
            "pada": g["pada"],
            "retro": g["retro"],
            "status": graha_status(gn),
        }

    return {
        "name": name,
        "place": place,
        "birth_date": birth_dt.strftime("%d %B %Y, %I:%M %p"),
        "lat": lat, "lon": lon, "tz": tz,
        "ayanamsha": round(ayanamsha, 4),
        "lagna": {
            "rashi": RASHI_NAMES[int(lagna / 30)],
            "nakshatra": NAKSHATRAS[int(lagna / (360 / 27))],
            "degree": round(lagna % 30, 2),
        },
        "grahas": grahas_out,
        "houses": [
            {"house": h["house"], "rashi": h["rashi_name"], "lord": h["lord"], "degree": h["degree"]}
            for h in houses
        ],
        "dashas": dashas,
        "yogas": yogas,
    }, None
