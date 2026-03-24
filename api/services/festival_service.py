"""
UPLOAD THIS TO VM:  ~/books/api/services/festival_service.py

Hindu festival calculator using pyswisseph.
Calculates every festival from lunar tithi rules — no hardcoded dates.
Includes Vedic dosh rules:
  - Bhadra/Vishti (Mukha / Deha / Puchha distinction)
  - Vridhi / Ksheya tithi
  - Nishita Kaal for Janmashtami
  - Grahan Dosha (eclipse conflict) for ALL festivals with exact timings
  - Adhik Maas (Malmas) detection and festival shift notes
"""
import logging
import math
from functools import lru_cache
import swisseph as swe
from datetime import date, timedelta
from typing import Optional, List, Dict, Any, Tuple
import os

_EPS = 1e-9  # epsilon for tithi/karana boundary float-precision safety

log = logging.getLogger(__name__)

EPHE_PATH = os.path.expanduser("~/books/data/swiss_ephe")

# ─── Lookup tables ─────────────────────────────────────────────────────────

TITHI_NAMES = [
    "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
    "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
    "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima",
    "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
    "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
    "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Amavasya",
]
PAKSHA_NAMES = ["Shukla"] * 15 + ["Krishna"] * 15

LUNAR_MONTH_NAMES = [
    "Chaitra", "Vaishakha", "Jyeshtha", "Ashadha", "Shravana",
    "Bhadrapada", "Ashwin", "Kartik", "Margashirsha", "Pausha", "Magha", "Phalguna",
]

# Purnimanta names shift one forward relative to Amanta (month ends at Purnima not Amavasya)
PURNIMANTA_MONTH_NAMES = [
    "Vaishakha", "Jyeshtha", "Ashadha", "Shravana", "Bhadrapada",
    "Ashwin", "Kartik", "Margashirsha", "Pausha", "Magha", "Phalguna", "Chaitra",
]

# Tradition-specific rules that differ across sampradayas
TRADITION_RULES: Dict[str, Dict[str, Any]] = {
    "smarta": {
        "ekadashi": "sunrise",          # Ekadashi observed on day the tithi covers sunrise
        "janmashtami_rohini": False,     # Rohini nakshatra not required (Nishita only)
        "description": "Standard Smarta / North Indian temple tradition",
    },
    "vaishnava": {
        "ekadashi": "arunodaya",        # Dashami must not touch Arunodaya (96 min before sunrise)
        "janmashtami_rohini": True,     # Require Rohini at Nishita Kaal for Vaishnava observance
        "description": "Vaishnava / ISKCON / Gaudiya / Madhva tradition",
    },
    "north_indian": {
        "ekadashi": "sunrise",
        "janmashtami_rohini": False,
        "description": "North Indian (UP/Bihar/Rajasthan) — same rules as Smarta, Purnimanta months",
    },
    "gujarati": {
        "ekadashi": "sunrise",
        "janmashtami_rohini": False,
        "description": "Gujarati / Swaminarayan — year begins Kartik Shukla Pratipada",
    },
    "bengali": {
        "ekadashi": "sunrise",
        "janmashtami_rohini": False,
        "description": "Bengali — Durga Puja uses Mahasaptami sunrise rule",
    },
}

# ─── Core ephemeris helpers ─────────────────────────────────────────────────

_inited = False

def _init():
    """Idempotent init — safe to call from every cached helper (no-op after first call)."""
    global _inited
    if _inited:
        return
    if not os.path.isdir(EPHE_PATH):
        raise RuntimeError(
            f"Swiss Ephemeris files missing at '{EPHE_PATH}'. "
            "Run: wget https://www.astro.com/swisseph/ephe/ into that directory."
        )
    swe.set_ephe_path(EPHE_PATH)
    swe.set_sid_mode(swe.SIDM_LAHIRI)  # Lahiri ayanamsha for all sidereal calculations
    _inited = True

@lru_cache(maxsize=16384)
def _moon_sun_diff_cached(jd_rounded: float) -> float:
    """Inner cached function — always called with pre-rounded JD to avoid duplicate cache entries."""
    # Tithi uses TROPICAL geocentric elongation — do NOT add FLG_SIDEREAL here.
    # swe.set_sid_mode(LAHIRI) only affects calls that explicitly pass FLG_SIDEREAL.
    sun  = swe.calc_ut(jd_rounded, swe.SUN)[0][0]
    moon = swe.calc_ut(jd_rounded, swe.MOON)[0][0]
    return (moon - sun) % 360.0

def _moon_sun_diff(jd: float) -> float:
    """Public wrapper — normalizes JD before hitting the cache to prevent duplicate entries."""
    return _moon_sun_diff_cached(round(jd, 8))

def _tithi_idx(jd: float) -> int:
    # math.floor + epsilon avoids misassignment at exact tithi boundaries due to float rounding.
    # _moon_sun_diff already normalizes jd internally.
    return int(math.floor((_moon_sun_diff(jd) + _EPS) / 12.0)) % 30

# ─── Vishti (Bhadra) karana detection ───────────────────────────────────────
# 60 karanas per lunar month (30 tithis × 2 half-tithis each = 60 slots of 6°).
# Index 0 = Kimstughna (fixed, first half of Shukla Pratipada) — NOT Vishti.
# Indices 1–56 = 8 cycles of 7 movable karanas (Bava,Balava,Kaulava,Taitila,Garaja,Vanija,Vishti).
# Vishti is the 7th in each cycle: indices 7,14,21,28,35,42,49,56.
# Indices 57–59 = Shakuni,Chatushpada,Nagava (fixed, end of month) — NOT Vishti.
# Formula: k % 7 == 0, but exclude k=0 (Kimstughna) and k>56 (fixed tail karanas).
#
# Bhadra Shirsh (head/face) = first 1/7 of slot ≈ ~1.3h — most inauspicious
# Bhadra Deha (body)        = middle 5/7 of slot ≈ ~6.4h — inauspicious
# Bhadra Puchha (tail)      = last 1/7 of slot  ≈ ~1.3h — acceptable for some work

def _karana_index(jd: float) -> int:
    """Return karana index 0..59 (each slot = 6° of Moon-Sun elongation)."""
    return int(math.floor((_moon_sun_diff(jd) + _EPS) / 6.0)) % 60

def _is_vishti_karana(k: int) -> bool:
    """True if karana index k is a Vishti slot (excludes Kimstughna at 0 and fixed tail 57-59)."""
    return k != 0 and k <= 56 and (k % 7) == 0

def _is_vishti(jd: float) -> bool:
    return _is_vishti_karana(_karana_index(jd))

def _vishti_fraction(jd: float) -> Optional[float]:
    """If in vishti, return progress 0.0–1.0 within the 6° karana slot. Otherwise None."""
    k = _karana_index(jd)
    if not _is_vishti_karana(k):
        return None
    diff  = _moon_sun_diff(jd)  # already normalizes internally
    lo_deg = k * 6.0                       # k <= 56 → k*6 <= 336 < 360, no wrap needed
    frac   = (diff - lo_deg) / 6.0
    return max(0.0, min(1.0, frac))

def _sun_lon(jd: float) -> float:
    return swe.calc_ut(jd, swe.SUN)[0][0]

def _sun_lon_sid(jd: float) -> float:
    return swe.calc_ut(jd, swe.SUN, swe.FLG_SIDEREAL)[0][0]

# Hindu rising: disc-center, no-refraction (SE_BIT_HINDU_RISING = 768)
_SE_DISC_CENTER   = 256
_SE_NO_REFRACTION = 512
_SE_HINDU_RISING  = _SE_DISC_CENTER | _SE_NO_REFRACTION  # 768

def _local_6am_jd(year: int, month: int, day: int, tz: float) -> float:
    """Fallback only — use _actual_sunrise_jd when lat/lon are available."""
    return swe.julday(year, month, day, 6.0 - tz)

@lru_cache(maxsize=1024)
def _actual_sunrise_jd(year: int, month: int, day: int,
                        lat: float, lon: float, tz: float) -> float:
    """
    Exact astronomical sunrise (Hindu: disc-centre, no refraction).
    Falls back to 6 AM proxy if rise_trans fails.
    Cached: same (date, location) called multiple times per festival calculation.
    """
    _init()
    jd_midnight_utc = swe.julday(year, month, day, -tz)  # local midnight in UTC
    geopos = [lon, lat, 0.0]
    try:
        rsmi = swe.CALC_RISE | _SE_HINDU_RISING
        ret = swe.rise_trans(jd_midnight_utc, swe.SUN, rsmi, geopos, 0.0, 0.0, swe.FLG_SWIEPH)
        if ret[0] == 0:
            return ret[1][0]
    except Exception:
        pass
    try:
        ret = swe.rise_trans(jd_midnight_utc, swe.SUN, swe.CALC_RISE, geopos, 0.0, 0.0, swe.FLG_SWIEPH)
        if ret[0] == 0:
            return ret[1][0]
    except Exception:
        pass
    return _local_6am_jd(year, month, day, tz)

@lru_cache(maxsize=1024)
def _actual_sunset_jd(year: int, month: int, day: int,
                       lat: float, lon: float, tz: float) -> float:
    """
    Exact astronomical sunset (Hindu: disc-centre, no refraction).
    Falls back to sunrise + 12.5h proxy if rise_trans fails.
    Cached: same (date, location) called multiple times per festival calculation.
    """
    _init()
    jd_noon_utc = swe.julday(year, month, day, 12.0 - tz)  # local noon in UTC (safe search start)
    geopos = [lon, lat, 0.0]
    try:
        rsmi = swe.CALC_SET | _SE_HINDU_RISING
        ret = swe.rise_trans(jd_noon_utc, swe.SUN, rsmi, geopos, 0.0, 0.0, swe.FLG_SWIEPH)
        if ret[0] == 0:
            return ret[1][0]
    except Exception:
        pass
    try:
        ret = swe.rise_trans(jd_noon_utc, swe.SUN, swe.CALC_SET, geopos, 0.0, 0.0, swe.FLG_SWIEPH)
        if ret[0] == 0:
            return ret[1][0]
    except Exception:
        pass
    return _local_6am_jd(year, month, day, tz) + 12.5 / 24.0

def _jd_to_parts(jd: float, tz: float):
    jd_loc = jd + tz / 24.0
    y, m, d, h = swe.revjul(jd_loc)
    return int(y), int(m), int(d), h

def _jd_to_date_str(jd: float, tz: float) -> str:
    y, m, d, _ = _jd_to_parts(jd, tz)
    return f"{y:04d}-{m:02d}-{d:02d}"

def _jd_to_time_str(jd: float, tz: float) -> str:
    _, _, _, h = _jd_to_parts(jd, tz)
    hr = int(h)
    mn = int((h - hr) * 60)
    return f"{hr:02d}:{mn:02d}"

def _iso_to_local_jd(iso_date: str, tz: float) -> float:
    """Convert YYYY-MM-DD to JD at local 6:00 AM (representative daytime moment)."""
    y, m, d = map(int, iso_date.split("-"))
    return swe.julday(y, m, d, 6.0 - tz)

def _festival_in_range(iso_date: str, rng: Optional[Tuple[float, float]], tz: float) -> bool:
    """Return True if festival date (as JD) falls within a lunar month JD range (±1 day buffer)."""
    if not rng:
        return False
    try:
        jd = _iso_to_local_jd(iso_date, tz)
        return (rng[0] - 1.0) <= jd <= (rng[1] + 1.0)
    except Exception:
        return False

# ─── Tithi boundary search ─────────────────────────────────────────────────

def _find_tithi_start(target_idx: int, near_jd: float) -> float:
    # 32 bisection iterations → precision ~1/2^32 of the 1h bracket ≈ 0.8 ms (needed for short Kshaya tithis)
    step = 1.0 / 24.0
    jd = near_jd
    for _ in range(52):
        jd -= step
        if _tithi_idx(jd) != target_idx:
            lo, hi = jd, jd + step
            for _ in range(32):
                mid = (lo + hi) / 2.0
                if _tithi_idx(mid) == target_idx:
                    hi = mid
                else:
                    lo = mid
            return hi
    return near_jd - 1.0

def _find_tithi_end(target_idx: int, near_jd: float) -> float:
    # 32 bisection iterations → precision ~1/2^32 of the 1h bracket ≈ 0.8 ms (needed for short Kshaya tithis)
    step = 1.0 / 24.0
    jd = near_jd
    for _ in range(52):
        jd += step
        if _tithi_idx(jd) != target_idx:
            lo, hi = jd - step, jd
            for _ in range(32):
                mid = (lo + hi) / 2.0
                if _tithi_idx(mid) == target_idx:
                    lo = mid
                else:
                    hi = mid
            return lo
    return near_jd + 1.0

# ─── Tithi type classification ─────────────────────────────────────────────

def _tithi_type(start_jd: float, end_jd: float, sunrise_jd: float, next_sunrise_jd: float) -> str:
    inside_first  = start_jd <= sunrise_jd <= end_jd
    inside_second = start_jd <= next_sunrise_jd <= end_jd
    if inside_first and inside_second:
        return "vridhi"
    duration_h = (end_jd - start_jd) * 24.0
    if duration_h < 20:
        return "ksheya"
    return "normal"

# ─── Solar rashi entry ─────────────────────────────────────────────────────

def _find_solar_entry(year: int, target_lon: float, approx_month: int, tz: float) -> Tuple[str, str]:
    def _rel(jd):
        return (_sun_lon_sid(jd) - target_lon + 180.0) % 360.0 - 180.0

    jd     = swe.julday(year, max(1, approx_month - 1), 1)
    jd_end = swe.julday(year, min(12, approx_month + 2), 1)
    prev   = _rel(jd)
    found  = None

    while jd <= jd_end:
        cur = _rel(jd)
        if prev < 0 <= cur:
            lo, hi = jd - 1.0, jd
            for _ in range(32):
                mid = (lo + hi) / 2.0
                if _rel(mid) < 0:
                    lo = mid
                else:
                    hi = mid
            found = (lo + hi) / 2.0
            break
        prev = cur
        jd  += 1.0

    if found is None:
        return f"{year}-{approx_month:02d}-14", "N/A"
    return _jd_to_date_str(found, tz), _jd_to_time_str(found, tz)

# ─── Bhadra (Vishti) dosh helpers ─────────────────────────────────────────

def _find_vishti_start_before(jd: float) -> Optional[float]:
    """Find when current Vishti period started (scan backward)."""
    if not _is_vishti(jd):
        return None
    step = 1.0 / 24.0
    for _ in range(52):
        jd -= step
        if not _is_vishti(jd):
            lo, hi = jd, jd + step
            for _ in range(22):
                mid = (lo + hi) / 2.0
                if _is_vishti(mid):
                    hi = mid
                else:
                    lo = mid
            return hi
    return None

def _find_vishti_end_after(jd: float) -> Optional[float]:
    """Scan forward to find when current/upcoming Vishti period ends."""
    if not _is_vishti(jd):
        step = 0.25
        for _ in range(20):
            jd += step
            if _is_vishti(jd):
                break
        else:
            return None

    step = 1.0 / 24.0
    for _ in range(52):
        jd += step
        if not _is_vishti(jd):
            lo, hi = jd - step, jd
            for _ in range(22):
                mid = (lo + hi) / 2.0
                if _is_vishti(mid):
                    lo = mid
                else:
                    hi = mid
            return lo
    return None

_MARTYA_RASHIS = {3, 4, 10, 11}   # Cancer, Leo, Aquarius, Pisces → Earth realm → malefic
# Swarga (0,1,2,7) and Patala (5,6,8,9) → benign, no warning needed

def _bhadra_loka(jd: float) -> str:
    """
    Returns Bhadra's current cosmic residence based on Moon's sidereal rashi.
    Martya Loka (Earth) = malefic. Swarga/Patala = benign.
    Rashis: 0=Aries, 1=Taurus, 2=Gemini, 3=Cancer, 4=Leo, 5=Virgo,
            6=Libra, 7=Scorpio, 8=Sagittarius, 9=Capricorn, 10=Aquarius, 11=Pisces
    """
    try:
        _init()  # ensures set_sid_mode(LAHIRI) is set
        moon_lon = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)[0][0]
        rashi = int(moon_lon / 30) % 12
        if rashi in _MARTYA_RASHIS:
            return "Martya"
        elif rashi in {0, 1, 2, 7}:
            return "Swarga"
        else:
            return "Patala"
    except Exception:
        return "Martya"  # conservative fallback: warn

def _bhadra_phase(jd: float, tz: float) -> Optional[str]:
    """
    Returns 'Shirsh' (head), 'Deha' (body), or 'Puchha' (tail) if in Bhadra.
    Shirsh = first 1/7, Deha = middle 5/7, Puchha = last 1/7.
    Returns None if not in Bhadra.
    """
    frac = _vishti_fraction(jd)
    if frac is None:
        return None
    if frac < 1/7:
        return "Shirsh (head) — most inauspicious"
    elif frac < 6/7:
        return "Deha (body) — inauspicious"
    else:
        return "Puchha (tail) — somewhat acceptable"

# ─── Eclipse detection for Grahan Dosha ────────────────────────────────────

