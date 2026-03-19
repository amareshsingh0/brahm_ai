"""
Monthly Hindu Panchang Calendar Service.
Returns per-day tithi, nakshatra, yoga, sunrise/sunset and festival data for a month.
"""
from __future__ import annotations

import calendar as cal_mod
import functools
from datetime import date, timedelta
from typing import Any, Dict, List

import swisseph as swe

from api.services.festival_service import (
    _actual_sunrise_jd,
    _actual_sunset_jd,
    _tithi_idx,
    _jd_to_time_str,
    _get_adhik_maas_note,
    _get_kshaya_masa_note,
    _get_lunar_month_name,
    get_festival_calendar,
    TITHI_NAMES,
)

# ─── Constants ────────────────────────────────────────────────────────────────

NAKSHATRA_NAMES = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha",
    "Ardra", "Punarvasu", "Pushya", "Ashlesha", "Magha",
    "P.Phalguni", "U.Phalguni", "Hasta", "Chitra", "Swati",
    "Vishakha", "Anuradha", "Jyeshtha", "Mula", "P.Ashadha",
    "U.Ashadha", "Shravana", "Dhanishtha", "Shatabhisha",
    "P.Bhadra", "U.Bhadra", "Revati",
]

YOGA_NAMES = [
    "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
    "Atiganda", "Sukarma", "Dhriti", "Shula", "Ganda",
    "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
    "Siddhi", "Vyatipata", "Variyana", "Parigha", "Shiva",
    "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma", "Indra", "Vaidhriti",
]

# Python weekday 0=Mon → Vedic Vara
VARA_NAMES = [
    "Somavara", "Mangalavara", "Budhavara", "Guruvara",
    "Shukravara", "Shanivara", "Ravivara",
]

# Sunday-first display names (index = (py_weekday+1)%7)
DISPLAY_DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]

# Rahu Kaal segment (1-8, first eighth = 1) per weekday (0=Mon Python convention)
# Sun=8, Mon=2, Tue=7, Wed=5, Thu=6, Fri=4, Sat=3
RAHU_KAAL_SEGMENT = [2, 7, 5, 6, 4, 3, 8]


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _nakshatra_idx(jd: float) -> int:
    swe.set_sid_mode(swe.SIDM_LAHIRI)
    moon_lon = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)[0][0]
    return int(moon_lon * 27 / 360) % 27


def _yoga_idx(jd: float) -> int:
    swe.set_sid_mode(swe.SIDM_LAHIRI)
    sun_lon  = swe.calc_ut(jd, swe.SUN,  swe.FLG_SIDEREAL)[0][0]
    moon_lon = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)[0][0]
    return int(((sun_lon + moon_lon) % 360) * 27 / 360) % 27


def _rahu_kaal_str(sr_jd: float, ss_jd: float, py_weekday: int, tz: float) -> str:
    """Return 'HH:MM – HH:MM' string for Rahu Kaal."""
    seg    = RAHU_KAAL_SEGMENT[py_weekday]          # 1-8
    eighth = (ss_jd - sr_jd) / 8.0
    rk_s   = _jd_to_time_str(sr_jd + (seg - 1) * eighth, tz)
    rk_e   = _jd_to_time_str(sr_jd + seg * eighth, tz)
    return f"{rk_s} – {rk_e}"


def _vikram_samvat(year: int, month: int) -> int:
    """Approximate Vikram Samvat year. VS starts at Chaitra (~April)."""
    return year + 57 if month >= 4 else year + 56


# Cache yearly festivals — navigating months of same year reuses calculation
@functools.lru_cache(maxsize=128)
def _cached_festivals(
    year: int, lat: float, lon: float, tz: float,
    tradition: str, lunar_system: str,
) -> tuple:
    result = get_festival_calendar(
        year, lat, lon, tz,
        tradition=tradition, lunar_system=lunar_system,
    )
    return tuple(result)  # must be hashable for lru_cache


# ─── Main entry point ─────────────────────────────────────────────────────────

