"""
Kundali calculation service.
Extracted from scripts/08_gradio_kundali.py — single source of truth.
Supports: ayanamsha selection, true/mean Rahu, antardasha, raw longitudes,
          graha relationships, house significations.
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
    GRAHA_HINDI, RASHI_HINDI, NAKSHATRA_HINDI,
    AYANAMSHA_MODES, GRAHA_EN, RASHI_EN,
    GRAHA_FRIENDS, GRAHA_ENEMIES, GRAHA_KARAKA,
    HOUSE_SIGNIFICATIONS,
)
from api.services.divisional_charts import calc_varga_chart, VARGA_NAMES


_GRAHAS_MAP_MEAN = None
_GRAHAS_MAP_TRUE = None


def _init_grahas():
    global _GRAHAS_MAP_MEAN, _GRAHAS_MAP_TRUE
    if not SWE_AVAILABLE:
        return
    if _GRAHAS_MAP_MEAN is None:
        _GRAHAS_MAP_MEAN = {
            swe.SUN: "Surya", swe.MOON: "Chandra", swe.MARS: "Mangal",
            swe.MERCURY: "Budh", swe.JUPITER: "Guru", swe.VENUS: "Shukra",
            swe.SATURN: "Shani", swe.MEAN_NODE: "Rahu",
        }
    if _GRAHAS_MAP_TRUE is None:
        _GRAHAS_MAP_TRUE = {
            swe.SUN: "Surya", swe.MOON: "Chandra", swe.MARS: "Mangal",
            swe.MERCURY: "Budh", swe.JUPITER: "Guru", swe.VENUS: "Shukra",
            swe.SATURN: "Shani", swe.TRUE_NODE: "Rahu",
        }


def _graha_relationship(gname: str, rashi_i: int) -> str:
    """Determine graha's relationship with the sign lord (Friend/Neutral/Enemy)."""
    if gname in ("Rahu", "Ketu"):
        return "Neutral"
    sign_lord = RASHI_LORDS.get(rashi_i, "")
    if sign_lord == gname:
        return "Own"
    if sign_lord in GRAHA_FRIENDS.get(gname, set()):
        return "Friend"
    if sign_lord in GRAHA_ENEMIES.get(gname, set()):
        return "Enemy"
    return "Neutral"


def _calc_antardashas(maha_lord: str, maha_start: datetime, maha_years: float) -> list:
    """Calculate Antardashas (sub-periods) within a Mahadasha."""
    total_days = maha_years * 365.25
    start_idx = DASHA_SEQ.index(maha_lord)
    antardashas = []
    cur = maha_start

    for i in range(9):
        antar_lord = DASHA_SEQ[(start_idx + i) % 9]
        # Antardasha duration = (Maha years × Antar years / 120) × 365.25 days
        antar_days = (maha_years * DASHA_YEARS[antar_lord] / 120) * 365.25
        end = cur + timedelta(days=antar_days)
        antardashas.append({
            "lord": antar_lord,
            "years": round(antar_days / 365.25, 2),
            "start": cur.strftime("%Y-%m-%d"),
            "end": end.strftime("%Y-%m-%d"),
        })
        cur = end

    return antardashas


