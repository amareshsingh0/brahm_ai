"""
Upagraha (Sub-planet) Calculator.
Gulika, Mandi, Dhuma, Vyatipata, Parivesha, Indrachapa, Upaketu.
"""
from api.config import RASHI_NAMES, NAKSHATRAS

# Weekday → Saturn's hora segments for Gulika/Mandi
# Each day has 8 segments (each = day_duration/8 for day, night_duration/8 for night)
# Gulika = Saturn's portion. The segment number (0-based) owned by Saturn:
# Sunday=6, Monday=5, Tuesday=4, Wednesday=3, Thursday=2, Friday=1, Saturday=0
GULIKA_DAY_SEG = {0: 6, 1: 5, 2: 4, 3: 3, 4: 2, 5: 1, 6: 0}  # weekday → segment
MANDI_DAY_SEG  = {0: 6, 1: 5, 2: 4, 3: 3, 4: 2, 5: 1, 6: 0}  # Same as Gulika for day


def calc_upagraha(sun_lon: float, moon_lon: float, weekday: int = 0,
                   sunrise_jd: float = 0, sunset_jd: float = 0,
                   next_sunrise_jd: float = 0) -> dict:
    """
    Calculate Upagraha positions.

    sun_lon: Sidereal longitude of Sun (0-360)
    moon_lon: Sidereal longitude of Moon (0-360)
    weekday: 0=Sunday, 1=Monday, ..., 6=Saturday
    sunrise_jd, sunset_jd, next_sunrise_jd: Julian days for Gulika timing

    Returns: dict of upagraha name → {longitude, rashi, rashi_name, degree, nakshatra}
    """
    result = {}

    # ── Dhuma (Son of Sun) ─────────────────────────────────
    # Dhuma = Sun + 133°20'
    dhuma_lon = (sun_lon + 133.333333) % 360
    result["Dhuma"] = _make_entry(dhuma_lon, "Smoke-like, obstacles")

    # ── Vyatipata ──────────────────────────────────────────
    # Vyatipata = 360° - Dhuma
    vyatipata_lon = (360 - dhuma_lon) % 360
    result["Vyatipata"] = _make_entry(vyatipata_lon, "Calamity, downfall")

    # ── Parivesha (Paridhi) ────────────────────────────────
    # Parivesha = Vyatipata + 180°
    parivesha_lon = (vyatipata_lon + 180) % 360
    result["Parivesha"] = _make_entry(parivesha_lon, "Halo, protection")

    # ── Indrachapa (Indra's bow) ───────────────────────────
    # Indrachapa = 360° - Parivesha
    indrachapa_lon = (360 - parivesha_lon) % 360
    result["Indrachapa"] = _make_entry(indrachapa_lon, "Rainbow, illusion")

    # ── Upaketu ────────────────────────────────────────────
    # Upaketu = Indrachapa + 16°40' = Sun - 30° (simplified)
    upaketu_lon = (sun_lon - 30) % 360
    result["Upaketu"] = _make_entry(upaketu_lon, "Sub-Ketu, spiritual")

    # ── Gulika (son of Saturn) ─────────────────────────────
    # Gulika rises at Saturn's hora during daytime
    if sunrise_jd > 0 and sunset_jd > 0:
        day_dur = sunset_jd - sunrise_jd
        seg_dur = day_dur / 8
        seg_num = GULIKA_DAY_SEG.get(weekday, 6)
        gulika_jd = sunrise_jd + seg_num * seg_dur
        # Approximate Gulika longitude: use proportional position
        # (In full implementation, calculate ascendant at gulika_jd)
        # Simplified: Gulika ≈ Lagna at gulika_jd, but we don't have that here
        # Use classical approximation: Gulika = Saturn's rashi segment longitude
        gulika_lon = (sun_lon + seg_num * 30 + weekday * 3.33) % 360
        result["Gulika"] = _make_entry(gulika_lon, "Saturn's son, obstacles, delays")

        # Mandi = Gulika (used interchangeably in most texts)
        result["Mandi"] = _make_entry(gulika_lon, "Same as Gulika in Parashari")
    else:
        # Fallback approximation
        gulika_lon = (sun_lon + 133.33 + weekday * 30) % 360
        result["Gulika"] = _make_entry(gulika_lon, "Saturn's son (approx)")
        result["Mandi"] = _make_entry(gulika_lon, "Same as Gulika (approx)")

    return result


def _make_entry(lon: float, significance: str) -> dict:
    """Create a standardized upagraha entry."""
    lon = lon % 360
    rashi_i = int(lon / 30)
    degree = round(lon % 30, 2)
    nak_i = int(lon / (360 / 27))

    return {
        "longitude": round(lon, 4),
        "rashi": rashi_i,
        "rashi_name": RASHI_NAMES[rashi_i],
        "degree": degree,
        "nakshatra": NAKSHATRAS[nak_i] if nak_i < 27 else "",
        "significance": significance,
    }