def _get_year_eclipses(year: int, lat: float, lon: float, tz: float) -> List[dict]:
    """
    Compute solar and lunar eclipses VISIBLE FROM lat/lon for the year.
    Uses topocentric _loc functions — eclipses invisible from the observer's
    location are excluded (no false Grahan Dosha for Pacific-only eclipses).
    Falls back to global detection if _loc raises an exception.
    """
    eclipses: List[dict] = []
    jd_start = swe.julday(year, 1, 1, 0.0)
    jd_end   = swe.julday(year + 1, 1, 1, 0.0)
    geopos   = [lon, lat, 0.0]

    def _parse_eclipse(tret, is_solar: bool, etype: int) -> dict:
        """Extract sparsha/madhya/moksha from tret array (same layout for _loc variants)."""
        madhya_jd  = tret[0] if tret[0] > 0 else 0
        sparsha_jd = tret[1] if len(tret) > 1 and tret[1] > 0 else madhya_jd - 1.5 / 24
        # Moksha: index 4 for partial end; index 3 or 6 for penumbral end
        _mok = tret[4] if len(tret) > 4 and tret[4] > madhya_jd else 0
        if not _mok and len(tret) > 6 and tret[6] > madhya_jd:
            _mok = tret[6]
        moksha_jd = _mok if _mok else madhya_jd + (madhya_jd - sparsha_jd)

        if is_solar:
            if etype & swe.ECL_TOTAL:     label = "Total Solar"
            elif etype & swe.ECL_ANNULAR: label = "Annular Solar"
            else:                          label = "Partial Solar"
            sutak_hrs = 12
        else:
            if etype & swe.ECL_TOTAL:     label = "Total Lunar"
            elif etype & swe.ECL_PARTIAL: label = "Partial Lunar"
            else:                          label = "Penumbral"
            sutak_hrs = 0 if label == "Penumbral" else 9

        return {
            "type":         label,
            "is_solar":     is_solar,
            "date":         _jd_to_date_str(madhya_jd, tz),
            "madhya_jd":    madhya_jd,
            "sparsha_jd":   sparsha_jd,
            "moksha_jd":    moksha_jd,
            "sparsha_time": _jd_to_time_str(sparsha_jd, tz),
            "madhya_time":  _jd_to_time_str(madhya_jd, tz),
            "moksha_time":  _jd_to_time_str(moksha_jd, tz),
            "sutak_hrs":    sutak_hrs,
            "sutak_start":  _jd_to_time_str(sparsha_jd - sutak_hrs / 24.0, tz) if sutak_hrs > 0 else None,
        }

    # ── Solar eclipses visible from this location ─────────────────────────
    jd = jd_start
    while jd < jd_end:
        try:
            ret   = swe.sol_eclipse_when_loc(jd, geopos, False, swe.FLG_SWIEPH)
            etype, tret = ret[0], ret[1]
        except Exception:
            # _loc unavailable — use global to advance jd only; skip adding (visibility unverified)
            try:
                ret = swe.sol_eclipse_when_glob(jd, swe.FLG_SWIEPH)
                etype, tret = ret[0], ret[1]
            except Exception:
                break
            if etype < 0:
                break
            jd = tret[0] + 20
            continue  # cannot verify local visibility — do not add to avoid false Grahan Dosha
        if etype < 0:
            break
        madhya_jd = tret[0]
        if madhya_jd <= jd or madhya_jd >= jd_end:
            break
        if etype > 0:   # etype > 0 = eclipse visible at this location
            eclipses.append(_parse_eclipse(tret, True, etype))
        jd = madhya_jd + 20

    # ── Lunar eclipses visible from this location ─────────────────────────
    jd = jd_start
    while jd < jd_end:
        try:
            ret   = swe.lun_eclipse_when_loc(jd, geopos, False, swe.FLG_SWIEPH)
            etype, tret = ret[0], ret[1]
        except Exception:
            # _loc unavailable — use global to advance jd only; skip adding (visibility unverified)
            try:
                ret = swe.lun_eclipse_when(jd, swe.FLG_SWIEPH)
                etype, tret = ret[0], ret[1]
            except Exception:
                break
            if etype < 0:
                break
            jd = tret[0] + 20
            continue  # cannot verify local visibility — do not add to avoid false Grahan Dosha
        if etype < 0:
            break
        madhya_jd = tret[0]
        if madhya_jd <= jd or madhya_jd >= jd_end:
            break
        if etype > 0:
            eclipses.append(_parse_eclipse(tret, False, etype))
        jd = madhya_jd + 20

    return eclipses


def _eclipse_conflict_note(festival_name: str, ec: dict) -> str:
    """Generate a festival-specific Grahan conflict note with exact timings."""
    etype    = ec["type"]
    sparsha  = ec["sparsha_time"]
    madhya   = ec["madhya_time"]
    moksha   = ec["moksha_time"]
    sutak    = ec.get("sutak_start")
    sutak_h  = ec["sutak_hrs"]
    has_sutak = sutak_h > 0

    timing   = f"Sparsha {sparsha} → Madhya {madhya} → Moksha {moksha}"
    sutak_s  = f"Sutak starts {sutak} ({sutak_h}h before Sparsha)" if has_sutak else "No Sutak (Penumbral)"
    after    = f"after Moksha ({moksha}) + purification bath"

    name = festival_name
    if name == "Holika Dahan":
        return (
            f"🌑 {etype} on Phalguna Purnima — Holika Dahan DIRECTLY AFFECTED. "
            f"{timing}. {sutak_s}. "
            "Per Dharmasindhu: Holika Dahan must be performed during Pradosh (evening twilight) "
            f"AFTER Sutak ends but BEFORE Bhadra. If eclipse covers the entire Pradosh window, "
            f"perform at next sunrise Pradosh per local panchangam guidance."
        )
    elif name == "Raksha Bandhan":
        return (
            f"🌑 {etype} on Shravana Purnima — Raksha Bandhan timing AFFECTED. "
            f"{timing}. {sutak_s}. "
            f"Rakhi must NOT be tied during Sutak or eclipse window. "
            f"Tie Rakhi BEFORE Sutak ({sutak}) or {after}. "
            "Avoid Bhadra period too — see Bhadra timing above."
        )
    elif name == "Guru Purnima":
        return (
            f"🌑 {etype} on Ashadha Purnima — Guru Purnima affected. "
            f"{timing}. {sutak_s}. "
            f"Complete Pada Puja and Guru Vandana BEFORE Sutak ({sutak}). "
            f"Alternatively, re-perform {after}. "
            "Guru Vandana during eclipse earns elevated spiritual merit."
        )
    elif name == "Buddha Purnima":
        return (
            f"🌑 {etype} on Vaishakha Purnima — Buddha Purnima affected. "
            f"{timing}. {sutak_s}. "
            f"Bodhi Vandana should be before Sutak or {after}. "
            "Meditation during eclipse yields amplified merit."
        )
    elif name == "Hanuman Jayanti":
        return (
            f"🌑 {etype} on Chaitra Purnima — Hanuman Jayanti affected. "
            f"{timing}. {sutak_s}. "
            f"Hanuman Chalisa and puja before Sutak ({sutak}) or {after}. "
            "Reciting Hanuman Chalisa during eclipse is exceptionally meritorious."
        )
    elif name == "Diwali":
        return (
            f"🌑 {etype} on Kartik Amavasya — Diwali / Lakshmi Puja AFFECTED. "
            f"{timing}. {sutak_s}. "
            f"Lakshmi Puja must NOT be in Sutak window. "
            f"Perform puja BEFORE Sutak ({sutak}) or {after}. "
            "Re-light diyas after purification."
        )
    elif name == "Maha Shivaratri":
        return (
            f"🌑 {etype} near Maha Shivaratri. "
            f"{timing}. {sutak_s}. "
            "Per Shaiva tradition: Shiva Abhishek during eclipse is permitted and deeply meritorious. "
            f"Night vigil (jaagran) continues. Main puja {after} is ideal if possible."
        )
    elif name == "Ganesh Chaturthi":
        return (
            f"🌑 {etype} on/near Ganesh Chaturthi. "
            f"{timing}. {sutak_s}. "
            f"Ganesh Sthapana (idol installation) should be BEFORE Sutak ({sutak}) or {after}. "
            "Do not begin Ganeshotsav during eclipse window."
        )
    elif name in ("Navratri", "Sharad Navratri", "Chaitra Navratri"):
        return (
            f"🌑 {etype} during {name}. "
            f"{timing}. {sutak_s}. "
            f"Devi Puja before Sutak ({sutak}) or {after}. "
            "Eclipse during Navratri intensifies spiritual energy — heightened meditation recommended."
        )
    elif name == "Mahalaya":
        return (
            f"🌑 {etype} on Mahalaya Amavasya — eclipse on ancestor worship day. "
            f"{timing}. {sutak_s}. "
            f"Pitru Tarpan: perform BEFORE Sutak ({sutak}) or {after}. "
            "Eclipse during Pitru Paksha is considered highly meritorious for ancestor rites."
        )
    elif name == "Dhanteras":
        return (
            f"🌑 {etype} on Kartik Krishna Trayodashi — Dhanteras affected. "
            f"{timing}. {sutak_s}. "
            f"Dhanvantari Puja and purchases: before Sutak ({sutak}) or {after}. "
            "Pradosh puja: verify it falls outside eclipse window."
        )
    elif name == "Naraka Chaturdashi":
        return (
            f"🌑 {etype} on Kartik Krishna Chaturdashi — Naraka Chaturdashi affected. "
            f"{timing}. {sutak_s}. "
            f"Abhyang Snan (oil bath) at Arunodaya: before Sutak ({sutak}) or {after}. "
            "Evening Diya lighting: only after Moksha and purification bath."
        )
    elif name == "Karva Chauth":
        return (
            f"🌑 {etype} on Kartik Krishna Chaturthi — Karva Chauth DIRECTLY AFFECTED. "
            f"{timing}. {sutak_s}. "
            "Moon is in eclipse/sutak — sighting eclipsed moon is spiritually significant. "
            f"Break fast ONLY after Moksha ({moksha}) and purification bath. "
            "Consult local panchangam for adjusted moonrise timing."
        )
    elif name == "Kartik Purnima":
        return (
            f"🌑 {etype} on Kartik Purnima — Dev Deepawali/Tripuri Purnima DIRECTLY AFFECTED. "
            f"{timing}. {sutak_s}. "
            f"Deep (lamp) offerings on Ganga: avoid during Sutak. Offer diyas {after}. "
            "Lunar eclipse on this night amplifies spiritual merit — deep meditation recommended."
        )
    elif name in ("Govardhan Puja", "Bhai Dooj", "Rath Yatra"):
        return (
            f"🌑 {etype} on/near {name}. "
            f"{timing}. {sutak_s}. "
            f"Main observance: before Sutak ({sutak}) or {after}."
        )
    elif name == "Dussehra":
        return (
            f"🌑 {etype} on Vijaya Dashami — Dussehra affected. "
            f"{timing}. {sutak_s}. "
            f"Ravan Dahan should be timed to avoid Sutak window. "
            f"If unavoidable, perform {after}."
        )
    elif name == "Akshaya Tritiya":
        return (
            f"🌑 {etype} on Akshaya Tritiya — rare event. "
            f"{timing}. {sutak_s}. "
            f"Gold purchases and new ventures: before Sutak ({sutak}) or {after}. "
            "Akshaya Tritiya retains its Swayamsiddha (self-illumined) quality even with eclipse."
        )
    elif name == "Krishna Janmashtami":
        return (
            f"🌑 {etype} on Bhadrapada Krishna Ashtami — Janmashtami affected. "
            f"{timing}. {sutak_s}. "
            "Midnight Krishna birth celebration during eclipse is considered highly auspicious. "
            f"Formal puja: before Sutak or {after}."
        )
    elif name == "Ram Navami":
        return (
            f"🌑 {etype} on Chaitra Shukla Navami — Ram Navami affected. "
            f"{timing}. {sutak_s}. "
            f"Ram Navami Abhishek before Sutak ({sutak}) or {after}. "
            "Ramayana paath during eclipse earns amplified merit."
        )
    elif name == "Chhath Puja":
        return (
            f"🌑 {etype} near Chhath Puja — Surya Arghya timing affected. "
            f"{timing}. {sutak_s}. "
            "Surya Arghya should NOT be given during Sutak or eclipse window. "
            f"Adjust to {after} at next sunrise if eclipse covers Arghya time."
        )
    elif name == "Nag Panchami":
        return (
            f"🌑 {etype} on Nag Panchami. "
            f"{timing}. {sutak_s}. "
            f"Naga Puja before Sutak ({sutak}) or {after}. "
            "Milk offerings to snake idols: avoid during Sutak."
        )
    elif name == "Dev Uthani Ekadashi":
        return (
            f"🌑 {etype} on Prabodhini Ekadashi — Dev Uthani Ekadashi affected. "
            f"{timing}. {sutak_s}. "
            f"Vishnu awakening ceremony before Sutak ({sutak}) or {after}. "
            "Wedding season opening: consult panchangam before scheduling."
        )
    elif name in ("Ugadi / Gudi Padwa", "Vasant Panchami"):
        return (
            f"🌑 {etype} on {name}. "
            f"{timing}. {sutak_s}. "
            f"Puja and new beginnings should be before Sutak ({sutak}) or {after}."
        )
    else:
        return (
            f"🌑 {etype} on/near {name} ({ec['date']}). "
            f"{timing}. {sutak_s}. "
            f"Avoid main puja during Sutak window. Perform {after} per tradition."
        )


def _find_eclipses_near(festival_date: str, eclipses: List[dict], window_days: int = 1) -> List[dict]:
    """Return all eclipses within ±window_days of the festival date."""
    try:
        fd = date.fromisoformat(festival_date)
    except Exception:
        return []
    result = []
    for ec in eclipses:
        try:
            ed = date.fromisoformat(ec["date"])
            if abs((ed - fd).days) <= window_days:
                result.append(ec)
        except Exception:
            pass
    return result

# ─── Adhik Maas (Malmas) detection ─────────────────────────────────────────

def _signed_moon_sun(jd: float) -> float:
    """Moon-Sun elongation normalized to [-180, +180). Zero-crossing = new moon."""
    return (_moon_sun_diff(jd) + 180.0) % 360.0 - 180.0

def _find_all_new_moons(jd_start: float, jd_end: float) -> list:
    """
    Scan jd_start..jd_end (daily steps) and return list of new moon JDs.
    Uses signed zero-crossing (robust) instead of >350/<10 heuristic.
    """
    new_moons = []
    jd   = jd_start
    prev = _signed_moon_sun(jd)
    while jd < jd_end:
        jd += 1.0
        cur = _signed_moon_sun(jd)
        # New moon (Amavasya) = signed elongation crosses from <=0 to >0
        if prev <= 0 and cur > 0:
            lo, hi = jd - 1.0, jd
            for _ in range(40):
                mid = (lo + hi) / 2.0
                if _signed_moon_sun(mid) > 0:
                    hi = mid   # crossing is before mid → narrow from right
                else:
                    lo = mid   # crossing is after mid → narrow from left
            new_moons.append((lo + hi) / 2.0)
            jd += 25.0  # skip ahead past the synodic period
        prev = cur
    return new_moons


def _get_lunar_month_name(jd: float, tz: float, lunar_system: str) -> Optional[str]:
    """Return the lunar month name containing the given JD.

    A lunar month is named for the rashi the sun ENTERS during that month
    (the solar sankranti within the month).  We detect this by comparing the
    sun's rashi at the start of the month (prev new moon) vs near the end
    (next new moon - 1 day).  This handles months where the sankranti falls
    in the second half (e.g. Magha: sun enters Aquarius around day 26).

    Adhik Maas: no sankranti → rashi unchanged → returns the same name
    (month is named "Adhik <same>" which the caller uses correctly).
    """
    try:
        _init()
        new_moons = _find_all_new_moons(jd - 40.0, jd + 40.0)
        prev_nm: Optional[float] = None
        next_nm: Optional[float] = None
        for nm in new_moons:
            if nm <= jd:
                prev_nm = nm
            elif next_nm is None:
                next_nm = nm
                break
        if prev_nm is None:
            return None
        if next_nm is None:
            next_nm = prev_nm + 29.5
        names = PURNIMANTA_MONTH_NAMES if lunar_system == "purnimanta" else LUNAR_MONTH_NAMES
        rashi_start = int(_sun_lon_sid(prev_nm) / 30) % 12
        # Scan daily through the month to find any solar rashi transition.
        # Using a single point (e.g. next_nm - 1.0) is fragile when the sankranti
        # falls on the last day of the month (e.g. Vrishabha sankranti ~May 15 for
        # Vaishakha 2026 when next_nm is May 16).
        check = prev_nm + 1.0
        while check < next_nm:
            r = int(_sun_lon_sid(check) / 30) % 12
            if r != rashi_start:
                return names[r]  # Sun entered a new rashi — this is the month's name
            check += 1.0
        # Final check just inside the month end (catches late-month sankrantis)
        r = int(_sun_lon_sid(next_nm - 0.1) / 30) % 12
        if r != rashi_start:
            return names[r]
        # No rashi transition → Adhik Maas (sun stayed in same rashi)
        return names[rashi_start]
    except Exception:
        return None


def _search_nirmala(base_jd: float, target_idx: int) -> float:
    """Find next occurrence of target tithi after base_jd (used by improved Adhik logic)."""
    for delta in sorted(range(-12, 13), key=abs):
        for sub_h in [0, 6, 12, 18]:
            t = base_jd + float(delta) + sub_h / 24.0
            if _tithi_idx(t) == target_idx:
                return t
    return base_jd


_adhika_cache: Dict[Tuple[int, float], Any] = {}

def _get_adhika_masa_range(year: int, tz: float) -> Optional[Tuple[float, float]]:
    """
    Returns (start_jd, end_jd) of the Adhika (intercalary) lunar month in `year`,
    or None if no Adhika month exists that year.
    An Adhika month is a new-moon-to-new-moon interval with NO solar rashi change.
    Keyed by (year, tz) because tz affects which calendar year a new moon belongs to.
    """
    key = (year, round(tz, 2))
    if key in _adhika_cache:
        return _adhika_cache[key]
    _init()   # ensures Lahiri sidereal mode is set before _sun_lon_sid calls
    try:
        jd_start = swe.julday(year, 1, 1, 0.0) - 35.0
        jd_end   = swe.julday(year + 1, 1, 1, 0.0) + 35.0
        new_moons = _find_all_new_moons(jd_start, jd_end)
        result = None
        for i in range(len(new_moons) - 1):
            nm_s = new_moons[i]
            nm_e = new_moons[i + 1]
            rashi_s = int(_sun_lon_sid(nm_s) / 30) % 12
            rashi_e = int(_sun_lon_sid(nm_e - 0.1) / 30) % 12
            if rashi_s == rashi_e:
                # Adhika month found — check it falls within the requested year
                y_nm, _, _, _ = _jd_to_parts(nm_s, tz)
                if y_nm == year:
                    result = (nm_s, nm_e)
                    break
        _adhika_cache[key] = result
        return result
    except Exception:
        log.exception("Adhika masa detection failed for year %d", year)
        _adhika_cache[key] = None
        return None


def _get_adhik_maas_note(year: int, tz: float, lunar_system: str = "amanta") -> Optional[str]:
    """
    Detect Adhik Maas for the year.
    A lunar month (new moon to new moon) with no solar Sankranti is Adhik.
    Returns a descriptive note or None.
    Uses PURNIMANTA_MONTH_NAMES when lunar_system="purnimanta".
    """
    try:
        rng = _get_adhika_masa_range(year, tz)
        if rng is None:
            return None
        nm_s, nm_e = rng
        rashi_s = int(_sun_lon_sid(nm_s) / 30) % 12
        names = PURNIMANTA_MONTH_NAMES if lunar_system == "purnimanta" else LUNAR_MONTH_NAMES
        month_name = names[rashi_s]
        y_nm, m_nm, d_nm, _ = _jd_to_parts(nm_s, tz)
        end_date = _jd_to_date_str(nm_e, tz)
        return (
            f"Adhik {month_name} Maas (Malmas) this year — begins {y_nm}-{m_nm:02d}-{d_nm:02d}, "
            f"ends {end_date}. Festivals in this month are not observed on their regular dates "
            "per most traditions. Consult local panchangam."
        )
    except Exception:
        return None


_kshaya_cache: Dict[Tuple[int, float], Any] = {}