def calc_kundali(
    year: int, month: int, day: int,
    hour: int, minute: int,
    lat: float, lon: float, tz: float,
    name: str = "", place: str = "",
    ayanamsha_mode: str = "lahiri",
    rahu_mode: str = "mean",
    calc_options: list = None,
) -> Tuple[Optional[dict], Optional[str]]:
    """
    Calculate full Vedic birth chart.
    ayanamsha_mode: "lahiri" | "raman" | "kp" | "true_citra"
    rahu_mode: "mean" | "true"
    calc_options: list of extra calcs to include, e.g. ["antardasha", "d3", "d10", ...]
    Returns (data_dict, error_str).
    """
    if not SWE_AVAILABLE:
        return None, "pyswisseph not installed"

    _init_grahas()
    swe.set_ephe_path(EPHE_PATH)

    # Set ayanamsha
    ayan_id = AYANAMSHA_MODES.get(ayanamsha_mode, 1)
    swe.set_sid_mode(ayan_id)

    # Select Rahu mode
    grahas_map = _GRAHAS_MAP_TRUE if rahu_mode == "true" else _GRAHAS_MAP_MEAN

    if calc_options is None:
        calc_options = []

    # Convert local time → UTC, handling date rollover
    dt_utc = datetime(year, month, day, hour, minute) - timedelta(hours=tz)
    jd = swe.julday(dt_utc.year, dt_utc.month, dt_utc.day,
                    dt_utc.hour + dt_utc.minute / 60.0)
    ayanamsha = swe.get_ayanamsa(jd)

    # ── Grahas ─────────────────────────────────────────────────────
    grahas = {}
    for pid, gname in grahas_map.items():
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

    # ── Lagna (Ascendant) ──────────────────────────────────────────
    _, ascmc = swe.houses_ex(jd, lat, lon, b'P', swe.FLG_SIDEREAL)
    lagna = ascmc[0]
    lagna_rashi_i = int(lagna / 30)

    # Whole Sign houses
    houses = []
    for i in range(12):
        ri = (lagna_rashi_i + i) % 12
        houses.append({
            "house": i + 1,
            "rashi": ri, "rashi_name": RASHI_NAMES[ri],
            "lord": RASHI_LORDS[ri],
        })

    # Assign house to each graha
    def get_house(lon_val: float) -> int:
        rashi_i = int(lon_val / 30)
        return (rashi_i - lagna_rashi_i) % 12 + 1

    for g in grahas.values():
        g["house"] = get_house(g["longitude"])

    # ── Graha status + relationship ────────────────────────────────
    def graha_status(gname: str) -> str:
        r = grahas[gname]["rashi"]
        if EXALTATION.get(gname) == r: return "Uchcha (Exalted)"
        if DEBILITATION.get(gname) == r: return "Neecha (Debilitated)"
        if RASHI_LORDS.get(r) == gname: return "Svakshetra (Own)"
        return "Normal"

    # ── Navamsha (D-9) via divisional_charts ───────────────────────
    nav_chart = calc_varga_chart(grahas, lagna, 9)
    navamsha_out = {}
    for gn, gd in nav_chart["grahas"].items():
        navamsha_out[gn] = {
            "rashi": gd["rashi"],
            "house": gd["house"],
            "status": gd["status"],
            "retro": gd["retro"],
        }
    navamsha_lagna = nav_chart["lagna"]
    navamsha_houses = nav_chart["houses"]

    # ── Vimshottari Dasha ──────────────────────────────────────────
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

    # First dasha (partial)
    lord = DASHA_SEQ[start_idx]
    maha_years = round(DASHA_YEARS[lord] * remaining, 4)
    days = int(maha_years * 365.25)
    end = datetime.fromordinal(cur.toordinal() + days)
    dasha_entry = {
        "lord": lord, "years": round(maha_years, 2),
        "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d"),
    }
    if "antardasha" in calc_options:
        dasha_entry["antardashas"] = _calc_antardashas(lord, cur, maha_years)
    dashas.append(dasha_entry)
    cur = end

    # Remaining 9 dashas (full)
    for i in range(1, 10):
        lord = DASHA_SEQ[(start_idx + i) % 9]
        maha_years_full = DASHA_YEARS[lord]
        days = int(maha_years_full * 365.25)
        end = datetime.fromordinal(cur.toordinal() + days)
        dasha_entry = {
            "lord": lord, "years": maha_years_full,
            "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d"),
        }
        if "antardasha" in calc_options:
            dasha_entry["antardashas"] = _calc_antardashas(lord, cur, maha_years_full)
        dashas.append(dasha_entry)
        cur = end

    # ── Yoga detection ─────────────────────────────────────────────
    yogas = []

    def rashi_of(n): return grahas[n]["rashi"]
    def house_of(n): return grahas[n]["house"]

    diff = (rashi_of("Guru") - rashi_of("Chandra")) % 12
    if diff in [0, 3, 6, 9]:
        yogas.append({"name": "Gajakesari Yoga", "desc": "Jupiter-Moon kendra — wisdom, fame, prosperity", "strength": "Strong", "category": "Power"})
    if rashi_of("Surya") == rashi_of("Budh"):
        yogas.append({"name": "Budhaditya Yoga", "desc": "Sun-Mercury conjunction — sharp intellect, eloquence", "strength": "Strong", "category": "Intellect"})
    if rashi_of("Chandra") == rashi_of("Mangal"):
        yogas.append({"name": "Chandra-Mangal Yoga", "desc": "Moon-Mars conjunction — courage brings wealth", "strength": "Strong", "category": "Wealth"})

    # Pancha Mahapurusha yogas
    for g, (yn, yd, cat) in [
        ("Mangal", ("Ruchaka Yoga", "Courage and leadership", "Power")),
        ("Budh",   ("Bhadra Yoga",  "Intellect and commerce", "Intellect")),
        ("Guru",   ("Hamsa Yoga",   "Spirituality and wisdom", "Spiritual")),
        ("Shukra", ("Malavya Yoga", "Beauty and arts", "Wealth")),
        ("Shani",  ("Sasa Yoga",    "Power and discipline", "Power")),
    ]:
        h = house_of(g); r = rashi_of(g)
        if h in [1, 4, 7, 10] and (RASHI_LORDS.get(r) == g or EXALTATION.get(g) == r):
            yogas.append({"name": yn, "desc": yd, "strength": "Very Strong", "category": cat})

    # Mangal Dosha
    mh = house_of("Mangal")
    if mh in [1, 2, 4, 7, 8, 12]:
        yogas.append({"name": "Mangal Dosha", "desc": f"Mars in house {mh} — caution in marriage", "strength": "Applicable", "category": "Marriage"})

    # Raj Yoga
    kendra_lords = set(); trikona_lords = set()
    for h in houses:
        if h["house"] in [1, 4, 7, 10]: kendra_lords.add(RASHI_LORDS[h["rashi"]])
        if h["house"] in [1, 5, 9]: trikona_lords.add(RASHI_LORDS[h["rashi"]])
    ry = kendra_lords & trikona_lords
    if ry:
        yogas.append({"name": "Raj Yoga", "desc": f"Kendra-trikona lords ({', '.join(ry)}) — success and fame", "strength": "Strong", "category": "Power"})

    # Dhana Yoga (2nd/11th lord in kendra or trikona)
    lord_2 = RASHI_LORDS.get(houses[1]["rashi"], "")
    lord_11 = RASHI_LORDS.get(houses[10]["rashi"], "")
    for dl in [lord_2, lord_11]:
        if dl and dl in grahas:
            dh = house_of(dl)
            if dh in [1, 4, 5, 7, 9, 10]:
                yogas.append({"name": "Dhana Yoga", "desc": f"{dl} (wealth lord) in house {dh} — financial prosperity", "strength": "Strong", "category": "Wealth"})
                break

    # Vipareeta Raja Yoga (6th/8th/12th lord in another dusthana)
    dusthana = [6, 8, 12]
    dusthana_lords = [RASHI_LORDS.get(houses[h-1]["rashi"], "") for h in dusthana]
    for dl in dusthana_lords:
        if dl and dl in grahas:
            dh = house_of(dl)
            if dh in dusthana and dh != [h for h in dusthana if RASHI_LORDS.get(houses[h-1]["rashi"]) == dl][0]:
                yogas.append({"name": "Vipareeta Raja Yoga", "desc": f"Dusthana lord {dl} in dusthana — success through adversity", "strength": "Strong", "category": "Power"})
                break

    # Kemadruma Yoga (Moon alone — no planets in 2nd/12th from Moon)
    moon_rashi = rashi_of("Chandra")
    adjacent = [(moon_rashi - 1) % 12, (moon_rashi + 1) % 12]
    planets_adjacent = any(
        rashi_of(gn) in adjacent
        for gn in ["Surya", "Mangal", "Budh", "Guru", "Shukra", "Shani"]
    )
    if not planets_adjacent:
        yogas.append({"name": "Kemadruma Yoga", "desc": "Moon isolated — emotional challenges but self-reliance", "strength": "Applicable", "category": "Adversity"})

    # ── Build clean API response ───────────────────────────────────
    graha_order = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu"]
    grahas_out = {}
    for gn in graha_order:
        g = grahas[gn]
        nak_lord = NAKSHATRA_LORDS[g["nakshatra_idx"]]
        relationship = _graha_relationship(gn, g["rashi"])
        grahas_out[gn] = {
            "rashi": g["rashi_name"],
            "rashi_en": RASHI_EN.get(g["rashi_name"], ""),
            "house": g["house"],
            "degree": g["degree"],
            "longitude": round(g["longitude"], 4),
            "nakshatra": g["nakshatra"],
            "nakshatra_hindi": NAKSHATRA_HINDI.get(g["nakshatra"], ""),
            "nakshatra_lord": nak_lord,
            "pada": g["pada"],
            "retro": g["retro"],
            "status": graha_status(gn),
            "relationship": relationship,
            "house_lord": RASHI_LORDS.get(g["rashi"], ""),
            "graha_en": GRAHA_EN.get(gn, gn),
            "graha_hindi": GRAHA_HINDI.get(gn, ""),
            "karaka": GRAHA_KARAKA.get(gn, ""),
            "speed": round(g["speed"], 4),
        }

    # House output with significations
    houses_out = []
    for h in houses:
        planets_in = [gn for gn in graha_order if grahas[gn]["house"] == h["house"]]
        houses_out.append({
            "house": h["house"],
            "rashi": h["rashi_name"],
            "rashi_en": RASHI_EN.get(h["rashi_name"], ""),
            "lord": h["lord"],
            "lord_en": GRAHA_EN.get(h["lord"], h["lord"]),
            "signification": HOUSE_SIGNIFICATIONS.get(h["house"], ""),
            "planets": planets_in,
        })

    # Lagna nakshatra
    lagna_nak_i = int(lagna / (360 / 27))
    lagna_data = {
        "rashi": RASHI_NAMES[lagna_rashi_i],
        "rashi_en": RASHI_EN.get(RASHI_NAMES[lagna_rashi_i], ""),
        "nakshatra": NAKSHATRAS[lagna_nak_i],
        "nakshatra_hindi": NAKSHATRA_HINDI.get(NAKSHATRAS[lagna_nak_i], ""),
        "pada": int((lagna % (360/27)) / (360/108)) + 1,
        "degree": round(lagna % 30, 2),
        "full_degree": round(lagna, 4),
    }

    # Ayanamsha mode name for display
    ayan_label = ayanamsha_mode.replace("_", " ").title()
    if ayanamsha_mode == "lahiri":
        ayan_label = "Lahiri/Chitra Paksha"
    elif ayanamsha_mode == "kp":
        ayan_label = "Krishnamurti (KP)"
    elif ayanamsha_mode == "true_citra":
        ayan_label = "True Chitrapaksha"

    result = {
        "name": name,
        "place": place,
        "birth_date": birth_dt.strftime("%d %B %Y, %I:%M %p"),
        "lat": lat, "lon": lon, "tz": tz,
        "ayanamsha": round(ayanamsha, 4),
        "ayanamsha_mode": ayanamsha_mode,
        "ayanamsha_label": ayan_label,
        "rahu_mode": rahu_mode,
        "lagna": lagna_data,
        "grahas": grahas_out,
        "houses": houses_out,
        "navamsha": navamsha_out,
        "navamsha_lagna": navamsha_lagna,
        "navamsha_houses": [
            {"house": h["house"], "rashi": h["rashi"], "lord": h["lord"]}
            for h in navamsha_houses
        ],
        "dashas": dashas,
        "yogas": yogas,
    }

    # ── Optional varga charts ──────────────────────────────────────
    requested_vargas = [opt for opt in calc_options if opt.startswith("d") and opt[1:].isdigit()]
    if requested_vargas:
        result["varga_charts"] = {}
        for rv in requested_vargas:
            div = int(rv[1:])
            if div in VARGA_NAMES and div != 1:  # D-1 is already the main chart
                vc = calc_varga_chart(grahas, lagna, div)
                result["varga_charts"][f"D-{div}"] = vc

    # ── Optional Ashtakavarga ────────────────────────────────────
    if "ashtakavarga" in calc_options:
        from api.services.ashtakavarga import calc_ashtakavarga
        result["ashtakavarga"] = calc_ashtakavarga(grahas, lagna_rashi_i)

    # ── Optional Shadbala ────────────────────────────────────────
    if "shadbala" in calc_options:
        from api.services.shadbala import calc_shadbala, calc_bhavabala
        sb = calc_shadbala(grahas)
        result["shadbala"] = sb
        result["bhavabala"] = calc_bhavabala(houses, grahas, sb)

    # ── Optional Upagraha ────────────────────────────────────────
    if "upagraha" in calc_options:
        from api.services.upagraha import calc_upagraha
        weekday = birth_dt.weekday()  # 0=Monday in Python, convert
        # Python weekday: 0=Mon,1=Tue,...,6=Sun → We need 0=Sun,1=Mon,...,6=Sat
        wday_sun = (weekday + 1) % 7
        result["upagraha"] = calc_upagraha(
            sun_lon=grahas["Surya"]["longitude"],
            moon_lon=grahas["Chandra"]["longitude"],
            weekday=wday_sun,
        )

    return result, None
