"""
Upagraha (Sub-planet) Calculator.
Calculates 11 upagrahas matching Drik Panchang:
  Dhuma, Vyatipata, Parivesha, Indrachapa, Upaketu  (Sun-derived)
  Gulika, Mandi, Kala, Mrityu, ArdhaPrahara, YamaGhantaka  (hora-based)
"""
from api.config import RASHI_NAMES, NAKSHATRAS, NAKSHATRA_LORDS

# Chaldean weekday hora sequence (starting from weekday's lord)
# Planet order in Chaldean hours: Sun, Venus, Mercury, Moon, Saturn, Jupiter, Mars
_HORA_SEQ = ["Surya", "Shukra", "Budh", "Chandra", "Shani", "Guru", "Mangal"]

# Weekday lords (0=Sunday…6=Saturday) → index into _HORA_SEQ
_WEEKDAY_LORD_IDX = [0, 3, 6, 2, 5, 1, 4]

# Which daytime hora-segment (0-based, out of 8) belongs to each upagraha planet
# Key = planet name in Chaldean order index; Value = segment number
# Mandi/Gulika = Saturn's hora; Kala=Sun's; Mrityu=Mars's; ArdhaPrahara=Mercury's; YamaGhantaka=Jupiter's
_UPAGRAHA_PLANET = {
    "Mandi":        "Shani",
    "Gulika":       "Shani",   # Gulika = mid-point of Mandi's hora
    "Kala":         "Surya",
    "Mrityu":       "Mangal",
    "ArdhaPrahara": "Budh",
    "YamaGhantaka": "Guru",
}


def _hora_segment(weekday: int, planet: str) -> int:
    """Return the 0-based daytime segment number (0-7) for `planet` on `weekday`."""
    start_idx = _WEEKDAY_LORD_IDX[weekday]
    for seg in range(8):
        if _HORA_SEQ[(start_idx + seg) % 7] == planet:
            return seg
    return 0


def calc_upagraha(sun_lon: float, moon_lon: float, weekday: int = 0,
                   sunrise_jd: float = 0, sunset_jd: float = 0,
                   next_sunrise_jd: float = 0,
                   jd: float = 0, lat: float = 0, lon: float = 0,
                   ayanamsha: float = 0) -> dict:
    """
    Calculate all 11 Upagraha positions.

    sun_lon, moon_lon : sidereal longitudes
    weekday           : 0=Sunday … 6=Saturday
    sunrise_jd        : Julian day of sunrise
    sunset_jd         : Julian day of sunset
    jd, lat, lon      : for ascendant calculation at hora times (optional)
    ayanamsha         : for sidereal ascendant conversion
    """
    result = {}

    # ── Sun-derived upagrahas (always accurate) ────────────────────────
    dhuma_lon      = (sun_lon + 133.333333) % 360
    vyatipata_lon  = (360 - dhuma_lon)      % 360
    parivesha_lon  = (vyatipata_lon + 180)  % 360
    indrachapa_lon = (360 - parivesha_lon)  % 360
    upaketu_lon    = (sun_lon - 30)         % 360

    result["Dhuma"]      = _make_entry(dhuma_lon,      "Smoke — obstacles, affliction")
    result["Vyatipata"]  = _make_entry(vyatipata_lon,  "Calamity — sudden downfall")
    result["Parivesha"]  = _make_entry(parivesha_lon,  "Halo — protection, grace")
    result["Indrachapa"] = _make_entry(indrachapa_lon, "Rainbow — illusion, desires")
    result["Upaketu"]    = _make_entry(upaketu_lon,    "Sub-Ketu — spiritual disruption")

    # ── Hora-based upagrahas ───────────────────────────────────────────
    if sunrise_jd > 0 and sunset_jd > 0:
        day_dur = sunset_jd - sunrise_jd
        seg_dur = day_dur / 8.0

        def _hora_lon(planet_name: str, mid: bool = False) -> float:
            seg = _hora_segment(weekday, planet_name)
            hora_jd = sunrise_jd + seg * seg_dur + (seg_dur / 2 if mid else 0)
            return _ascendant_at(hora_jd, lat, lon, ayanamsha)

        mandi_lon  = _hora_lon("Shani", mid=False)
        gulika_lon = _hora_lon("Shani", mid=True)   # Gulika = mid of Mandi's hora
        kala_lon   = _hora_lon("Surya")
        mrityu_lon = _hora_lon("Mangal")
        ardha_lon  = _hora_lon("Budh")
        yama_lon   = _hora_lon("Guru")
    else:
        # Fallback: classical approximate formulas (no JD available)
        mandi_lon  = (sun_lon + 133.33 + weekday * 30) % 360
        gulika_lon = (mandi_lon + 14) % 360
        kala_lon   = (sun_lon + 56.25 * ((weekday + 2) % 8)) % 360
        mrityu_lon = (sun_lon + 56.25 * ((weekday + 3) % 8)) % 360
        ardha_lon  = (sun_lon + 56.25 * ((weekday + 5) % 8)) % 360
        yama_lon   = (sun_lon + 56.25 * ((weekday + 4) % 8)) % 360

    result["Mandi"]        = _make_entry(mandi_lon,  "Mandi (Saturn's hora start) — affliction, obstacles")
    result["Gulika"]       = _make_entry(gulika_lon, "Gulika (Saturn's hora mid) — delays, poison-like effects")
    result["Kala"]         = _make_entry(kala_lon,   "Kala (Sun's hora) — time, death-like influences")
    result["Mrityu"]       = _make_entry(mrityu_lon, "Mrityu (Mars's hora) — danger, accidents")
    result["ArdhaPrahara"] = _make_entry(ardha_lon,  "Ardha Prahara (Mercury's hora) — confusion, mixed results")
    result["YamaGhantaka"] = _make_entry(yama_lon,   "Yama Ghantaka (Jupiter's hora) — caution in benefic matters")

    return result


def _ascendant_at(hora_jd: float, lat: float, lon: float, ayanamsha: float) -> float:
    """Calculate sidereal ascendant longitude at a given Julian Day."""
    try:
        import swisseph as swe
        cusps, ascmc = swe.houses_ex(hora_jd, lat, lon, b'P')
        return (ascmc[0] - ayanamsha) % 360
    except Exception:
        return 0.0


def _make_entry(lon: float, significance: str) -> dict:
    """Create a standardized upagraha entry."""
    lon     = lon % 360
    rashi_i = int(lon / 30)
    degree  = round(lon % 30, 4)
    nak_i   = int(lon / (360 / 27))
    nak_lord = NAKSHATRA_LORDS[nak_i] if nak_i < 27 else ""

    # DMS format
    deg_int = int(degree)
    min_val = (degree - deg_int) * 60
    min_int = int(min_val)
    sec_int = int((min_val - min_int) * 60)
    dms = f"{deg_int:02d}° {min_int:02d}' {sec_int:02d}\""

    return {
        "longitude":    round(lon, 4),
        "rashi":        rashi_i,
        "rashi_name":   RASHI_NAMES[rashi_i],
        "degree":       degree,
        "dms":          dms,
        "nakshatra":    NAKSHATRAS[nak_i] if nak_i < 27 else "",
        "nakshatra_lord": nak_lord,
        "significance": significance,
    }