def _get_kshaya_masa_range(year: int, tz: float) -> Optional[Tuple[float, float]]:
    """
    Returns (start_jd, end_jd) of the Kshaya (expunged/decayed) lunar month in `year`,
    or None if none exists.

    Kshaya Maas = a lunar month where the Sun transits through TWO zodiac signs
    (rashi_diff == 2). This happens near perihelion (Jan) when the Sun moves fastest.
    Extremely rare: last 1983, next 2124.
    Keyed by (year, tz) because tz affects which calendar year a new moon belongs to.
    """
    key = (year, round(tz, 2))
    if key in _kshaya_cache:
        return _kshaya_cache[key]
    _init()   # ensures Lahiri sidereal mode is set before _sun_lon_sid calls
    try:
        jd_start = swe.julday(year, 1, 1, 0.0) - 35.0
        jd_end   = swe.julday(year + 1, 1, 1, 0.0) + 35.0
        new_moons = _find_all_new_moons(jd_start, jd_end)
        result = None
        for i in range(len(new_moons) - 1):
            nm_s = new_moons[i]
            nm_e = new_moons[i + 1]
            rashi_s = int(_sun_lon_sid(nm_s) / 30) % 12
            rashi_e = int(_sun_lon_sid(nm_e - 0.1) / 30) % 12
            rashi_diff = (rashi_e - rashi_s) % 12
            if rashi_diff == 2:       # Sun crossed TWO rashis → Kshaya
                y_nm, _, _, _ = _jd_to_parts(nm_s, tz)
                if y_nm == year:
                    result = (nm_s, nm_e)
                    break
        _kshaya_cache[key] = result
        return result
    except Exception:
        log.exception("Kshaya masa detection failed for year %d", year)
        _kshaya_cache[key] = None
        return None


def _get_kshaya_masa_note(year: int, tz: float, lunar_system: str = "amanta") -> Optional[str]:
    """
    Returns a human-readable note if a Kshaya (expunged) month occurs this year.
    Kshaya years almost always contain TWO Adhika months (before and after the Kshaya).
    Uses PURNIMANTA_MONTH_NAMES when lunar_system="purnimanta".
    """
    try:
        rng = _get_kshaya_masa_range(year, tz)
        if rng is None:
            return None
        nm_s, nm_e = rng
        rashi_s    = int(_sun_lon_sid(nm_s) / 30) % 12
        # Kshaya month is named after the month that disappears
        # The missing name is the month between rashi_s and rashi_s+2
        missing_rashi = (rashi_s + 1) % 12
        names = PURNIMANTA_MONTH_NAMES if lunar_system == "purnimanta" else LUNAR_MONTH_NAMES
        missing_name  = names[missing_rashi]
        y_nm, m_nm, d_nm, _ = _jd_to_parts(nm_s, tz)
        end_date = _jd_to_date_str(nm_e, tz)
        return (
            f"⚠️ Kshaya {missing_name} Maas this year — the month of {missing_name} is expunged "
            f"({y_nm}-{m_nm:02d}-{d_nm:02d} to {end_date}). "
            f"Sun crosses two rashis within this lunar month. "
            f"Festivals of {missing_name} are merged into adjacent months per Dharmashastra. "
            "This is an extremely rare event (last: 1983, next: 2124). "
            "Consult authoritative regional panchangam for exact observances."
        )
    except Exception:
        return None


def _skip_adhika_if_needed(found_jd: float, target_idx: int,
                            adhika_range: Optional[Tuple[float, float]],
                            jd_center: Optional[float] = None,
                            expected_month: Optional[str] = None,
                            tz: Optional[float] = None) -> float:
    """
    Skip to the Nirmala month when found_jd landed in the Adhika month.

    Case 1 — found_jd is INSIDE the Adhika month:
      Try both directions (±29.5 days). Pick the candidate whose lunar month
      matches expected_month. If expected_month is unknown, fall back to forward.

    Case 2 — found_jd is in the month immediately AFTER the Adhika month, but the
      festival belongs to a later month (the Adhika shifted the whole calendar forward).
      Detected when: found_jd is AFTER Adhika ends, is BEFORE the approx center by
      more than 5 days, and the Adhika month ends before the approx center.
    """
    if adhika_range is None:
        return found_jd
    am_start, am_end = adhika_range

    def _search_nirmala(base_jd: float) -> float:
        for delta in sorted(range(-12, 13), key=abs):
            for sub_h in [0, 6, 12, 18]:
                t = base_jd + float(delta) + sub_h / 24.0
                if _tithi_idx(t) == target_idx:
                    return t
        return found_jd  # fallback: keep original

    # Case 1: found directly inside the Adhika month (±3-day boundary buffer)
    if am_start - 3 <= found_jd <= am_end + 3:
        if expected_month and tz is not None:
            # Try both directions; pick the one whose month matches expected_month
            for delta in (-29.5, 29.5):
                candidate = _search_nirmala(found_jd + delta)
                if _get_lunar_month_name(candidate, tz, "amanta") == expected_month:
                    return candidate
        # Fallback: forward (traditional Nirmala-after-Adhik order)
        return _search_nirmala(found_jd + 29.5)

    # Case 2: festival is in a month AFTER the Adhika month, but found one cycle too early.
    # The Adhika month pushes all subsequent months ~29.5 days later than in a normal year.
    # Conditions (all must hold):
    #  a) Adhika ends before the expected center (festival is after the Adhika month)
    #  b) found_jd is after the Adhika ended (not a pre-Adhika festival)
    #  c) found_jd is meaningfully before center (>5 days) — it found the wrong occurrence
    if (jd_center is not None
            and am_end < jd_center          # (a) Adhika before festival
            and found_jd > am_end           # (b) found is post-Adhika
            and found_jd < jd_center - 5): # (c) found is before expected center
        return _search_nirmala(found_jd + 29.5)

    return found_jd

# ─── Rule helpers ──────────────────────────────────────────────────────────

def _find_sunrise_date_in_tithi(start_jd: float, end_jd: float, lat: float, lon: float, tz: float) -> Tuple[int, int, int]:
    """Return the civil date where sunrise falls inside the tithi (Udaya tithi rule)."""
    y, m, d = _jd_to_parts(start_jd, tz)[:3]
    for off in range(-1, 3):
        td = date(y, m, d) + timedelta(days=off)
        sr_jd = _actual_sunrise_jd(td.year, td.month, td.day, lat, lon, tz)
        if start_jd <= sr_jd <= end_jd:
            return td.year, td.month, td.day
    return _jd_to_parts(start_jd, tz)[:3]  # fallback


def _rule_to_idx(paksha: str, num: int) -> int:
    return (num - 1) if paksha == "S" else (14 + num)

# ─── Festival definitions ──────────────────────────────────────────────────