def get_monthly_calendar(
    year: int, month: int,
    lat: float, lon: float, tz: float,
    tradition: str = "smarta",
    lunar_system: str = "amanta",
) -> Dict[str, Any]:
    """Return all per-day panchang + festival data for the given month."""

    # Fetch yearly festivals (cached)
    yearly_festivals = list(_cached_festivals(year, lat, lon, tz, tradition, lunar_system))

    # Build date → [festival, ...] lookup
    fest_by_date: Dict[str, List] = {}
    for f in yearly_festivals:
        start_d = f.get("date", "")
        if not start_d:
            continue
        fest_by_date.setdefault(start_d, []).append(f)

        # Only expand festivals that genuinely span multiple days (Navratri = 9 days, etc.)
        # Single-tithi festivals set date_end = next day when tithi crosses midnight — don't expand those.
        end_d = f.get("date_end")
        if end_d and end_d != start_d and (date.fromisoformat(end_d) - date.fromisoformat(start_d)).days > 1:
            try:
                cur = date.fromisoformat(start_d) + timedelta(days=1)
                end = date.fromisoformat(end_d)
                while cur <= end:
                    ds = cur.isoformat()
                    # Only add a lightweight reference (don't re-duplicate heavy fields)
                    fest_by_date.setdefault(ds, []).append({
                        "name": f["name"],
                        "hindi": f.get("hindi", ""),
                        "icon": f.get("icon", "🔔"),
                        "date": ds,
                        "deity": f.get("deity", ""),
                        "significance": f.get("significance", ""),
                        "tithi_name": f.get("tithi_name", ""),
                        "paksha": f.get("paksha", ""),
                        "_continuing": True,
                    })
                    cur += timedelta(days=1)
            except Exception:
                pass

    _, days_in_month = cal_mod.monthrange(year, month)
    month_name = cal_mod.month_name[month]

    # first_weekday: 0=Sun … 6=Sat  (Sunday-first grid offset)
    py_wd = date(year, month, 1).weekday()   # 0=Mon
    first_weekday = (py_wd + 1) % 7          # 0=Sun

    today_str = date.today().isoformat()
    days: List[Dict[str, Any]] = []

    for day_num in range(1, days_in_month + 1):
        date_str = f"{year:04d}-{month:02d}-{day_num:02d}"
        d_obj    = date(year, month, day_num)
        py_wd    = d_obj.weekday()  # 0=Mon

        try:
            sr_jd = _actual_sunrise_jd(year, month, day_num, lat, lon, tz)
            ss_jd = _actual_sunset_jd(year, month, day_num, lat, lon, tz)
            sunrise = _jd_to_time_str(sr_jd, tz)
            sunset  = _jd_to_time_str(ss_jd, tz)

            tidx      = _tithi_idx(sr_jd)
            tithi     = TITHI_NAMES[tidx]
            paksha    = "Shukla" if tidx < 15 else "Krishna"
            paksha_s  = "S" if tidx < 15 else "K"
            tithi_num = (tidx % 15) + 1  # 1-15

            nak_idx   = _nakshatra_idx(sr_jd)
            yoga_i    = _yoga_idx(sr_jd)
            nakshatra = NAKSHATRA_NAMES[nak_idx]
            yoga      = YOGA_NAMES[yoga_i]
            vara      = VARA_NAMES[py_wd]

            rahu_kaal  = _rahu_kaal_str(sr_jd, ss_jd, py_wd, tz)
            lunar_month = _get_lunar_month_name(sr_jd, tz, lunar_system) or ""

            day_festivals = fest_by_date.get(date_str, [])

            days.append({
                "date":         date_str,
                "day":          day_num,
                "weekday":      DISPLAY_DAYS[(py_wd + 1) % 7],
                "vara":         vara,
                "sunrise":      sunrise,
                "sunset":       sunset,
                "tithi_idx":    tidx,
                "tithi":        tithi,
                "tithi_num":    tithi_num,
                "paksha":       paksha,
                "paksha_short": paksha_s,
                "nakshatra":    nakshatra,
                "yoga":         yoga,
                "rahu_kaal":    rahu_kaal,
                "lunar_month":  lunar_month,
                "festivals":    day_festivals,
                "is_today":     date_str == today_str,
                "is_purnima":   tidx == 14,
                "is_amavasya":  tidx == 29,
                "is_ekadashi":  tidx in (10, 25),
                "is_chaturthi": tidx in (3, 18),
                "is_pradosh":   tidx in (12, 27),
                "is_ashtami":   tidx in (7, 22),
                "has_festival": bool(day_festivals),
            })

        except Exception as exc:
            days.append({
                "date":         date_str,
                "day":          day_num,
                "weekday":      DISPLAY_DAYS[(py_wd + 1) % 7],
                "vara":         VARA_NAMES[py_wd],
                "sunrise":      "N/A",
                "sunset":       "N/A",
                "tithi_idx":    0,
                "tithi":        "N/A",
                "tithi_num":    0,
                "paksha":       "N/A",
                "paksha_short": "S",
                "nakshatra":    "N/A",
                "yoga":         "N/A",
                "rahu_kaal":    "N/A",
                "lunar_month":  "",
                "festivals":    [],
                "is_today":     date_str == today_str,
                "is_purnima":   False,
                "is_amavasya":  False,
                "is_ekadashi":  False,
                "is_chaturthi": False,
                "is_pradosh":   False,
                "is_ashtami":   False,
                "has_festival": False,
                "error":        str(exc),
            })

    adhik_note  = None
    kshaya_note = None
    try:
        adhik_note  = _get_adhik_maas_note(year, tz, lunar_system)
        kshaya_note = _get_kshaya_masa_note(year, tz, lunar_system)
    except Exception:
        pass

    # Compute Vikram Samvat year
    vikram_samvat = _vikram_samvat(year, month)

    # Lunar months present in this Gregorian month (collect unique non-empty values)
    lunar_months = list(dict.fromkeys(
        d["lunar_month"] for d in days if d.get("lunar_month")
    ))

    return {
        "year":             year,
        "month":            month,
        "month_name":       month_name,
        "days_in_month":    days_in_month,
        "first_weekday":    first_weekday,
        "tradition":        tradition,
        "lunar_system":     lunar_system,
        "vikram_samvat":    vikram_samvat,
        "lunar_months":     lunar_months,
        "adhik_maas_note":  adhik_note,
        "kshaya_maas_note": kshaya_note,
        "days":             days,
    }