FESTIVAL_RULES: List[Dict[str, Any]] = [
    {"name": "Makar Sankranti",     "hindi": "मकर संक्रांति",       "icon": "🪁",
     "deity": "Surya",          "month": "Pausha",
     "significance": "Sun enters Capricorn (Makara rashi). Harvest festival. Day of charity, kite flying, and sesame offerings.",
     "rule": {"type": "solar", "lon": 270.0, "approx_month": 1}},

    {"name": "Pongal",               "hindi": "पोंगल",                "icon": "🏺",
     "deity": "Surya / Indra",   "month": "Thai",
     "significance": "Sun enters Capricorn. Tamil Nadu 4-day harvest festival: Bhogi, Thai Pongal, Mattu Pongal, Kaanum Pongal. Sweet rice cooked in new pots until it boils over ('Pongal!'). Also Uzhavar Thirunal (Farmers' Day).",
     "fast_note": None,
     "rule": {"type": "solar", "lon": 270.0, "approx_month": 1}},

    {"name": "Lohri",               "hindi": "लोहड़ी",                "icon": "🎆",
     "deity": "Agni / Sun",      "month": "Pausha",
     "significance": "Eve of Makar Sankranti. Punjab and North India harvest and bonfire festival. Sesame (til), revri, gachak and popcorn offered into the bonfire (Agni puja). Folk songs (Sundri Mundri) and Bhangra dance celebrate the winter harvest.",
     "fast_note": None,
     "rule": {"type": "day_before", "ref": "Makar Sankranti"}},

    {"name": "Vasant Panchami",     "hindi": "वसंत पंचमी",          "icon": "🌸",
     "deity": "Saraswati",      "month": "Magha",
     "significance": "Magha Shukla Panchami. Worship of Goddess Saraswati. Start of spring season. Yellow attire worn.",
     "fast_note": "Light fast (optional): Avoid non-vegetarian. Yellow food (rice, laddoo) offered to Saraswati. Break after morning puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 5, "approx_month": 1, "approx_day": 26}},

    {"name": "Ratha Saptami",        "hindi": "रथ सप्तमी",             "icon": "☀️",
     "deity": "Surya",           "month": "Magha",
     "significance": "Magha Shukla Saptami. Sun's birthday (Surya Jayanti) and northward chariot turn. Aruna Saptami Snan (sunrise bath with Arka leaves placed on body). Surya Puja with red flowers. Celebrated at Tirupati and across South India.",
     "fast_note": "Vrat: Sunrise bath with 7 Arka leaves on body. Offer Argya (water) to rising Sun with Surya Gayatri. Red flowers and red sandalwood to Surya idol. Break fast after morning puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 7, "approx_month": 2, "approx_day": 4}},

    {"name": "Phulera Dooj",         "hindi": "फुलेरा दूज",             "icon": "🌺",
     "deity": "Radha / Krishna",  "month": "Phalguna",
     "significance": "Phalguna Shukla Dvitiya. Braj festival — Krishna sports with flowers (phul) before Holi. Considered fully auspicious (Swayamsiddha) like Akshaya Tritiya. New beginnings and auspicious works can be started without muhurta today.",
     "fast_note": None,
     "rule": {"type": "tithi", "paksha": "S", "num": 2, "approx_month": 3, "approx_day": 5}},

    {"name": "Amalaki Ekadashi",     "hindi": "आमलकी एकादशी",          "icon": "🌿",
     "deity": "Lord Vishnu",     "month": "Phalguna",
     "significance": "Phalguna Shukla Ekadashi. Vishnu puja under Amla (Indian gooseberry) tree. Amla is considered Vishnu's form. Breaking fast on Amla fruit earns same merit as gold charity. Circumambulation of Amla tree is traditional.",
     "fast_note": "Ekadashi Vrat: No grains. Amla Puja and Vishnu Sahasranama. Break fast on Dvadashi with Amla prasad.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 3, "approx_day": 12, "special": "ekadashi_viddha"}},

    {"name": "Maha Shivaratri",     "hindi": "महाशिवरात्रि",         "icon": "🔱",
     "deity": "Lord Shiva",     "month": "Magha",
     "significance": "Phalguna Krishna Chaturdashi. Night of Lord Shiva. Fasting, night vigil (jaagran), and Shiva abhishek. Observed when Chaturdashi covers Nishita Kaal (midnight).",
     "fast_note": "Vrat: Nirjala (no water) or phalahari fast from today's sunrise through the night. Perform 4 Prahar Shiva Abhishek every 3 hours. Break fast (parana) at sunrise next morning after Chaturdashi ends — take bath first.",
     "rule": {"type": "tithi", "paksha": "K", "num": 14, "approx_month": 2, "approx_day": 16, "date_from_start": True}},

    {"name": "Holika Dahan",        "hindi": "होलिका दहन",           "icon": "🔥",
     "deity": "Vishnu",         "month": "Phalguna",
     "significance": "Phalguna Purnima Pradosh. Burning of Holika effigy. Victory of Prahlad's devotion over evil. Must be performed during Pradosh, after Bhadra ends if present.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 3, "approx_day": 13, "special": "bhadra_check", "pradosh_date": True, "bhadra_shift": True, "no_vridhi_shift": True}},

    {"name": "Holi",                "hindi": "होली",                 "icon": "🎨",
     "deity": "Krishna",        "month": "Phalguna",
     "significance": "Day after Holika Dahan (Phalguna Purnima+1). Festival of colors. Joy, renewal, and bhang.",
     "rule": {"type": "day_after", "ref": "Holika Dahan"}},

    {"name": "Ugadi / Gudi Padwa",  "hindi": "उगादी / गुड़ी पाड़वा",  "icon": "🏴",
     "deity": "Brahma",         "month": "Chaitra",
     "significance": "Chaitra Shukla Pratipada. Hindu Lunar New Year. New beginnings, special pachadi (six-taste dish) consumed.",
     "rule": {"type": "tithi", "paksha": "S", "num": 1, "approx_month": 3, "approx_day": 20}},

    {"name": "Chaitra Navratri",     "hindi": "चैत्र नवरात्रि",         "icon": "🌸",
     "deity": "Durga / Devi",    "month": "Chaitra",
     "significance": "Chaitra Shukla Pratipada to Navami. Spring Navratri (Vasanta Navratri). Nine nights of Devi worship coinciding with Hindu New Year. Ghat Sthapana on Pratipada. Kanya Puja on Ashtami or Navami. Ends on Ram Navami.",
     "fast_note": "9-day Navratri Vrat: Phalahari diet (fruits, milk, sabudana — no grains). Ghat Sthapana on day 1. Akhand Jyot (continuous flame). Kanya Pujan on Ashtami/Navami. Break fast on Ram Navami.",
     "rule": {"type": "tithi_range", "paksha": "S", "start": 1, "end": 9, "approx_month": 3, "approx_day": 25}},

    {"name": "Baisakhi",             "hindi": "बैसाखी",                "icon": "🌾",
     "deity": "Surya / Vishnu",  "month": "Vaishakha",
     "significance": "Sun enters Aries (Mesha Rashi). Multi-regional harvest festival: Baisakhi (Punjab Rabi crop, Khalsa Panth founding 1699), Vishu (Kerala), Puthandu (Tamil New Year), Bohag Bihu (Assam New Year), Ugadi (Telugu/Kannada — nearby). Golden Temple celebrations, Bhangra dance.",
     "fast_note": None,
     "rule": {"type": "solar", "lon": 0.0, "approx_month": 4}},

    {"name": "Sheetala Ashtami",     "hindi": "शीतला अष्टमी",           "icon": "🏺",
     "deity": "Sheetala Mata",   "month": "Phalguna",
     "significance": "Phalguna Krishna Ashtami (Basoda) — 8 days after Holika Dahan. Worship of Sheetala Mata (goddess of cooling and pox). Cold food cooked the previous day is offered — no fire lit today. Celebrated in UP, Rajasthan, Punjab, Haryana. Women carry cool water pots to her shrine.",
     "fast_note": "Vrat: No cooking on fire today (Basoda = cold food day). Eat only food prepared yesterday. Offer cold curd, rice, halwa to Sheetala Mata. Break fast at sunset.",
     "rule": {"type": "tithi", "paksha": "K", "num": 8, "approx_month": 3, "approx_day": 12}},

    {"name": "Ram Navami",          "hindi": "राम नवमी",             "icon": "🏹",
     "deity": "Lord Rama",      "month": "Chaitra",
     "significance": "Chaitra Shukla Navami. Birthday of Lord Rama at noon (Abhijit Muhurta). Ramayana recitation and Ram Navami puja.",
     "fast_note": "Vrat observed on Ram Navami (this date). Break fast at noon (Abhijit Muhurta) with Ram Navami prasad — Lord Rama's birth time. Ramayana paath and Ram Naam Japa throughout the day.",
     "rule": {"type": "tithi", "paksha": "S", "num": 9, "approx_month": 3, "approx_day": 28, "no_vridhi_shift": True, "special": "madhyahna_note", "date_from_start": True}},

    {"name": "Hanuman Jayanti",     "hindi": "हनुमान जयंती",         "icon": "🐒",
     "deity": "Hanuman",        "month": "Chaitra",
     "significance": "Chaitra Purnima. Birth of Lord Hanuman. Recitation of Hanuman Chalisa, processions, and oil lamp offerings.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 4, "approx_day": 12}},

    {"name": "Akshaya Tritiya",     "hindi": "अक्षय तृतीया",         "icon": "✨",
     "deity": "Vishnu/Lakshmi", "month": "Vaishakha",
     "significance": "Vaishakha Shukla Tritiya. Most auspicious self-illumined day (Swayamsiddha Muhurta). Best for gold purchase, new ventures, Pitra Tarpan.",
     "rule": {"type": "tithi", "paksha": "S", "num": 3, "approx_month": 4, "approx_day": 20, "no_vridhi_shift": True, "date_from_start": True}},

    {"name": "Parshuram Jayanti",    "hindi": "परशुराम जयंती",          "icon": "⚔️",
     "deity": "Lord Parshuram",  "month": "Vaishakha",
     "significance": "Vaishakha Shukla Tritiya. Birthday of Lord Parshuram (6th Vishnu avatar). Coincides with Akshaya Tritiya — same self-illumined (Swayamsiddha) tithi. Parshuram is the only immortal (Chiranjeevi) Vishnu avatar. Observed with Parshuram Puja and fast.",
     "fast_note": "Fast until Parshuram Puja. Offer tulsi, curd, honey. Parshuram Stotram recitation.",
     "rule": {"type": "tithi", "paksha": "S", "num": 3, "approx_month": 4, "approx_day": 20}},

    {"name": "Buddha Purnima",      "hindi": "बुद्ध पूर्णिमा",       "icon": "🪷",
     "deity": "Lord Buddha",    "month": "Vaishakha",
     "significance": "Vaishakha Purnima. Commemorates birth, enlightenment (Bodhi), and Parinirvana of Gautama Buddha on the same full moon.",
     "fast_note": "Observance (optional): Avoid non-veg and alcohol. Dana (charity) and meditation yield amplified merit. Some traditions observe Upavasatha (Buddhist fasting day).",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 5, "approx_day": 12}},

    {"name": "Narasimha Jayanti",    "hindi": "नरसिंह जयंती",           "icon": "🦁",
     "deity": "Lord Narasimha",  "month": "Vaishakha",
     "significance": "Vaishakha Shukla Chaturdashi. Birthday of Narasimha avatar (half-man, half-lion, 4th Vishnu avatar). Appeared at twilight (Sandhya) to destroy Hiranyakashipu. Twilight puja at sunset (Pradosh) is especially auspicious. Narasimha Kavacham recitation.",
     "fast_note": "Vrat: Nirjala or phalahari fast. Narasimha Puja at twilight. Narasimha Kavacham and Stotram. Break fast after sunset puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 14, "approx_month": 5, "approx_day": 5}},

    {"name": "Nirjala Ekadashi",     "hindi": "निर्जला एकादशी",         "icon": "💧",
     "deity": "Lord Vishnu",     "month": "Jyeshtha",
     "significance": "Jyeshtha Shukla Ekadashi (Bhima Ekadashi / Pandava Ekadashi). Most important Ekadashi — observing it gives merit of all 24 annual Ekadashis. Nirjala = without even water. Starts at sunrise and ends at next sunrise.",
     "fast_note": "Vrat: Absolute Nirjala fast — no food, no water from sunrise to next dawn. Only Achamana (ceremonial sip) permitted. Vishnu Sahasranama recitation. Break fast (Parana) on Dvadashi within prescribed time window.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 5, "approx_day": 28, "special": "ekadashi_viddha"}},

    {"name": "Vat Savitri Purnima",  "hindi": "वट सावित्री पूर्णिमा",   "icon": "🌳",
     "deity": "Savitri / Satyavan", "month": "Jyeshtha",
     "significance": "Jyeshtha Purnima. Married women observe Vat Savitri Vrat under the banyan tree, praying for husband's long life — just as Savitri won back Satyavan from Yama. Observed in North India (UP, Bihar, Bengal, Orissa). Maharashtra/Gujarat observe on Jyeshtha Amavasya.",
     "fast_note": "Vrat: Married women fast from sunrise. Circumambulate the Vat (banyan) tree 108 times, tying thread while reciting Savitri-Satyavan story. Break fast at moonrise after puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 6, "approx_day": 11}},

    {"name": "Shani Jayanti",        "hindi": "शनि जयंती",              "icon": "🪐",
     "deity": "Lord Shani",      "month": "Jyeshtha",
     "significance": "Jyeshtha Amavasya. Birthday of Lord Shani (Saturn). Shani Puja with sesame oil, black sesame, blue/black flowers. Shani Stotra and Chalisa recitation. Visiting Shani Shingnapur and Shani temples. Also observed as Vat Savitri in Maharashtra and Gujarat.",
     "fast_note": "Vrat: Offer sesame oil, black sesame, blue cloth to Shani. Visit Shani temple. Shani Stotram, Chalisa, and Navagrah Puja. Donate black items to Brahmins.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 6, "approx_day": 20, "date_from_sunrise": True}},

    {"name": "Rath Yatra",           "hindi": "रथ यात्रा",               "icon": "🎡",
     "deity": "Lord Jagannath",  "month": "Ashadha",
     "significance": "Ashadha Shukla Dvitiya. Lord Jagannath's chariot procession from Puri temple to Gundicha temple (9 days). Lord Jagannath, Balabhadra, and Subhadra ride in decorated chariots. Pulling the chariot rope (Rath Dori) is said to give liberation (Moksha). Celebrated in Puri, Odisha and across India.",
     "fast_note": None,
     "rule": {"type": "tithi", "paksha": "S", "num": 2, "approx_month": 6, "approx_day": 21, "date_from_sunrise": True}},

    {"name": "Ashadhi Ekadashi",     "hindi": "आषाढ़ी एकादशी",           "icon": "🔔",
     "deity": "Lord Vishnu",     "month": "Ashadha",
     "significance": "Ashadha Shukla Ekadashi (Deva Shayani Ekadashi / Padma Ekadashi). Lord Vishnu begins 4-month cosmic sleep (Chaturmas). Warkari pilgrimage to Pandharpur (Maharashtra). Marriage ceremonies and auspicious works pause until Dev Uthani Ekadashi.",
     "fast_note": "Ekadashi Vrat: No grains. Vishnu Sahasranama and Chaturmas Vow. Chaturmas begins — austerities (tap, fast, study) continue until Dev Uthani Ekadashi. Break fast on Dvadashi.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 7, "approx_day": 3, "special": "ekadashi_viddha", "date_from_sunrise": True}},

    {"name": "Guru Purnima",        "hindi": "गुरु पूर्णिमा",        "icon": "🙏",
     "deity": "Guru/Vyasa",     "month": "Ashadha",
     "significance": "Ashadha Purnima. Birthday of Maharshi Vyasa (Vyasa Purnima). Honoring the Guru lineage. Meditation and pada puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 7, "approx_day": 18}},

    {"name": "Hariyali Teej",        "hindi": "हरियाली तीज",            "icon": "🌿",
     "deity": "Parvati / Shiva",  "month": "Shravana",
     "significance": "Shravana Shukla Tritiya. Festival of green (Hariyali = greenery). Women pray for happy married life like Parvati-Shiva. Swing (Jhula) hung on trees, folk songs, and green attire (green chuda, green bangle). Celebrated in UP, Rajasthan, Punjab, Bihar.",
     "fast_note": "Vrat: Married women observe Nirjala or phalahari fast. Parvati-Shiva Puja. Hariyali songs and swing (Jhula) rituals. Break fast at sunrise the next day.",
     "rule": {"type": "tithi", "paksha": "S", "num": 3, "approx_month": 7, "approx_day": 20}},

    {"name": "Nag Panchami",        "hindi": "नाग पंचमी",            "icon": "🐍",
     "deity": "Naga Devatas",   "month": "Shravana",
     "significance": "Shravana Shukla Panchami. Worship of serpent gods (Shesha, Vasuki, Takshaka). Milk offerings to snake idols.",
     "fast_note": "Traditional vrat (especially observed by women): Avoid cooking on fire — eat only raw/uncooked food. Break after Naga Puja with milk prasad and sesame.",
     "rule": {"type": "tithi", "paksha": "S", "num": 5, "approx_month": 8, "approx_day": 7, "date_from_sunrise": True}},

    {"name": "Raksha Bandhan",      "hindi": "रक्षाबंधन",            "icon": "🎀",
     "deity": "Indra/Lakshmi",  "month": "Shravana",
     "significance": "Shravana Purnima. Rakhi tied by sister to brother during Aparahna (afternoon). Avoid tying during Bhadra period — inauspicious.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 8, "approx_day": 18, "special": "bhadra_check", "date_from_sunrise": True}},

    {"name": "Hartalika Teej",       "hindi": "हरतालिका तीज",           "icon": "🌸",
     "deity": "Parvati / Shiva",  "month": "Bhadrapada",
     "significance": "Bhadrapada Shukla Tritiya (day before Ganesh Chaturthi). Most important Teej. Parvati's penance — her friend (hartali = kidnapped) took her to forest to prevent forced marriage to Vishnu. Sand Shiva-Parvati idols worshipped through the night. Celebrated in Maharashtra, UP, Bihar, Rajasthan.",
     "fast_note": "Vrat: Extremely strict Nirjala fast (no food, no water) for 24 hours. Hartalika Puja with 16-type worship (Shodashopachara) through the night. Night vigil (jaagran). Break fast at sunrise next morning.",
     "rule": {"type": "tithi", "paksha": "S", "num": 3, "approx_month": 8, "approx_day": 27}},

    {"name": "Krishna Janmashtami", "hindi": "कृष्ण जन्माष्टमी",     "icon": "🦚",
     "deity": "Lord Krishna",   "month": "Shravana",
     "significance": "Bhadrapada Krishna Ashtami. Birthday of Lord Krishna at Nishita Kaal (midnight). Fasting, midnight puja, Dahi Handi.",
     "fast_note": "Vrat: Phalahari (fruits, milk, dry fruits) fast from today's sunrise. Fast continues until midnight (Nishita Kaal) when Lord Krishna was born. Break fast after midnight puja with panchamrit and prasad. Rohini nakshatra active tonight is especially auspicious.",
     "rule": {"type": "tithi", "paksha": "K", "num": 8, "approx_month": 8, "approx_day": 24, "special": "nishita_note"}},

    {"name": "Kajari Teej",          "hindi": "कजरी तीज",               "icon": "🎵",
     "deity": "Parvati / Shiva",  "month": "Shravana",
     "significance": "Bhadrapada Krishna Tritiya. Kajari Teej — harvest festival with Kajari folk songs about monsoon. Neem tree worshipped. Celebrated in Varanasi, Mirzapur, Madhya Pradesh, and Bihar. Women sing Kajari songs through the night.",
     "fast_note": "Vrat: Women fast and worship Neem tree for husband's long life. Kajari folk songs through the night. Break fast after Moon sighting.",
     "rule": {"type": "tithi", "paksha": "K", "num": 3, "approx_month": 8, "approx_day": 14}},

    {"name": "Ganesh Chaturthi",    "hindi": "गणेश चतुर्थी",         "icon": "🐘",
     "deity": "Lord Ganesha",   "month": "Bhadrapada",
     "significance": "Bhadrapada Shukla Chaturthi. Birthday of Ganesha. 10-day festival (Ganeshotsav). Idol installation and Visarjan.",
     "fast_note": "Vrat: Observe Ganesh Chaturthi fast — eat only after Ganesh Sthapana and puja. Modak prasad as break. ⚠️ Strictly avoid looking at the moon tonight (Chandra Darshan is prohibited — causes false accusations per Bhavishya Purana).",
     "rule": {"type": "tithi", "paksha": "S", "num": 4, "approx_month": 9, "approx_day": 1}},

    {"name": "Anant Chaturdashi",    "hindi": "अनंत चतुर्दशी",          "icon": "🐍",
     "deity": "Vishnu / Ananta",  "month": "Bhadrapada",
     "significance": "Bhadrapada Shukla Chaturdashi. End of 10-day Ganeshotsav — Ganesh Visarjan (immersion). Ananta Puja: Lord Vishnu's Ananta (infinite) form worshipped with a 14-knotted silk thread (Ananta Dor) tied on wrist. 14 categories of offerings.",
     "fast_note": "Ananta Vrat: Fast until Ananta Puja. Tie Ananta Dor (14-knotted thread) on right wrist. Offer 14 items of worship. Break fast after puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 14, "approx_month": 9, "approx_day": 8}},

    {"name": "Onam",                "hindi": "ओणम",                   "icon": "🌺",
     "deity": "King Mahabali / Vamana", "month": "Chingam",
     "significance": "Thiruvonam — Moon in Shravana nakshatra during Kerala's Chingam month (Sun in Leo). Kerala's biggest festival. Celebrates King Mahabali's annual visit to his people. Pookalam (flower carpet), Onam Sadya (26-dish feast on banana leaf), Vallam Kali (snake boat race).",
     "fast_note": None,
     "rule": {"type": "nakshatra", "nakshatra": 21, "approx_month": 8, "approx_day": 25}},

    {"name": "Vishwakarma Puja",     "hindi": "विश्वकर्मा पूजा",         "icon": "⚙️",
     "deity": "Lord Vishwakarma", "month": "Bhadrapada",
     "significance": "Sun enters Virgo (Kanya Sankranti). Worship of Lord Vishwakarma, divine architect who built Lanka, Dwarka, and Indraprastha. Tools, machines, vehicles, and workplaces worshipped. Celebrated by craftsmen, engineers, factory workers, and artisans across India.",
     "fast_note": None,
     "rule": {"type": "solar", "lon": 150.0, "approx_month": 9}},

    {"name": "Mahalaya",             "hindi": "महालया",                 "icon": "🙏",
     "deity": "Pitru / Ancestors", "month": "Bhadrapada",
     "significance": "Ashwin Krishna Amavasya (Sarvapitri Amavasya / Pitru Paksha end). Most important day for ancestor worship. Tarpan (water offering) for all ancestors with sesame and kusha grass. In Bengal, Mahalaya marks Devi's descent — Mahishasura Mardini broadcast marks dawn. 16-day Pitru Paksha concludes.",
     "fast_note": "Pitru Tarpan: Offer water with sesame (til) and kusha grass facing south. Pinda Daan at sacred river if possible. Charity (Anna Daan, Vastra Daan) to Brahmins in ancestors' names.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 9, "approx_day": 28}},

    {"name": "Sharad Navratri",     "hindi": "शारद नवरात्रि",         "icon": "🪔",
     "deity": "Durga",          "month": "Ashwin",
     "significance": "Ashwin Shukla Pratipada to Navami. Nine nights of Goddess Durga worship. Garba, Dandiya, fasting, and Kanya Pujan. The most widely celebrated Navratri.",
     "fast_note": "9-day Navratri Vrat: Phalahari diet (fruits, milk, sabudana, sendha namak — no grains). Fast from Pratipada to Navami. Kanya Pujan on Ashtami or Navami. Break on Vijaya Dashami (Dussehra) morning with prasad.",
     "rule": {"type": "tithi_range", "paksha": "S", "start": 1, "end": 9, "approx_month": 10, "approx_day": 5}},

    {"name": "Dussehra",            "hindi": "दशहरा",                "icon": "🏹",
     "deity": "Lord Rama",      "month": "Ashwin",
     "significance": "Ashwin Shukla Dashami (Vijaya Dashami). Victory of Rama over Ravana. Ravan Dahan at sunset. Shami tree worship.",
     "fast_note": "Fast (optional): Some traditions keep Vijaya Dashami vrat. Break at Vijay Muhurta (afternoon, ~3 PM). Shastra puja and Shami tree worship at sunset before Ravan Dahan.",
     "rule": {"type": "tithi", "paksha": "S", "num": 10, "approx_month": 10, "approx_day": 13, "special": "aparahna_note"}},

    {"name": "Karva Chauth",         "hindi": "करवा चौथ",               "icon": "🌙",
     "deity": "Chandra / Shiva-Parvati", "month": "Ashwin",
     "significance": "Kartik Krishna Chaturthi. Married women fast from sunrise to moonrise for husband's long life. Moon sighted through a sieve, then husband's face seen — fast broken. Popular across North India (UP, Punjab, Haryana, Rajasthan, Delhi).",
     "fast_note": "Vrat: Nirjala fast from sunrise to moonrise. Karva Chauth Katha recited in group in the evening. Sight the Moon through sieve using lamp or diya, then see husband's face — break fast with water and food from husband's hands. Karva (clay pot) with sweets given to mother-in-law.",
     "rule": {"type": "tithi", "paksha": "K", "num": 4, "approx_month": 10, "approx_day": 28}},

    {"name": "Ahoi Ashtami",         "hindi": "अहोई अष्टमी",             "icon": "⭐",
     "deity": "Ahoi Mata",       "month": "Ashwin",
     "significance": "Kartik Krishna Ashtami. Mothers fast from sunrise to star-sighting for their sons' well-being. Ahoi Mata (form of Parvati) is worshipped with her 7 sons depicted on wall. Celebrated in UP, Punjab, Haryana, Rajasthan.",
     "fast_note": "Vrat: Mothers keep Nirjala fast from sunrise. Ahoi Mata puja in evening (with image of Ahoi Mata and her 7 sons on wall). Break fast after sighting stars (or moon if stars not visible) — offer Argya.",
     "rule": {"type": "tithi", "paksha": "K", "num": 8, "approx_month": 10, "approx_day": 24}},

    {"name": "Dhanteras",            "hindi": "धनतेरस",                 "icon": "🪙",
     "deity": "Dhanvantari / Kubera / Lakshmi", "month": "Ashwin",
     "significance": "Kartik Krishna Trayodashi (Dhanvantari Jayanti). Lord Dhanvantari (father of Ayurveda) emerged from ocean with nectar. Buying metals (gold, silver, utensils) on this day is auspicious. Lakshmi Puja at Pradosh. Diya lit at entrance to guide Lakshmi home.",
     "fast_note": "Purchase silver, gold, or new utensils for prosperity. Dhanvantari Puja and Lakshmi Puja at Pradosh (sunset). Light Diya at front door in 4 directions (Yamraj Deepak) to ward off untimely death.",
     "rule": {"type": "tithi", "paksha": "K", "num": 13, "approx_month": 11, "approx_day": 2, "pradosh_date": True}},

    {"name": "Diwali",              "hindi": "दीपावली",              "icon": "✨",
     "deity": "Lakshmi/Rama",   "month": "Ashwin",
     "significance": "Kartik Amavasya. Festival of lights. Lakshmi Puja at Pradosh. The entire Amavasya night is auspicious for Diya lighting.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 11, "approx_day": 5, "pradosh_date": True, "no_vridhi_shift": True}},

    {"name": "Naraka Chaturdashi",   "hindi": "नरक चतुर्दशी",           "icon": "🪔",
     "deity": "Krishna / Kali",  "month": "Kartik",
     "significance": "Kartik Krishna Chaturdashi (Choti Diwali / Roop Chaudas). Krishna killed Narakasura demon. Abhyang Snan (oil bath) at Arunodaya (dawn). In South India: oil bath with Shikakai before sunrise is mandatory. In Maharashtra: Roop Chaturdashi — beauty rituals. Diyas lit in evening.",
     "fast_note": "Abhyang Snan: Apply sesame oil, scrub with Shikakai/ubtan before sunrise. Bath while Moon is still visible. Lakshmi and Kali puja in evening.",
     "rule": {"type": "day_before", "ref": "Diwali"}},

    {"name": "Govardhan Puja",       "hindi": "गोवर्धन पूजा",           "icon": "🐄",
     "deity": "Lord Krishna / Indra", "month": "Kartik",
     "significance": "Kartik Shukla Pratipada (day after Diwali). Krishna lifted Govardhan hill to shelter villagers from Indra's floods. Govardhan (cow-dung hill) worshipped. Annakut (mountain of food) offering to Lord. Cows decorated and worshipped. Bali Pratipada (Bali Raja's day) in Maharashtra.",
     "fast_note": "Govardhan Puja: Prepare Annakut (56 types of food) for Krishna. Circumambulate Govardhan hill image. Cows decorated with flowers and lamps.",
     "rule": {"type": "day_after", "ref": "Diwali"}},

    {"name": "Bhai Dooj",            "hindi": "भाई दूज",                "icon": "🤝",
     "deity": "Yama / Yamuna",   "month": "Kartik",
     "significance": "Kartik Shukla Dvitiya (2 days after Diwali). Sisters apply tilak on brother's forehead and pray for his long life. Brothers give gifts. Yama (Death god) visited his sister Yamuna on this day — brothers protected from untimely death. Also Bhai Tika (Nepal), Bhau Beej (Maharashtra).",
     "fast_note": "Bhai Dooj ritual: Sister applies tilak (roli/kumkum), gives sweets and aarti. Brother gives gifts and blessings. Yama Dvitiya fast optional — sisters pray for brothers' long life.",
     "rule": {"type": "day_offset", "ref": "Diwali", "offset": 2}},

    {"name": "Skanda Sashti",        "hindi": "स्कंद षष्ठी",             "icon": "🏹",
     "deity": "Lord Murugan / Kartikeya", "month": "Kartik",
     "significance": "Kartik Shukla Shashthi. Lord Murugan's (Kartikeya) victory over Soorapadman demon (Soora Samharam). 6-day Skanda Sashti fast. Tamil Nadu's most important festival after Pongal. Major celebrations at Tiruchendur, Palani, Swamimalai, and Tiruttani temples.",
     "fast_note": "6-day Skanda Sashti Vrat: From Pratipada to Shashthi (fast each day). Murugan puja morning and evening. Kavadi dance. Break 6-day fast on Soora Samharam day (Shashthi) with Murugan prasad.",
     "rule": {"type": "tithi", "paksha": "S", "num": 6, "approx_month": 11, "approx_day": 8}},

    {"name": "Chhath Puja",         "hindi": "छठ पूजा",              "icon": "🌅",
     "deity": "Surya",          "month": "Kartik",
     "significance": "Kartik Shukla Shashthi. Sun worship with 36-hour fast. Arghya offered to setting sun (Sandhya Arghya) and rising sun (Usha Arghya).",
     "fast_note": "Vrat: Extremely strict 36-hour Nirjala fast (no food, no water). Starts Panchami evening. Sandhya Arghya at sunset today. Nirjala night. Usha Arghya at sunrise next morning. Fast broken only after Usha Arghya. One of Hinduism's most rigorous vrats.",
     "rule": {"type": "tithi", "paksha": "S", "num": 6, "approx_month": 11, "approx_day": 10}},

    {"name": "Dev Uthani Ekadashi", "hindi": "देवउठनी एकादशी",       "icon": "🔔",
     "deity": "Lord Vishnu",    "month": "Kartik",
     "significance": "Kartik Shukla Ekadashi (Prabodhini Ekadashi). Lord Vishnu wakes from Yoganidra after 4 months (Chaturmas ends). Auspicious wedding season begins.",
     "fast_note": "Ekadashi Vrat: No grains (anna) today — fruits, milk, dry fruits only. Vishnu awakening ceremony (Tulsi Vivah) in evening. Break fast (Parana) tomorrow (Dwadashi) between sunrise and Dwadashi end time.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 11, "approx_day": 15,
              "special": "ekadashi_viddha"}},
    {"name": "Tulsi Vivah",          "hindi": "तुलसी विवाह",            "icon": "🪴",
     "deity": "Tulsi / Vishnu",   "month": "Kartik",
     "significance": "Kartik Shukla Dvadashi (or Ekadashi evening to Dvadashi). Ceremonial marriage of Tulsi (holy basil) with Lord Vishnu (Shaligram). Marks end of Chaturmas. Auspicious wedding season officially begins after Tulsi Vivah. Celebrated with full wedding rites.",
     "fast_note": "Perform Tulsi Vivah puja in evening. Tulsi plant decorated as bride. Shaligram or Vishnu image as groom. Marriage ceremony with Vivah mantras. Weddings can now be scheduled.",
     "rule": {"type": "tithi", "paksha": "S", "num": 12, "approx_month": 11, "approx_day": 22}},

    {"name": "Kartik Purnima",       "hindi": "कार्तिक पूर्णिमा",        "icon": "🪔",
     "deity": "Vishnu / Shiva",   "month": "Kartik",
     "significance": "Kartik Shukla Purnima (Tripuri Purnima / Dev Deepawali). Lord Shiva destroyed Tripurasura demons today. Dev Deepawali — Gods descend to bathe in Ganga (Varanasi ghats lit with 10 lakh diyas). Guru Nanak Jayanti (Sikh). Kartik Snan (month-long holy bath) concludes.",
     "fast_note": "Kartik Snan Parana: Complete the month-long Kartik Vrat. Dev Deepawali: Light diyas at Ganga or any water body. Charity and Vishnu Puja. Guru Nanak Jayanti: Akhand Path and Nagar Kirtan.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 11, "approx_day": 25}},

    {"name": "Geeta Jayanti",        "hindi": "गीता जयंती",              "icon": "📖",
     "deity": "Lord Krishna",     "month": "Margashirsha",
     "significance": "Margashirsha Shukla Ekadashi (Mokshada Ekadashi / Vaikunta Ekadashi). Lord Krishna spoke the Bhagavad Gita to Arjuna on Kurukshetra on this day (18 chapters). Vaikunta Dwadashi in South India — gates of Vaikunta open. Gita Jayanti celebrated at Kurukshetra.",
     "fast_note": "Ekadashi Vrat: No grains. Bhagavad Gita recitation (ideally all 18 chapters). Moksha-granting Ekadashi — Vishnu worship with Tulsi. Break fast on Dvadashi (Vaikunta Dvadashi).",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 12, "approx_day": 6, "special": "ekadashi_viddha"}},

    {"name": "Dattatreya Jayanti",   "hindi": "दत्तात्रेय जयंती",         "icon": "🔱",
     "deity": "Lord Dattatreya",  "month": "Margashirsha",
     "significance": "Margashirsha Shukla Purnima. Birthday of Lord Dattatreya — the divine trinity (Brahma+Vishnu+Shiva) in one form with 3 heads and 6 hands. Guru of all gurus. Celebrated at Gangapur, Narsobawadi, and Audumbar in Maharashtra and Karnataka. Dattatreya Stotram and Guru Charitra recitation.",
     "fast_note": "Fast until evening Dattatreya Puja. Dattatreya Stotram, Guru Charitra (Marathi text), and charity to Brahmin/Guru. Offer cooked rice to Dattatreya.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 12, "approx_day": 15}},

    # ─── Chhath Puja — 4-Day Breakdown ──────────────────────────────────────────
    {"name": "Chhath Puja — Nahay Khay", "hindi": "छठ पूजा — नहाय खाय",   "icon": "🛁",
     "deity": "Surya",            "month": "Kartik",
     "significance": "Kartik Shukla Chaturthi. First day of Chhath Puja. Devotees take holy bath (Ganga, river, or pond), wash house, cook single sattvic meal (lauki bhaat — bottle gourd rice). Symbolises purification before the 4-day solar worship.",
     "fast_note": "Partial fast: one meal only (lauki/kaddu + chana dal + rice cooked in bronze/clay vessel, no salt). No onion/garlic. Bath in river/holy water. Rest of family also eats sattvic food.",
     "rule": {"type": "tithi", "paksha": "S", "num": 4, "approx_month": 11, "approx_day": 7}},

    {"name": "Chhath Puja — Kharna",     "hindi": "छठ पूजा — खरना",        "icon": "🍚",
     "deity": "Surya",            "month": "Kartik",
     "significance": "Kartik Shukla Panchami. Second day of Chhath Puja. Kharna — devotees observe full-day nirjala fast (no food, no water). In the evening they prepare kheer (rice pudding with jaggery) and rotis as prasad for Chhathi Maiya. Fast is broken after sunset puja with kheer+roti.",
     "fast_note": "Nirjala fast all day. Evening: cook kheer (rice + jaggery + milk in clay pot on mango-wood fire) and wheat rotis. Offer to Chhathi Maiya, then break fast. Post-Kharna: 36-hour continuous nirjala fast begins for the vrati.",
     "rule": {"type": "tithi", "paksha": "S", "num": 5, "approx_month": 11, "approx_day": 8}},

    {"name": "Chhath Puja — Sandhya Arghya", "hindi": "छठ पूजा — संध्या अर्घ्य", "icon": "🌅",
     "deity": "Surya",            "month": "Kartik",
     "significance": "Kartik Shukla Shashthi. Third and main day of Chhath Puja. Devotees (vrati) carry bamboo baskets (sup/daura) filled with fruits, thekua, sugar cane, and prasad to river/pond bank. Sandhya Arghya (evening offering) is given to the setting sun. Massive community gatherings at ghats.",
     "fast_note": "36-hour Nirjala Vrat continues (started Kharna evening). No food, no water. Reach ghat 2 hours before sunset. Offer Arghya to setting sun with fruits, thekua, coconut, sugarcane in bamboo sup. Standing in water if possible. Return home, keep vigil all night.",
     "rule": {"type": "tithi", "paksha": "S", "num": 6, "approx_month": 11, "approx_day": 9}},

    {"name": "Chhath Puja — Usha Arghya", "hindi": "छठ पूजा — उषा अर्घ्य",   "icon": "🌄",
     "deity": "Surya",            "month": "Kartik",
     "significance": "Kartik Shukla Saptami. Fourth and final day of Chhath Puja. Usha Arghya — devotees return to ghat before sunrise and offer Arghya to the rising sun. This concludes the 36-hour nirjala fast. The vrati finally breaks fast with thekua, ginger, and water. One of the most spiritually powerful moments of Chhath.",
     "fast_note": "Reach ghat well before sunrise. Offer Arghya to rising sun. After Arghya complete: break 36-hour nirjala fast with: 1 small piece of thekua + raw ginger + water first, then full prasad. Distribute thekua, fruits, sugarcane to all.",
     "rule": {"type": "tithi", "paksha": "S", "num": 7, "approx_month": 11, "approx_day": 10}},

    # ─── Regional & South Indian Festivals ──────────────────────────────────────
    {"name": "Vishu",                    "hindi": "विषु",                    "icon": "🌾",
     "deity": "Lord Vishnu / Krishna", "month": "Mesha (Solar)",
     "significance": "Kerala New Year. Sun enters Mesha Rashi (Aries) — astronomical new year (same as Baisakhi / Puthandu). Vishukkani ceremony: early morning the first sight (kani) must be auspicious objects — Vishnu idol, fruits, vegetables, gold, coins, rice, kani konna flowers (Cassia fistula). Vishu Kaineettam (elders gift money to children). Grand fireworks (Vishuppadakam).",
     "fast_note": "No strict vrat. Wake before sunrise. First sight (Vishukkani): look at Vishnu/Krishna idol surrounded by Kani Konna flowers, fruits (jackfruit, cucumber, mango), raw rice, gold, betel leaves, mirror. Light traditional lamp (nilavilakku). Receive Kaineettam from elders.",
     "rule": {"type": "solar", "approx_month": 4, "approx_day": 14}},

    {"name": "Chithra Pournami",         "hindi": "चित्रा पूर्णिमा",         "icon": "🌕",
     "deity": "Chitra Gupta",     "month": "Chaitra",
     "significance": "Chaitra Purnima (Full Moon in Chitra Nakshatra). Chitragupta Puja — worship of Chitragupta who maintains karmic records of all beings. Tamil Nadu's major festival: Chithirai Full Moon. Holy dip in tanks of Madurai Meenakshi temple. Chitragupta — the divine accountant of Yama — is worshipped for removal of sins.",
     "fast_note": "Fast until moonrise. Chitragupta Puja with pen, inkpot, account books (symbolising karmic ledger). Offer: betel nut, flowers, lamp, sweets. Holy bath in river or temple tank. Charity and feeding of poor. Break fast after moonrise puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 4, "approx_day": 12}},

    {"name": "Panguni Uthiram",          "hindi": "पंगुनी उत्तिरम",          "icon": "🌸",
     "deity": "Lord Murugan / Shiva-Parvati", "month": "Phalguna",
     "significance": "Phalguna (Panguni) Purnima with Uttara Phalguni Nakshatra. One of Tamil Nadu's most auspicious festivals. Celestial weddings: Shiva-Parvati, Murugan-Devasena, Murugan-Valli. Celebrated grandly at Tiruchendur, Palani Murugan temples and Madurai Meenakshi temple (Celestial Wedding of Meenakshi-Sundareswarar). Kavadi processions.",
     "fast_note": "Pournami fast until moonrise. Murugan / Shiva-Parvati puja. Visit Murugan or Shiva temple. Kavadi or Pal Kodam (pot of milk) offering. Archana with 108 names. Abishekam (milk, curd, honey, rosewater) on Shivalinga.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 3, "approx_day": 25}},

    {"name": "Thiruvathira",             "hindi": "थिरुवातिरा",               "icon": "💃",
     "deity": "Lord Shiva",       "month": "Margashirsha",
     "significance": "Margashirsha Purnima / Ardra Nakshatra (Thiruvadhirai). Kerala festival dedicated to Lord Shiva. Ardra Nakshatra on Margashirsha Purnima — considered Shiva's birth star. Women (especially married) observe Thiruvathira Vrat for husband's long life. All-night singing and Thiruvathirakali (circle dance). Preparation of kali (rice porridge) and ellu (sesame) dishes.",
     "fast_note": "Women's vrat: fast the day before (Purnima eve). On Thiruvathira: wake early, ritual bath, kolam (rangoli). Fast all day — only fruits or light kali after puja. Thiruvathirakali dance in evening in groups of 8. Break fast after Ardra Nakshatra period ends.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 12, "approx_day": 20}},

    {"name": "Karthigai Deepam",         "hindi": "कार्तिगई दीपम",            "icon": "🔦",
     "deity": "Lord Shiva (Annamalaiyar)", "month": "Kartik",
     "significance": "Kartik Purnima (Karthigai month in Tamil calendar) with Krittika Nakshatra. Tamil Nadu's Festival of Lights — older than Diwali in South India. Shiva appeared as a pillar of fire (Jyoti Lingam) on Tiruvannamalai hill (Annamalai). The beacon fire (Maha Deepam) lit on top of Tiruvannamalai hill is visible for miles. Every home lit with rows of clay lamps (karthigai vilakku). Celebrated on Karthigai Masa Purnima.",
     "fast_note": "Light clay lamps (karthigai vilakku) throughout the house at dusk — rows on steps, roof, courtyard. Special dishes: pori urundai (puffed rice balls), kadalai mittai, appam. Tiruvannamalai Girivalam (circumambulation of Annamalai hill, 14km) on this night. No strict vrat — lamps and puja required.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 11, "approx_day": 27}},

    {"name": "Vara Lakshmi Vratam",      "hindi": "वरलक्ष्मी व्रतम",           "icon": "🪙",
     "deity": "Goddess Lakshmi",  "month": "Shravana",
     "significance": "Shravana Shukla Friday (last/second-last Friday before Purnima). Worship of Vara Lakshmi — boon-giving form of Goddess Lakshmi. Predominantly celebrated in Andhra Pradesh, Telangana, Karnataka, Tamil Nadu by married women for the well-being and prosperity of family. Kalasha Puja: a pot (kalasha) adorned with mango leaves and coconut represents Lakshmi. Lakshmi Ashtottara (108 names) recited.",
     "fast_note": "Women's vrat: dawn bath, dress in new clothes. Set up Vara Lakshmi Kalasha with turmeric, kumkum, silk sari, flowers, fruits. Draw rangoli (muggulu). Puja with 108 names (Lakshmi Ashtottara). Akshata (rice+turmeric) offerings. Dinner shared with neighbors. Break fast after evening puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 13, "approx_month": 8, "approx_day": 10}},

    {"name": "Aadi Perukku",             "hindi": "आदि पेरुक्कू",              "icon": "🌊",
     "deity": "Goddess Kaveri / River Goddess", "month": "Aadi (Solar)",
     "significance": "18th day of Aadi (Tamil solar month, mid-July to mid-August). Celebration of rivers and water bodies — especially Kaveri river. Women worship rivers for their life-giving nature. The river rises and overflows (perukku = to rise) during monsoon. Families picnic on river banks. Pongal-like sweet rice (aadi koozh) offered to rivers. Predominantly Tamil Nadu — also known as Aadi Pathinettam Perukku.",
     "fast_note": "No strict vrat. Visit river or water body (especially Kaveri). Women offer flowers, bananas, betel, turmeric-kumkum to river. Cook sweet rice (aadi koozh / sweet pongal). Picnic on river banks with family. Return home before sunset.",
     "rule": {"type": "solar", "approx_month": 8, "approx_day": 2}},

    # ─── Gupta Navratri ──────────────────────────────────────────────────────────
    {"name": "Ashadha (Gupta) Navratri", "hindi": "आषाढ़ गुप्त नवरात्रि",     "icon": "🔐",
     "deity": "Goddess Durga (10 Mahavidyas)", "month": "Ashadha",
     "significance": "Ashadha Shukla Pratipada to Navami (9 nights). Gupta Navratri — 'secret' Navratri observed by Tantric practitioners and Shakti devotees. Worship of the 10 Mahavidyas (Kali, Tara, Tripura Sundari, Bhuvaneshvari, Bhairavi, Chhinnamasta, Dhumavati, Bagalamukhi, Matangi, Kamala). Less publicly celebrated than Chaitra/Sharad Navratris but equally powerful for Tantra sadhana.",
     "fast_note": "9-day Devi vrat. Ghatasthapana on Pratipada. Daily Devi puja (Durga Saptashati or Mahavidya mantras). Strict sattvic diet or full fast. Midnight Tantric puja optional (Gupta means secret — deeper sadhana). Kanya Puja on Ashtami or Navami. Havan on Navami.",
     "rule": {"type": "tithi_range", "paksha": "S", "num": 1, "length": 9, "approx_month": 7, "approx_day": 1}},

    {"name": "Magha (Gupta) Navratri",   "hindi": "माघ गुप्त नवरात्रि",        "icon": "❄️",
     "deity": "Goddess Durga (10 Mahavidyas)", "month": "Magha",
     "significance": "Magha Shukla Pratipada to Navami (9 nights). Second Gupta Navratri of the year. Winter Navratri observed by Shakti and Tantric devotees. Worship of 10 Mahavidyas. Less publicly known but highly important in Shakta tradition. Bagalamukhi and Tara are especially propitiated in this Navratri.",
     "fast_note": "9-day Devi vrat. Ghatasthapana on Pratipada. Daily Devi Saptashati recitation. Sattvic diet (no onion, garlic, non-veg). Midnight Mahavidya puja for deeper practitioners. Kanya Puja on Ashtami. Havan on Navami. Complete fast or one-meal-a-day (phalahar).",
     "rule": {"type": "tithi_range", "paksha": "S", "num": 1, "length": 9, "approx_month": 2, "approx_day": 2}},

    # ─── Recurring Vrats ─────────────────────────────────────────────────────────
    {"name": "Pradosh Vrat (Shukla)",    "hindi": "प्रदोष व्रत (शुक्ल)",       "icon": "🌙",
     "deity": "Lord Shiva",       "month": "Recurring",
     "significance": "Shukla Trayodashi (13th lunar day, waxing fortnight). Pradosh Vrat — Shiva and Parvati are in an especially joyful mood during Pradosh Kaal (1.5 hours around sunset on Trayodashi). Lord Shiva performs the cosmic dance (Tandava) during Pradosh. Most powerful Shiva vrat — all sins destroyed, liberation attained. Somavar Pradosh (Monday) and Shani Pradosh (Saturday) are extra potent.",
     "fast_note": "Fast all day until Pradosh Kaal (approx. 1.5 hrs before to after sunset). Abhishek of Shivalinga: milk, curd, honey, gangajal, bilva (bael) leaves. Pradosh Stotra recitation. Break fast after puja in Pradosh Kaal. Om Namah Shivaya japa (108 times minimum).",
     "rule": {"type": "tithi", "paksha": "S", "num": 13, "approx_month": 1, "approx_day": 10}},

    {"name": "Pradosh Vrat (Krishna)",   "hindi": "प्रदोष व्रत (कृष्ण)",       "icon": "🌙",
     "deity": "Lord Shiva",       "month": "Recurring",
     "significance": "Krishna Trayodashi (13th lunar day, waning fortnight). Pradosh Vrat — Shiva worship during Pradosh Kaal at dusk. Krishna Paksha Pradosh is equally powerful as Shukla Paksha Pradosh. Shani Pradosh (Saturday Krishna Trayodashi) is especially potent for removing Saturn afflictions.",
     "fast_note": "Fast all day until Pradosh Kaal (1.5 hrs before sunset to 1.5 hrs after). Shiva Abhishek with panchamrit, bilva leaves. Pradosh Katha or Stotra. Break fast after sunset puja. Observe Brahmacharya on this day.",
     "rule": {"type": "tithi", "paksha": "K", "num": 13, "approx_month": 1, "approx_day": 25}},

    {"name": "Kojagiri Purnima",         "hindi": "कोजागिरी पूर्णिमा",         "icon": "🌕",
     "deity": "Goddess Lakshmi",  "month": "Ashwin",
     "significance": "Ashwin Shukla Purnima (Sharad Purnima / Kumar Purnima / Kojagari Purnima). Most significant Purnima of the year — moon is closest (perigee) and brightest. Goddess Lakshmi descends at midnight and asks 'Ko jagarti?' (Who is awake?). Nectar (amrit) falls from the moon into rice/milk kept in moonlight. Special kheer (rice-milk) left in moonlight absorbs divine nectar. Celebrated as Kumar Purnima in Odisha.",
     "fast_note": "Fast until moonrise. Cook kheer in open vessel, place in bright moonlight from moonrise to midnight. Lakshmi Puja at midnight. Then consume the moonlit kheer. Stay awake all night (Jagaran) playing cards/dice — symbolises being 'awake' when Lakshmi asks. Distribute kheer. No salt after sunset.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 10, "approx_day": 17}},

    # ─── Bengali & Eastern Festivals ─────────────────────────────────────────────
    {"name": "Poila Boishakh",           "hindi": "पोइला बैशाख",               "icon": "🎊",
     "deity": "Lord Vishnu / Goddess Lakshmi", "month": "Mesha (Solar)",
     "significance": "Bengali New Year. Sun enters Mesha Rashi (Aries). 1st day of Boishakh (Bengali calendar, approx. April 14-15). Celebrated in West Bengal, Bangladesh, and worldwide Bengali diaspora. Hal Khata (new account books opened by merchants — Lakshmi Ganesh Puja). Mangal Shobhajatra (UNESCO listed) procession in Dhaka. New clothes, sweets (mishti doi, sandesh), visits to relatives.",
     "fast_note": "No strict vrat. Morning bath, new clothes. Visit Lakshmi-Ganesh temple for Hal Khata puja (if merchant/businessman). Eat traditional foods: muri-moa, mishti doi, ilish (hilsa) fish. Evening cultural programs. Greet with 'Shubho Nababarsha' (Happy New Year in Bengali).",
     "rule": {"type": "solar", "approx_month": 4, "approx_day": 15}},

    {"name": "Durga Puja (Saptami)",     "hindi": "दुर्गा पूजा — सप्तमी",      "icon": "🪆",
     "deity": "Goddess Durga",    "month": "Ashwin",
     "significance": "Ashwin Shukla Saptami. First major day of Durga Puja (Bengal's greatest festival, also celebrated in Odisha, Assam, Jharkhand). Nabapatrika (9 plants representing 9 forms of Durga) installed and worshipped. Grand pandals (temporary temples) across Bengal light up. Goddess Durga idol (clay, 10-hands, with Lakshmi-Saraswati-Kartik-Ganesha) unveiled. Dhak drums fill the air.",
     "fast_note": "Partial fast on Saptami morning until Nabapatrika puja. Wear new clothes. Visit pandals. Dhuno daan (myrrh/incense offering). Anjali (flower offering) in morning at pandals. Prasad: khichdi, labra (mixed vegetables), payesh.",
     "rule": {"type": "tithi", "paksha": "S", "num": 7, "approx_month": 10, "approx_day": 2}},

    {"name": "Durga Puja (Ashtami)",     "hindi": "दुर्गा पूजा — अष्टमी",      "icon": "⚔️",
     "deity": "Goddess Durga",    "month": "Ashwin",
     "significance": "Ashwin Shukla Ashtami. Peak day of Durga Puja. Sandhi Puja at the junction of Ashtami-Navami (most powerful moment — Goddess destroys Chanda-Munda demons). 108 lamps lit. Pushpanjali in early morning with 108 lotus flowers or bel leaves. Kumari Puja (worship of young pre-pubescent girls as Devi). Dhunuchi Naach (clay pot dance with burning coconut husk). Sindur Khela for married women.",
     "fast_note": "Ashtami fast: no grains. Pushpanjali in morning (3 times: 5 AM, 8 AM, 10 AM approx). Sandhi Puja: fast until completion. Prasad only after puja. Kumari Puja: worship young girls with feet-washing, sindur, sweets. Break fast after Sandhi Puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 8, "approx_month": 10, "approx_day": 3}},

    {"name": "Durga Puja (Navami)",      "hindi": "दुर्गा पूजा — नवमी",        "icon": "🏺",
     "deity": "Goddess Durga",    "month": "Ashwin",
     "significance": "Ashwin Shukla Navami. Mahanavami — Durga's final battle victory day. Grand Navami Puja with Mahasnan (ritual bath of Durga idol). Havan and Bali (symbolic sacrifice with vegetables). Last Pushpanjali. Bhog (khichdi, labra, chutney, payesh) offered grandly. Devotees spend maximum time at pandals. Navami evening — families gather for final celebration.",
     "fast_note": "Navami fast until afternoon Havan. Mahanavami Havan with 108 ahutis. Final Pushpanjali with bel leaves. Grand Bhog distribution after puja. Evening: Navami feast with traditional Bengali dishes. Married women Sindur Khela begins tonight.",
     "rule": {"type": "tithi", "paksha": "S", "num": 9, "approx_month": 10, "approx_day": 4}},

    {"name": "Kali Puja",                "hindi": "काली पूजा",                  "icon": "🌑",
     "deity": "Goddess Kali",     "month": "Kartik",
     "significance": "Kartik Krishna Amavasya (same night as Diwali). Bengal's Shyama Puja / Kali Puja. While North India worships Lakshmi on Diwali night, Bengal worships Goddess Kali — the fierce destroyer of evil. Tantric midnight puja. Grand Kali pandals in Kolkata. Fireworks. Rani Rashmoni's Kalighat and Dakshineswar Kali temples are especially significant.",
     "fast_note": "Fast from morning until midnight puja. Kali Puja starts at midnight (Nishita Kaal). Offerings: red hibiscus, fish, rice, black sesame, bilva leaves. Tantric puja with mantras. Animal offerings are traditional at some temples. Break fast after midnight puja with prasad.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 11, "approx_day": 1}},

    {"name": "Manasa Puja",              "hindi": "मनसा पूजा",                  "icon": "🐍",
     "deity": "Goddess Manasa",   "month": "Shravana",
     "significance": "Shravana month / Nag Panchami period. Manasa Devi — Snake Goddess, daughter of Shiva, goddess of snakes and protection from snakebite. Worshipped throughout Bengal, Odisha, Bihar, Jharkhand. Manasha Pala (folk narrative songs about Manasa) recited by women. Mud images of Manasa placed under trees. Especially popular in rural Bengal and the Sundarbans region.",
     "fast_note": "Women's vrat in Shravana. Fast on the day of Manasa Puja. Offer: milk, flowers (white), fish (in some traditions), bananas to Manasa image. Manasha Pala (devotional songs) recited. Do not harm snakes during Shravana. Break fast after evening puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 5, "approx_month": 8, "approx_day": 9}},

    {"name": "Bishwakarma Puja",         "hindi": "विश्वकर्मा पूजा",             "icon": "⚙️",
     "deity": "Vishwakarma",      "month": "Bhadrapada / Solar",
     "significance": "Solar calendar: Sun in Virgo (Kanya Sankranti, approx September 17). Vishwakarma — divine architect of the gods who built Lanka, Dwarka, and Indra's heaven. Factories, workshops, machines, vehicles, tools are worshipped. Entire industrial Bengal/Jharkhand/Odisha shuts down — workers worship their machines. Kite flying (paatla hawa) popular in Bengal on this day. Aircraft, ships, cars decorated with flowers.",
     "fast_note": "No strict vrat. Morning puja of machines, tools, vehicles with flowers, sindur, incense. Vishwakarma idol placed in workplace. Offerings: fruits, sweets, betel. Do not use machines until puja complete. Evening feast and kite flying. No work order on this day.",
     "rule": {"type": "solar", "approx_month": 9, "approx_day": 17}},

    {"name": "Tusu Puja",                "hindi": "टुसू पूजा",                  "icon": "🌻",
     "deity": "Goddess Tusu",     "month": "Paush",
     "significance": "Paush month (December–January), concluding on Makar Sankranti / Poush Sankranti. Tusu Puja — tribal and folk festival of Jharkhand, West Bengal (Purulia, Bankura, Bardhaman), and Odisha. Young unmarried girls worship Tusu Devi (goddess of harvest and prosperity) throughout Paush month. On the last day (Poush Sankranti), Tusu is immersed in water with singing and dancing. Chaur (folk songs) sung every evening.",
     "fast_note": "Month-long observation by unmarried girls. Tusu image (clay, rice husk, or flowers) installed in a decorated palanquin (chaura). Daily evening songs (Tusu Gaan) and lamp lighting. No non-vegetarian food during Paush. Final immersion on Makar Sankranti day with procession and song.",
     "rule": {"type": "solar", "approx_month": 1, "approx_day": 14}},

    {"name": "Bandna Parab",             "hindi": "बंदना परब",                  "icon": "🐄",
     "deity": "Lord Vishnu / Cattle Goddess", "month": "Kartik",
     "significance": "Kartik month, around Diwali period. Tribal and folk festival of Jharkhand, Bengal, and Odisha — cattle worship festival (similar to Govardhan Puja). Adivasi and farming communities worship cows, bullocks, plough tools. Houses decorated with floral art. Community singing, traditional games. Marks end of agricultural season and harvest thanksgiving.",
     "fast_note": "No strict vrat. Bathe and decorate cattle with turmeric, sindur, marigold garlands. Offer: grass, gram, jaggery to cattle. Puja of plough and farming tools. Community feast with tribal food (handia rice beer in some communities). Songs and dance (jhumar) in evening.",
     "rule": {"type": "tithi", "paksha": "K", "num": 14, "approx_month": 11, "approx_day": 2}},

    # ─── Maharashtra Regional Festivals ──────────────────────────────────────────
    {"name": "Bail Pola",                "hindi": "बैल पोला",                   "icon": "🐂",
     "deity": "Cattle / Lord Shiva",  "month": "Shravana / Bhadrapada",
     "significance": "Shravana or Bhadrapada Krishna Amavasya (Pithori Amavasya). Maharashtra's Bull Worshipping festival. Farmers decorate bullocks with colourful clothes, bells, garlands, and sindur. Wooden toy bulls (khilobale) for children. Procession of decorated bulls through village streets. Bullock races in some areas. Expresses gratitude to bulls for their year-round farming assistance.",
     "fast_note": "No strict vrat. Bathe and decorate bulls with turmeric (haldi), oil, kumkum, garlands of flowers and marigold. Paint horns in colours. Feed special food: cooked rice, jaggery, gram. New bell and rope for bull. Mahila (women) cook puran poli and chakali as prasad. Procession with dhol-tasha music.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 8, "approx_day": 28}},

    # ─── Pushkar & Pilgrimage Festivals ──────────────────────────────────────────
    {"name": "Pushkar Mela",             "hindi": "पुष्कर मेला",                "icon": "🐪",
     "deity": "Lord Brahma",      "month": "Kartik",
     "significance": "Kartik Shukla Ekadashi to Purnima (5 days, peaks on Purnima). World's largest camel fair and one of India's most sacred pilgrimages. Pushkar — only temple to Lord Brahma in the world. The sacred Pushkar Lake was created when Brahma dropped a lotus here. Holy bath in Pushkar Lake on Kartik Purnima grants moksha. Camel trading, folk performances, hot air balloons. UNESCO recognized.",
     "fast_note": "Kartik Purnima: mandatory holy bath (Pushkar Snan) in Pushkar Lake (52 ghats). Brahma temple puja. Offer: lotus flowers, camphor, coins to Pushkar Lake. Lamp floating (diya) on lake. 5-day mela: arrive by Ekadashi. Strict no-meat, no-alcohol zone near Pushkar. Stay for Purnima snan.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 11, "approx_day": 26}},

    # ─── Kartik Snan ─────────────────────────────────────────────────────────────
    {"name": "Kartik Snan Aarambha",     "hindi": "कार्तिक स्नान आरंभ",         "icon": "🌅",
     "deity": "Lord Vishnu",      "month": "Ashwin",
     "significance": "Ashwin/Kartik Amavasya (Diwali Amavasya) to Kartik Purnima — 30-day holy bath observance. Kartik Snan: the most meritorious of all holy baths, performed before sunrise at rivers, ponds, or water bodies throughout Kartik month. Devotees (especially women) wake at Brahma Muhurta (4 AM) and take holy bath, light diyas, perform Tulsi puja. Equal in merit to bathing at all tirthas. Varanasi Panchkosi Yatra during this month.",
     "fast_note": "Daily Brahma Muhurta bath for full month. After bath: light diya near Tulsi plant. Sattvic diet whole month. Ekadashi and Purnima fasts are especially meritorious in Kartik. Charity (feeding Brahmins, poor) every day if possible. Avoid non-veg, alcohol. Read Vishnu Sahasranama or Bhagavata Purana.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 11, "approx_day": 1}},

    # ─── Assamese Festival ────────────────────────────────────────────────────────
    {"name": "Bohag Bihu (Rongali Bihu)", "hindi": "बोहाग बिहू (रोंगाली बिहू)", "icon": "🌿",
     "deity": "Nature / Cattle",  "month": "Mesha (Solar)",
     "significance": "Assamese New Year. Sun enters Mesha (Aries) — same as Baisakhi/Vishu/Puthandu but with Assamese traditions. Rongali = joyful; 7-day celebration. First day (Goru Bihu): cattle washed with turmeric and fed special food. Second day (Manuh Bihu): humans exchange gamosa (cotton towel) and pitha (rice cake). Traditional Bihu dance (Bihu Naas) in groups. Bihu songs (Bihu geet). Most festive New Year of Northeast India.",
     "fast_note": "No strict vrat. Goru Bihu (Day 1): bathe cattle before sunrise with turmeric, feed with gourd, brinjal, pulses. Manuh Bihu (Day 2): exchange gamosa and pitha with elders. Wear traditional mekhela chador (women) / dhoti-gamosa (men). Cook: til pitha (sesame rice cake), ghila pitha, sunga pitha. Bihu dance in community.",
     "rule": {"type": "solar", "approx_month": 4, "approx_day": 14}},

    # ─── Additional Vrat / Tithi ─────────────────────────────────────────────────
    {"name": "Vaikunta Ekadashi (South)", "hindi": "वैकुंठ एकादशी",             "icon": "🚪",
     "deity": "Lord Vishnu",      "month": "Margashirsha",
     "significance": "Margashirsha Shukla Ekadashi (same day as Geeta Jayanti in North). In South India (especially Tamil Nadu, Andhra, Karnataka), this is Vaikunta Ekadashi — the most important Ekadashi of all 24. The Vaikunta Dwaram (gate to heaven) at Srirangam and Tirupati is symbolically opened. Thousands queue overnight to enter the temple through the Swarga Vasal (heaven gate) at first light. Vishnu's grace is 1000x on this day.",
     "fast_note": "Ekadashi Vrat: no grains (most devotees do nirjala fast). At Srirangam, Tirupati, Parthasarathy (Chennai): arrive night before, queue overnight to enter through Swarga Vasal at dawn. Recite Vishnu Sahasranama, Thiruvaymoli (Divya Prabandham). Break fast on Dwadashi (Vaikunta Dwadashi) with Pancharatna Kootu and rice.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 12, "approx_day": 7, "special": "ekadashi_viddha"}},

    # ─── Chaitra Chhath — 4-Day Breakdown ───────────────────────────────────────
    {"name": "Chaitra Chhath — Nahay Khay", "hindi": "चैत्र छठ — नहाय खाय",  "icon": "🛁",
     "deity": "Surya",            "month": "Chaitra",
     "significance": "Chaitra Shukla Chaturthi. First day of Chaitra Chhath Puja — less widely observed than Kartik Chhath but equally sacred in Bihar, UP, and Jharkhand. Devotees take holy bath and cook a single sattvic meal (lauki/kaddu + chana dal + rice). Marks beginning of the 4-day Surya worship cycle in spring.",
     "fast_note": "Partial fast: one meal only (lauki/kaddu + chana dal + rice in bronze/clay vessel, no salt). No onion/garlic. Holy bath in river or pond at dawn. Sattvic food for entire family. Prepare for Kharna next day.",
     "rule": {"type": "tithi", "paksha": "S", "num": 4, "approx_month": 4, "approx_day": 3}},

    {"name": "Chaitra Chhath — Kharna",     "hindi": "चैत्र छठ — खरना",       "icon": "🍚",
     "deity": "Surya",            "month": "Chaitra",
     "significance": "Chaitra Shukla Panchami. Second day of Chaitra Chhath. Kharna — full-day nirjala fast (no food, no water). Evening: kheer (rice+jaggery) and wheat rotis prepared as prasad. Fast broken after sunset puja. After Kharna the 36-hour continuous nirjala vrat begins for the vrati.",
     "fast_note": "Nirjala fast all day. Evening: cook kheer in clay pot on mango-wood fire + wheat rotis. Offer to Chhathi Maiya and break fast. Post-Kharna: 36-hour continuous nirjala fast begins — no food, no water until Usha Arghya.",
     "rule": {"type": "tithi", "paksha": "S", "num": 5, "approx_month": 4, "approx_day": 4}},

    {"name": "Chaitra Chhath — Sandhya Arghya", "hindi": "चैत्र छठ — संध्या अर्घ्य", "icon": "🌅",
     "deity": "Surya",            "month": "Chaitra",
     "significance": "Chaitra Shukla Shashthi. Third and main day of Chaitra Chhath. Sandhya Arghya — evening offering to the setting sun at river/pond ghat with bamboo baskets of fruits, thekua, sugarcane, and coconut. 36-hour nirjala fast continues. All-night vigil follows.",
     "fast_note": "36-hour Nirjala Vrat continues. No food, no water. Reach ghat 2 hours before sunset with bamboo sup. Offer Arghya to setting sun with fruits, thekua, coconut, sugarcane. Stand in water if possible. Keep all-night vigil after returning.",
     "rule": {"type": "tithi", "paksha": "S", "num": 6, "approx_month": 4, "approx_day": 5}},

    {"name": "Chaitra Chhath — Usha Arghya", "hindi": "चैत्र छठ — उषा अर्घ्य",  "icon": "🌄",
     "deity": "Surya",            "month": "Chaitra",
     "significance": "Chaitra Shukla Saptami. Final day of Chaitra Chhath. Usha Arghya — pre-dawn arrival at ghat, offering to the rising sun. Concludes the 36-hour nirjala fast. Vrati breaks fast with thekua, raw ginger, and water after sunrise Arghya. Prasad (thekua, fruits, sugarcane) distributed to all.",
     "fast_note": "Reach ghat before sunrise. Offer Arghya to rising sun in water. After sunrise Arghya complete: break 36-hour fast with thekua + raw ginger + water. Then full prasad. Distribute to all present. Chaitra Chhath complete.",
     "rule": {"type": "tithi", "paksha": "S", "num": 7, "approx_month": 4, "approx_day": 6}},

    # ─── Kumbh Mela ───────────────────────────────────────────────────────────────
    {"name": "Kumbh Mela (Prayagraj)",   "hindi": "कुंभ मेला (प्रयागराज)",     "icon": "🏺",
     "deity": "Lord Vishnu / Triveni Sangam", "month": "Magha / Phalguna",
     "significance": "Magha Shukla Ekadashi to Mahashivaratri period (every 12 years at Prayagraj — Purna Kumbh; every 6 years — Ardha Kumbh; every year — Magh Mela). Jupiter in Aries + Sun in Aries + Moon in Aries (Makar Sankranti triggers the opening). World's largest human gathering. Holy bath at Triveni Sangam (confluence of Ganga, Yamuna, Saraswati) destroys all sins and grants moksha. Major Shahi Snan (royal bath) dates: Makar Sankranti, Mauni Amavasya, Basant Panchami, Maghi Purnima, Mahashivaratri.",
     "fast_note": "Shahi Snan (Royal Bath) on auspicious dates — arrive before dawn. Bath at Triveni Sangam at Prayagraj. Donate to sadhus and saints. Attend Satsang and Pravachan in Kalpa Vasa camps. Kalpa Vasa: month-long stay at Sangam (sleeping on ground, one meal/day, total celibacy). Charity (Anna Daan) daily during Kumbh.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 1, "approx_day": 23}},

    # ─── Dol Purnima ──────────────────────────────────────────────────────────────
    {"name": "Dol Purnima",              "hindi": "डोल पूर्णिमा",               "icon": "🎨",
     "deity": "Lord Krishna / Radha",  "month": "Phalguna",
     "significance": "Phalguna Shukla Purnima. Bengali and Odia celebration of Holi — also known as Dol Jatra or Dol Yatra. Idols of Radha-Krishna placed on a beautifully decorated swing (dol) and carried in procession. Devotees throw abir (fragrant coloured powder — red, yellow, green) on the idols and each other. Tagore's Shantiniketenbasant Utsav is celebrated on this day with baul music, dance, and natural colours. Chaitanya Mahaprabhu's birth anniversary (Gaura Purnima) also falls on this day.",
     "fast_note": "No strict vrat. Morning: bathe and wear yellow/saffron clothes. Visit Radha-Krishna temple for Dol Yatra. Offer abir (gulal) to idols first, then play with abir. Sing Kirtan and Baul songs. Evening: cultural programs. Gaura Purnima: Chaitanya Mahaprabhu puja, Hare Krishna kirtan all night.",
     "rule": {"type": "tithi", "paksha": "S", "num": 15, "approx_month": 3, "approx_day": 14}},

    # ─── Lakshmi Puja (Diwali) ───────────────────────────────────────────────────
    {"name": "Lakshmi Puja (Diwali)",    "hindi": "लक्ष्मी पूजा (दीवाली)",      "icon": "💰",
     "deity": "Goddess Lakshmi",  "month": "Kartik",
     "significance": "Kartik Krishna Amavasya (Diwali night). Main puja of Diwali — Goddess Lakshmi visits every home that is clean, lit, and joyful on this night. Lakshmi emerged from the Kshira Sagara (cosmic ocean) during Samudra Manthan on this day. Merchants perform Chopda Pujan (new account books). Goddess Saraswati, Ganesh, and Kubera also worshipped alongside Lakshmi. Entire India lit with diyas, candles, and lights. New year for Gujarati and Marwari communities.",
     "fast_note": "Fast until evening Lakshmi Puja (Pradosh Kaal). Clean and decorate entire house. Draw Lakshmi footprints (auspicious) from door to puja room. Light diyas in every room. Puja: Lakshmi idol with lotus, coins, sweets (kheel-batasha). Lakshmi Stotram / Ashtalakshmi recitation. Chopda Puja for business owners. Fireworks after puja. Break fast with prasad.",
     "rule": {"type": "tithi", "paksha": "K", "num": 15, "approx_month": 11, "approx_day": 1}},

    # ─── Mysuru Dasara ────────────────────────────────────────────────────────────
    {"name": "Mysuru Dasara",            "hindi": "मैसूर दशहरा",                "icon": "👑",
     "deity": "Goddess Chamundeshwari", "month": "Ashwin",
     "significance": "Ashwin Shukla Dasami (Vijayadashami). Karnataka's grandest festival — Mysuru (Mysore) Palace is illuminated with 100,000 lights throughout Navratri. On Dasara day: grand royal procession (Jamboo Savari) — golden howdah elephant carries Goddess Chamundeshwari idol through Mysuru city streets with caparisoned elephants, horses, camels, tableaux, and police bands. Torchlight parade (Panjina Kavayathu) at night. Chamundeshwari temple atop Chamundi Hills is main pilgrimage site. Declared National Festival by Government of India.",
     "fast_note": "Navratri: Golu (doll display) set up at home. Chamundeshwari temple darshan especially on Ashtami and Navami. Dasara day: watch Jamboo Savari procession (starts Mysuru Palace, ends Bannimantap). Banni tree worship (Shami Puja) on Dasara. Ayudha Puja day before Dasara: worship vehicles and tools.",
     "rule": {"type": "tithi", "paksha": "S", "num": 10, "approx_month": 10, "approx_day": 3}},

    # ─── Navratri Golu / Bommai Kolu ─────────────────────────────────────────────
    {"name": "Navratri Golu (Bommai Kolu)", "hindi": "नवरात्रि गोलू (बोम्मई कोलु)", "icon": "🪆",
     "deity": "Goddess Durga / Saraswati / Lakshmi", "month": "Ashwin",
     "significance": "Ashwin Shukla Pratipada to Navami (9 nights of Sharad Navratri). South Indian tradition of displaying Golu (Tamil: Kolu) — stepped platforms (odd number: 3, 5, 7, 9 steps) with dolls and figurines arranged in themes (gods, epics, village scenes, nature). Tamil Nadu, Karnataka (Gombe Habba), Andhra Pradesh and Telangana celebrate this. Women invite neighbours for Golu darshan and exchange gifts (sundal — boiled legumes, tamboolam — betel+coconut). Saraswati Puja on Ashtami (Saraswati Puja for books and instruments) and Ayudha Puja on Navami.",
     "fast_note": "Set up Golu platform on Navratri Pratipada. Add new doll every year. Daily Devi puja with flowers and lamp. Invite neighbours for Golu darshan (especially in evenings). Distribute sundal (chickpea, green gram, black-eyed peas) as prasad each day — different legume each day. Saraswati Puja (Ashtami): keep books, instruments, tools before Saraswati. Ayudha Puja (Navami): worship vehicles, machines. Vijayadashami: take books back (Vidyarambham for children).",
     "rule": {"type": "tithi", "paksha": "S", "num": 1, "approx_month": 10, "approx_day": 3}},

    # ─── Ayyappa Mandalam ─────────────────────────────────────────────────────────
    {"name": "Ayyappa Mandalam (Deeksha)", "hindi": "अयप्पा मंडलम (दीक्षा)",   "icon": "🔱",
     "deity": "Lord Ayyappa (Sastha)", "month": "Vrischika (Solar)",
     "significance": "41-day vrat (Mandala Kalam) beginning on the first day of Malayalam solar month Vrischika (approx. November 15–16) and concluding on Makaravilakku (Makara Jyoti, January 14). Sabarimala pilgrimage — one of the world's largest annual pilgrimages (50 million+ devotees). Devotees (Ayyappa bhaktas) observe strict 41-day Deeksha: black/blue dress, tulsi/rudraksha mala, bare feet, celibacy, two meals/day, no shaving, sleep on floor. Journey to Sabarimala mountain temple (Kerala). 18 Golden Steps (Pathinettam Padi) to the shrine. Makaravilakku: celestial star (Makara Jyothi) appears on Makaravilakku night.",
     "fast_note": "41-day Deeksha rules: wear black/blue irumudi (cloth). Carry irumudi (two-compartment bag) with coconut filled with ghee. No non-veg, no alcohol, no tobacco, no sexual contact. Two vegetarian meals/day. Sleep on floor/mat. No footwear. Daily Ayyappa puja morning and evening. Conclude: trek Sabarimala, climb 18 steps, break coconut, offer ghee to Ayyappa. Makaravilakku darshan is the main goal.",
     "rule": {"type": "solar", "approx_month": 11, "approx_day": 15}},

    # ─── Ekadashi Vrat (Recurring) ───────────────────────────────────────────────
    {"name": "Ekadashi Vrat",            "hindi": "एकादशी व्रत",                "icon": "🙏",
     "deity": "Lord Vishnu",      "month": "Recurring",
     "significance": "Ekadashi (11th lunar day) occurs twice every month — Shukla Ekadashi (waxing) and Krishna Ekadashi (waning). Total 24 Ekadashis per year (25 in leap lunar year). Each has a unique name and specific significance. Major Ekadashis: Nirjala (Jyeshtha S11 — toughest, most meritorious), Ashadhi / Dev Shayani (Ashadha S11 — Vishnu enters Yoganidra), Dev Uthani / Prabodhini (Kartik S11 — Vishnu wakes), Geeta Jayanti / Vaikunta (Margashirsha S11 — most liberating). Ekadashi vrat is the easiest path to Vishnu's grace — observed even by those who can't do other vrats.",
     "fast_note": "Ekadashi Vrat rules (apply to all Ekadashis): No grains (anna — rice, wheat, dal, etc.) from Dashami sunset to Dwadashi sunrise. Allowed: fruits, milk, dry fruits, sabudana, sendha namak (rock salt), potatoes, kuttu. Ideal: Nirjala (no water either) for maximum merit. Parana (fast breaking) on Dwadashi: must be done between sunrise and Dwadashi end time — NEVER after that (Viddha Ekadashi rule). Vishnu Sahasranama or Ekadashi Mahatmya recitation. Tulsi puja.",
     "rule": {"type": "tithi", "paksha": "S", "num": 11, "approx_month": 1, "approx_day": 10, "special": "ekadashi_viddha"}},

]

# ─── Main calculation ──────────────────────────────────────────────────────

_cache: Dict = {}

def get_festival_calendar(
    year: int,
    lat: float = 28.6139,
    lon: float = 77.209,
    tz: float = 5.5,
    tradition: str = "smarta",
    lunar_system: str = "amanta",
) -> List[Dict]:
    """
    tradition: "smarta" (default) | "vaishnava" | "north_indian" | "gujarati" | "bengali"
    lunar_system: "amanta" (default, month ends at Amavasya) | "purnimanta" (month ends at Purnima)
    """
    trad_rules = TRADITION_RULES.get(tradition, TRADITION_RULES["smarta"])
    cache_key = (year, round(lat, 2), round(lon, 2), round(tz, 1), tradition, lunar_system)
    if cache_key in _cache:
        return _cache[cache_key]

    _init()
    results  = []
    ref_dates: Dict[str, str] = {}

    # Pre-compute all eclipses for the year (used for Grahan Dosha checks)
    try:
        year_eclipses = _get_year_eclipses(year, lat, lon, tz)
    except Exception:
        log.exception("Eclipse computation failed for year %d", year)
        year_eclipses = []

    # Check for Adhik / Kshaya Maas (JD ranges used inside festival searches)
    try:
        adhika_range = _get_adhika_masa_range(year, tz)
        adhik_note   = _get_adhik_maas_note(year, tz, lunar_system=lunar_system)
    except Exception:
        log.exception("Adhika maas note failed for year %d", year)
        adhika_range = None
        adhik_note   = None

    # Pre-compute the name of the Adhik month (e.g. "Jyeshtha") for improved skip logic
    adhik_month_name: Optional[str] = None
    if adhika_range is not None:
        try:
            nm_s = adhika_range[0]
            rashi_s = int(_sun_lon_sid(nm_s) / 30) % 12
            names = PURNIMANTA_MONTH_NAMES if lunar_system == "purnimanta" else LUNAR_MONTH_NAMES
            adhik_month_name = names[rashi_s]
        except Exception:
            pass

    try:
        kshaya_range = _get_kshaya_masa_range(year, tz)
        kshaya_note  = _get_kshaya_masa_note(year, tz, lunar_system=lunar_system)
    except Exception:
        log.exception("Kshaya maas note failed for year %d", year)
        kshaya_range = None
        kshaya_note  = None

    for festival in FESTIVAL_RULES:
        rule  = festival["rule"]
        entry = {k: v for k, v in festival.items() if k != "rule"}
        entry["dosh_notes"]  = []
        entry["tithi_type"]  = "normal"
        entry["date_end"]    = None
        entry["tithi_start"] = "N/A"
        entry["tithi_end"]   = "N/A"
        entry["paksha"]      = "N/A"
        entry["tithi_name"]  = "N/A"
        entry["tithi_prev_day"]  = False   # tithi started the previous calendar day
        entry["tithi_start_date"] = None   # ISO date (YYYY-MM-DD) when this tithi actually begins
        entry["date_smarta"]     = None    # Smarta tradition observance date
        entry["date_vaishnava"]  = None    # Vaishnava tradition observance date (may differ for Ekadashi)
        entry["tradition"]       = tradition
        entry["lunar_system"]    = lunar_system
        entry.setdefault("fast_note", None)

        try:
            # ── Solar entry (Sankranti) ─────────────────────────────────
            if rule["type"] == "solar":
                d, t = _find_solar_entry(year, rule["lon"], rule["approx_month"], tz)
                entry["date"]             = d
                entry["tithi_start"]      = t
                entry["tithi_end"]        = t
                entry["tithi_start_date"] = d
                entry["paksha"]           = "N/A"
                entry["tithi_name"]       = "Sankranti"
                ref_dates[festival["name"]] = d

            # ── Day after a reference festival ─────────────────────────
            elif rule["type"] == "day_after":
                ref = ref_dates.get(rule["ref"])
                if ref:
                    d = (date.fromisoformat(ref) + timedelta(days=1)).isoformat()
                    entry["date"]             = d
                    entry["tithi_start"]      = "00:00"
                    entry["tithi_end"]        = "23:59"
                    entry["tithi_start_date"] = d
                    entry["paksha"]           = "N/A"
                    entry["tithi_name"]       = "Full day"
                    ref_dates[festival["name"]] = d
                else:
                    entry["date"] = f"{year}-01-01"

            # ── Day before a reference festival ────────────────────────
            elif rule["type"] == "day_before":
                ref = ref_dates.get(rule["ref"])
                if ref:
                    d = (date.fromisoformat(ref) - timedelta(days=1)).isoformat()
                    entry["date"]             = d
                    entry["tithi_start"]      = "00:00"
                    entry["tithi_end"]        = "23:59"
                    entry["tithi_start_date"] = d
                    entry["paksha"]           = "N/A"
                    entry["tithi_name"]       = "Full day"
                    ref_dates[festival["name"]] = d
                else:
                    entry["date"] = f"{year}-01-13"  # Lohri fallback

            # ── N days offset from a reference festival ─────────────────
            elif rule["type"] == "day_offset":
                ref = ref_dates.get(rule["ref"])
                offset = rule.get("offset", 1)
                if ref:
                    d = (date.fromisoformat(ref) + timedelta(days=offset)).isoformat()
                    entry["date"]             = d
                    entry["tithi_start"]      = "00:00"
                    entry["tithi_end"]        = "23:59"
                    entry["tithi_start_date"] = d
                    entry["paksha"]           = "N/A"
                    entry["tithi_name"]       = "Full day"
                    ref_dates[festival["name"]] = d
                else:
                    entry["date"] = f"{year}-01-01"

            # ── Nakshatra-based date (Moon in target nakshatra) ─────────
            elif rule["type"] == "nakshatra":
                target_nak   = rule["nakshatra"]  # 0-indexed (0=Ashwini … 26=Revati)
                approx_month = rule["approx_month"]
                approx_day   = rule.get("approx_day", 15)
                jd_center    = swe.julday(year, approx_month, approx_day, 6.0 - tz)

                _init()
                found_jd = None
                for delta in sorted(range(-20, 21), key=abs):
                    for sub_h in [0, 6, 12, 18]:
                        test_jd  = jd_center + float(delta) + sub_h / 24.0
                        moon_lon = swe.calc_ut(test_jd, swe.MOON, swe.FLG_SIDEREAL)[0][0]
                        nak      = int(moon_lon * 27 / 360) % 27
                        if nak == target_nak:
                            found_jd = test_jd
                            break
                    if found_jd is not None:
                        break

                if found_jd is not None:
                    y2, m2, d2 = _jd_to_parts(found_jd, tz)[:3]
                else:
                    y2, m2, d2 = year, approx_month, approx_day

                d = f"{y2:04d}-{m2:02d}-{d2:02d}"
                sr_jd = _actual_sunrise_jd(y2, m2, d2, lat, lon, tz)
                entry["date"]             = d
                entry["tithi_start"]      = _jd_to_time_str(sr_jd, tz)
                entry["tithi_end"]        = "23:59"
                entry["tithi_start_date"] = d
                entry["paksha"]           = "N/A"
                entry["tithi_name"]       = "Nakshatra Day"
                ref_dates[festival["name"]] = d

            # ── Single tithi ────────────────────────────────────────────
            elif rule["type"] == "tithi":
                target_idx   = _rule_to_idx(rule["paksha"], rule["num"])
                approx_month = rule["approx_month"]
                approx_day   = rule.get("approx_day", 15)
                jd_center    = swe.julday(year, approx_month, approx_day, 6.0 - tz)

                found_jd = None
                for delta in sorted(range(-60, 61), key=abs):
                    # Check at 4 sub-points per day to catch short/ksheya tithis
                    # that don't appear at 6 AM (e.g. Ugadi Pratipada 2026)
                    for sub_h in [0, 6, 12, 18]:
                        test_jd = jd_center + float(delta) + sub_h / 24.0
                        if _tithi_idx(test_jd) == target_idx:
                            found_jd = test_jd
                            break
                    if found_jd is not None:
                        break

                if found_jd is None:
                    entry["date"] = f"{year}-{approx_month:02d}-15"
                    results.append(entry)
                    continue

                # All festivals skip the Adhika month — observed in the nirmala month only.
                found_jd = _skip_adhika_if_needed(found_jd, target_idx, adhika_range, jd_center=jd_center,
                                                  expected_month=festival.get("month"), tz=tz)

                # ── MONTH CORRECTION ──
                # Always use amanta for internal check — festival["month"] fields are in Amanta convention.
                if festival.get("month"):
                    got = _get_lunar_month_name(found_jd, tz, "amanta")
                    if got and got != festival["month"]:
                        # Try one month earlier first (handles late jd_center overshoot)
                        earlier = _search_nirmala(found_jd - 29.5, target_idx)
                        if _get_lunar_month_name(earlier, tz, "amanta") == festival["month"]:
                            found_jd = earlier
                        else:
                            found_jd = _search_nirmala(found_jd + 29.5, target_idx)
                        start_jd = _find_tithi_start(target_idx, found_jd)
                        end_jd   = _find_tithi_end(target_idx, found_jd)

                start_jd = _find_tithi_start(target_idx, found_jd)
                end_jd   = _find_tithi_end(target_idx, found_jd)

                # ── Special date rules (Pradosh / Sunrise / Start / Bhadra shift) ──
                if rule.get("pradosh_date", False):
                    y_temp, m_temp, d_temp = _jd_to_parts(start_jd, tz)[:3]
                    for _off in range(-1, 4):  # start 1 day before tithi start
                        _td = date(y_temp, m_temp, d_temp) + timedelta(days=_off)
                        _pr = _actual_sunset_jd(_td.year, _td.month, _td.day, lat, lon, tz)
                        if not (start_jd <= _pr <= end_jd):
                            continue
                        # Auto-skip Bhadra Martya day if flag set (Holika Dahan)
                        if rule.get("bhadra_shift", False) and _is_vishti(_pr) and _bhadra_loka(_pr) == "Martya":
                            continue
                        y2, m2, d2 = _td.year, _td.month, _td.day
                        break
                    else:
                        y2, m2, d2 = _jd_to_parts(found_jd, tz)[:3]

                elif rule.get("date_from_sunrise", False):
                    y2, m2, d2 = _find_sunrise_date_in_tithi(start_jd, end_jd, lat, lon, tz)

                elif rule.get("date_from_start", False):
                    y2, m2, d2 = _jd_to_parts(start_jd, tz)[:3]

                else:
                    y2, m2, d2 = _jd_to_parts(found_jd, tz)[:3]
                sunrise_jd      = _actual_sunrise_jd(y2, m2, d2, lat, lon, tz)
                next_day        = date(y2, m2, d2) + timedelta(days=1)
                next_sunrise_jd = _actual_sunrise_jd(next_day.year, next_day.month, next_day.day, lat, lon, tz)

                entry["date"]             = f"{y2:04d}-{m2:02d}-{d2:02d}"
                entry["tithi_start"]      = _jd_to_time_str(start_jd, tz)
                entry["tithi_end"]        = _jd_to_time_str(end_jd, tz)
                entry["tithi_start_date"] = _jd_to_date_str(start_jd, tz)
                entry["tithi_prev_day"]   = _jd_to_date_str(start_jd, tz) < entry["date"]
                entry["tithi_type"]  = _tithi_type(start_jd, end_jd, sunrise_jd, next_sunrise_jd)

                # Vridhi: observe on the SECOND day per Dharmashastra
                # Skip for Swayamsiddha (Akshaya Tritiya) and Madhyahna-rule festivals (Ram Navami)
                if entry["tithi_type"] == "vridhi" and not rule.get("no_vridhi_shift"):
                    _nd = date(y2, m2, d2) + timedelta(days=1)
                    y2, m2, d2 = _nd.year, _nd.month, _nd.day
                    entry["date"]  = f"{y2:04d}-{m2:02d}-{d2:02d}"
                    sunrise_jd     = _actual_sunrise_jd(y2, m2, d2, lat, lon, tz)

                entry["paksha"]      = PAKSHA_NAMES[target_idx]
                entry["tithi_name"]  = TITHI_NAMES[target_idx]
                end_date             = _jd_to_date_str(end_jd, tz)
                entry["date_end"]    = end_date if end_date != entry["date"] else None

                ref_dates[festival["name"]] = entry["date"]

                # ── Vedic dosh rules ────────────────────────────────────
                special = rule.get("special")

                if special == "bhadra_check":
                    # Use actual sunset for Pradosh window (not fixed 12.5h proxy)
                    pradosh_jd = _actual_sunset_jd(y2, m2, d2, lat, lon, tz)

                    if _is_vishti(pradosh_jd):
                        # Bhadra Loka check: only warn if Moon is in Martya Loka
                        loka = _bhadra_loka(pradosh_jd)
                        ve = _find_vishti_end_after(pradosh_jd)
                        vs = _find_vishti_start_before(pradosh_jd)
                        phase = _bhadra_phase(pradosh_jd, tz)
                        start_str = _jd_to_time_str(vs, tz) if vs else "N/A"
                        end_str   = _jd_to_time_str(ve, tz) if ve else "N/A"
                        if loka == "Martya":
                            entry["dosh_notes"].append(
                                f"⚠️ Bhadra Dosh at Pradosh — Bhadra active {start_str}–{end_str} "
                                f"({phase}, Moon in Martya Loka — Earth realm, malefic). "
                                f"Per Dharmasindhu: observe AFTER Bhadra ends at {end_str}. "
                                "Performing during Bhadra is considered inauspicious."
                            )
                        else:
                            entry["dosh_notes"].append(
                                f"✅ Bhadra present {start_str}–{end_str} but Moon is in {loka} Loka — "
                                "benign residence, no inauspicious effect on Earth. Pradosh is clear."
                            )
                    elif _is_vishti(sunrise_jd):
                        loka = _bhadra_loka(sunrise_jd)
                        ve = _find_vishti_end_after(sunrise_jd)
                        phase = _bhadra_phase(sunrise_jd, tz)
                        end_str = _jd_to_time_str(ve, tz) if ve else "N/A"
                        if loka == "Martya":
                            entry["dosh_notes"].append(
                                f"ℹ️ Bhadra present in morning ({phase}, Martya Loka). "
                                f"Auspicious window opens after Bhadra ends at {end_str}."
                            )
                        else:
                            entry["dosh_notes"].append(
                                f"✅ Bhadra in morning but in {loka} Loka — benign, no Earth effect."
                            )
                    else:
                        entry["dosh_notes"].append("✅ No Bhadra at Pradosh — window is clear and fully auspicious.")

                elif special == "madhyahna_note":
                    # Ram Navami: Navami must cover Madhyahna = 2/5 of Dinamana (day-length)
                    sunset_jd = _actual_sunset_jd(y2, m2, d2, lat, lon, tz)
                    dinamana  = sunset_jd - sunrise_jd         # total day fraction
                    madhya_jd = sunrise_jd + (2.0 / 5.0) * dinamana  # Madhyahna start
                    if start_jd <= madhya_jd <= end_jd:
                        entry["dosh_notes"].append(
                            "☀️ Navami covers Madhyahna (noon) today — correct day for Ram Janma puja. "
                            "Lord Rama was born at noon (Abhijit Muhurta)."
                        )
                    else:
                        entry["dosh_notes"].append(
                            "☀️ Navami begins after noon today — Ram Navami puja at noon is not possible. "
                            "Perform puja during the active Navami window instead."
                        )

                elif special == "aparahna_note":
                    # Dussehra: Vijaya Muhurta = Aparahna = 3/5 of day-length after sunrise
                    sunset_jd   = _actual_sunset_jd(y2, m2, d2, lat, lon, tz)
                    day_len     = sunset_jd - sunrise_jd          # total day in JD fractions
                    aparahna_jd = sunrise_jd + (3.0 / 5.0) * day_len  # 3rd of 5 daytime divisions
                    aparahna_str = _jd_to_time_str(aparahna_jd, tz)
                    if start_jd <= aparahna_jd <= end_jd:
                        entry["dosh_notes"].append(
                            f"🏆 Vijaya Muhurta (Aparahna ~{aparahna_str}) falls within Dashami — "
                            "auspicious time for Shastra Puja and Ravan Dahan."
                        )
                    else:
                        entry["dosh_notes"].append(
                            f"⚠️ Dashami begins after Aparahna (~{aparahna_str}). "
                            "Vijaya Muhurta is limited — check tomorrow if Dashami covers Aparahna then."
                        )

                elif special == "nishita_note":
                    # Nishita Kaal = midpoint of night (between sunset and next sunrise)
                    sunset_jd   = _actual_sunset_jd(y2, m2, d2, lat, lon, tz)
                    midnight_jd = (sunset_jd + next_sunrise_jd) / 2.0
                    require_rohini = trad_rules.get("janmashtami_rohini", False)
                    try:
                        moon_lon = swe.calc_ut(midnight_jd, swe.MOON, swe.FLG_SIDEREAL)[0][0]
                        nak_idx  = int(moon_lon / (360.0 / 27))   # 0–26
                        rohini_active = (nak_idx == 3)             # Rohini = nakshatra 4 (0-indexed 3)
                        nak_name = ["Ashwini","Bharani","Krittika","Rohini","Mrigashira",
                                    "Ardra","Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni",
                                    "Uttara Phalguni","Hasta","Chitra","Swati","Vishakha","Anuradha",
                                    "Jyeshtha","Mula","Purva Ashadha","Uttara Ashadha","Shravana",
                                    "Dhanishtha","Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada",
                                    "Revati"][nak_idx]
                    except Exception:
                        rohini_active = False
                        nak_name = "unknown"

                    if start_jd <= midnight_jd <= end_jd:
                        # Smarta: Ashtami covers Nishita → today is correct
                        entry["date_smarta"] = entry["date"]
                        if require_rohini and not rohini_active:
                            # Vaishnava: Rohini required but not present tonight — check next night
                            next_sunset_jd   = _actual_sunset_jd(next_day.year, next_day.month, next_day.day, lat, lon, tz)
                            nd2              = next_day + timedelta(days=1)
                            nd2_sunrise_jd   = _actual_sunrise_jd(nd2.year, nd2.month, nd2.day, lat, lon, tz)
                            next_midnight_jd = (next_sunset_jd + nd2_sunrise_jd) / 2.0
                            try:
                                ml2 = swe.calc_ut(next_midnight_jd, swe.MOON, swe.FLG_SIDEREAL)[0][0]
                                rohini_next = (int(ml2 / (360.0 / 27)) == 3)
                            except Exception:
                                rohini_next = False
                            if rohini_next and start_jd <= next_midnight_jd <= end_jd:
                                entry["date_vaishnava"] = next_day.isoformat()
                                entry["dosh_notes"].append(
                                    f"🌙 Nishita Kaal within Ashtami tonight — Smarta observance today. "
                                    f"Moon in {nak_name} (not Rohini). "
                                    f"🌟 Rohini active at Nishita TOMORROW night — VAISHNAVA observance: {next_day.isoformat()}."
                                )
                                if trad_rules.get("janmashtami_rohini"):
                                    entry["date"] = entry["date_vaishnava"]
                            else:
                                entry["date_vaishnava"] = entry["date"]
                                entry["dosh_notes"].append(
                                    f"🌙 Nishita Kaal within Ashtami — correct for midnight Krishna Janma puja. "
                                    f"Moon in {nak_name} at Nishita (Rohini not present on either night). "
                                    "Both Smarta and Vaishnava observe today."
                                )
                        else:
                            entry["date_vaishnava"] = entry["date"]
                            rohini_str = "🌟 Rohini also active at Nishita — VAISHNAVA observance confirmed!" if rohini_active else f"(Moon in {nak_name} at Nishita)"
                            entry["dosh_notes"].append(
                                f"🌙 Nishita Kaal falls within Ashtami — correct for midnight Krishna Janma puja. "
                                f"{rohini_str}"
                            )
                    else:
                        # Ashtami doesn't cover tonight's Nishita — check next night
                        next_sunset_jd    = _actual_sunset_jd(next_day.year, next_day.month, next_day.day, lat, lon, tz)
                        nd2               = next_day + timedelta(days=1)
                        nd2_sunrise_jd    = _actual_sunrise_jd(nd2.year, nd2.month, nd2.day, lat, lon, tz)
                        next_midnight_jd  = (next_sunset_jd + nd2_sunrise_jd) / 2.0
                        if start_jd <= next_midnight_jd <= end_jd:
                            entry["date_smarta"]    = entry["date"]
                            entry["date_vaishnava"] = next_day.isoformat()
                            entry["dosh_notes"].append(
                                "🌙 Ashtami covers Nishita Kaal on the NEXT night — "
                                f"VAISHNAVA observance: {next_day.isoformat()}. "
                                f"SMARTA observance: {entry['date']} (today)."
                            )
                            if trad_rules.get("janmashtami_rohini"):
                                entry["date"] = entry["date_vaishnava"]
                        else:
                            entry["date_smarta"]    = entry["date"]
                            entry["date_vaishnava"] = entry["date"]
                            entry["dosh_notes"].append(
                                "🌙 Ashtami does not cover Nishita Kaal on this night. "
                                "Check next day — some traditions observe Janmashtami the following day."
                            )

                # ── Ekadashi Viddha check — tradition-aware ───────────────────
                # Arunodaya = 4 Ghatikas (96 min) before sunrise.
                # Vaishnava rule: if Dashami touches Arunodaya → Ekadashi is contaminated (Dashami Viddha).
                #   → Vaishnava fast shifts to next day (Dvadashi, Mahadvadasi).
                # Smarta rule: observe on the day Ekadashi covers sunrise (no Arunodaya check).
                if special == "ekadashi_viddha":
                    arunodaya_jd = sunrise_jd - 96.0 / 1440.0   # 96 min before sunrise
                    dashami_idx  = (target_idx - 1) % 30
                    arun_str     = _jd_to_time_str(arunodaya_jd, tz)
                    dvadashi_date = date(y2, m2, d2) + timedelta(days=1)

                    if _tithi_idx(arunodaya_jd) == dashami_idx:
                        # Dashami active at Arunodaya → Viddha
                        entry["date_smarta"]    = entry["date"]
                        entry["date_vaishnava"] = dvadashi_date.isoformat()
                        entry["dosh_notes"].append(
                            f"🔱 Dashami Viddha — Dashami active at Arunodaya ({arun_str}, 96 min before sunrise). "
                            f"SMARTA observance: {entry['date_smarta']} (today). "
                            f"VAISHNAVA observance: {entry['date_vaishnava']} (Mahadvadasi — per Hari Bhakti Vilas)."
                        )
                        # If caller requested Vaishnava tradition, shift the primary date
                        if trad_rules.get("ekadashi") == "arunodaya":
                            entry["date"] = entry["date_vaishnava"]
                    else:
                        entry["date_smarta"]    = entry["date"]
                        entry["date_vaishnava"] = entry["date"]
                        entry["dosh_notes"].append(
                            f"✅ Shuddha Ekadashi — Dashami does not touch Arunodaya ({arun_str}). "
                            "Both Smarta and Vaishnava traditions observe on this date."
                        )

                # Vridhi / Ksheya notes
                if entry["tithi_type"] == "vridhi":
                    if rule.get("no_vridhi_shift"):
                        entry["dosh_notes"].append(
                            f"📅 Vridhi Tithi — {entry['tithi_name']} spans two consecutive sunrises (>24h). "
                            "Festival date is the first day (Madhyahna/Swayamsiddha rule applies)."
                        )
                    else:
                        entry["dosh_notes"].append(
                            f"📅 Vridhi Tithi — {entry['tithi_name']} spans two consecutive sunrises (>24h). "
                            "Date shown is the SECOND day per Dharmashastra."
                        )
                elif entry["tithi_type"] == "ksheya":
                    entry["dosh_notes"].append(
                        f"⚡ Ksheya Tithi — {entry['tithi_name']} is very short (~<20h), skipped by one sunrise. "
                        "Festival date shifts per local panchangam tradition."
                    )

            # ── Multi-day tithi range (Navratri etc.) ──────────────────
            elif rule["type"] == "tithi_range":
                start_idx    = _rule_to_idx(rule["paksha"], rule["start"])
                end_idx      = _rule_to_idx(rule["paksha"], rule["end"])
                approx_month = rule["approx_month"]
                approx_day   = rule.get("approx_day", 15)
                jd_center    = swe.julday(year, approx_month, approx_day, 6.0 - tz)

                # In Adhik Maas: force search center to midpoint of Adhik range
                if (adhika_range is not None and adhik_month_name is not None
                        and festival.get("month") == adhik_month_name):
                    jd_center = (adhika_range[0] + adhika_range[1]) / 2.0

                start_found = end_found = None
                for delta in sorted(range(-35, 36), key=abs):
                    for sub_h in [0, 6, 12, 18]:
                        test_jd = jd_center + float(delta) + sub_h / 24.0
                        tidx = _tithi_idx(test_jd)
                        if tidx == start_idx and start_found is None:
                            start_found = test_jd
                            break
                    if start_found is not None:
                        break
                if start_found is not None:
                    for delta in range(0, 14):
                        for sub_h in [0, 6, 12, 18]:
                            test_jd = start_found + float(delta) + sub_h / 24.0
                            if _tithi_idx(test_jd) == end_idx:
                                end_found = test_jd
                                break
                        if end_found is not None:
                            break

                # Skip Adhika month for range festivals — but not if festival belongs to Adhik month
                if start_found is not None:
                    if (adhika_range is not None and adhik_month_name is not None
                            and festival.get("month") == adhik_month_name):
                        pass  # observed in Adhik Maas
                    else:
                        start_found = _skip_adhika_if_needed(start_found, start_idx, adhika_range, jd_center=jd_center)
                    end_found = None
                    for delta in range(0, 12):
                        test_jd = start_found + float(delta)
                        if _tithi_idx(test_jd) == end_idx:
                            end_found = test_jd
                            break

                if start_found and end_found:
                    ts_jd = _find_tithi_start(start_idx, start_found)
                    te_jd = _find_tithi_end(end_idx, end_found)

                    entry["date"]             = _jd_to_date_str(start_found, tz)
                    entry["date_end"]         = _jd_to_date_str(end_found, tz)
                    entry["tithi_start"]      = _jd_to_time_str(ts_jd, tz)
                    entry["tithi_end"]        = _jd_to_time_str(te_jd, tz)
                    entry["tithi_start_date"] = _jd_to_date_str(ts_jd, tz)
                    entry["tithi_prev_day"]   = _jd_to_date_str(ts_jd, tz) < entry["date"]
                    entry["paksha"]      = PAKSHA_NAMES[start_idx]
                    entry["tithi_name"]  = f"{TITHI_NAMES[start_idx]} – {TITHI_NAMES[end_idx]}"
                    ref_dates[festival["name"]] = entry["date"]
                else:
                    entry["date"] = f"{year}-{approx_month:02d}-07"

        except Exception as exc:
            log.exception("Festival calculation failed for %s year=%d", festival.get("name", "?"), year)
            entry["dosh_notes"].append(f"Calculation error: {exc}")
            entry.setdefault("date", f"{year}-01-01")
            entry.setdefault("tithi_start_date", entry.get("date"))

        # ── Fill smarta/vaishnava defaults (if not set by sectarian logic above) ─
        current_date = entry.get("date")
        if current_date:
            if not entry.get("date_smarta"):
                entry["date_smarta"] = current_date
            if not entry.get("date_vaishnava"):
                entry["date_vaishnava"] = current_date

        # ── Grahan Dosha: check for eclipses near this festival ─────────
        festival_date = entry.get("date", "")
        if festival_date:
            nearby = _find_eclipses_near(festival_date, year_eclipses, window_days=1)
            for ec in nearby:
                entry["dosh_notes"].append(_eclipse_conflict_note(festival["name"], ec))

        # ── Adhik Maas note (festival JD falls inside Adhika month range) ─
        if adhik_note and festival_date and _festival_in_range(festival_date, adhika_range, tz):
            entry["dosh_notes"].append(
                f"📅 Adhik Maas alert: {adhik_note}. "
                f"{festival['name']} in this Adhik month — observe on the regular (Nija) month date."
            )

        # ── Kshaya Maas note (festival JD falls inside expunged month range) ─
        if kshaya_note and festival_date and _festival_in_range(festival_date, kshaya_range, tz):
            entry["dosh_notes"].append(
                f"⚠️ Kshaya Maas: {kshaya_note}"
            )

        # Ensure tithi_start_date always has a value
        if not entry.get("tithi_start_date"):
            entry["tithi_start_date"] = entry.get("date")
        results.append(entry)

    results.sort(key=lambda x: x.get("date", "9999"))
    _cache[cache_key] = results
    return results
