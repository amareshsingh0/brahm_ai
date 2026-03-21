"""
Varshphal (Solar Return) Service.
Finds the exact moment when the Sun returns to its natal longitude for a given year,
then computes the full chart at that moment.
"""
from datetime import datetime, timedelta
from typing import Tuple, Optional

try:
    import swisseph as swe
    SWE_AVAILABLE = True
except ImportError:
    SWE_AVAILABLE = False

from api.config import EPHE_PATH, AYANAMSHA_MODES
from api.services.kundali_service import calc_kundali


def _get_natal_sun_lon(
    year: int, month: int, day: int,
    hour: int, minute: int, tz: float,
    ayanamsha_mode: str,
) -> float:
    """Get the sidereal longitude of the natal Sun."""
    ayan_id = AYANAMSHA_MODES.get(ayanamsha_mode, 1)
    swe.set_sid_mode(ayan_id)
    dt_utc = datetime(year, month, day, hour, minute) - timedelta(hours=tz)
    jd = swe.julday(dt_utc.year, dt_utc.month, dt_utc.day,
                    dt_utc.hour + dt_utc.minute / 60.0)
    flags = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
    res = swe.calc_ut(jd, swe.SUN, flags)
    return res[0][0]


def _find_solar_return_jd(natal_sun_lon: float, target_year: int) -> float:
    """
    Binary-search the Julian Day when the Sun's sidereal longitude equals natal_sun_lon
    in the target_year. Starts from a rough estimate (March/April of target year)
    and converges within <1 arcsecond.
    """
    # Rough start: Sun longitude 0° = Mesha Sankranti ≈ April 14
    # shift start to ≈ natal_sun_lon / 360 of year before that
    approx_day = int(natal_sun_lon / 360 * 365.25)
    start_jd = swe.julday(target_year, 1, 1, 12.0) + approx_day

    jd = start_jd
    for _ in range(60):   # converges in <20 iterations
        flags = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
        res = swe.calc_ut(jd, swe.SUN, flags)
        cur_lon = res[0][0]
        diff = (natal_sun_lon - cur_lon + 180) % 360 - 180
        if abs(diff) < 0.0003:   # ~1 arcsecond
            break
        # Sun moves ~1°/day
        jd += diff / 360.0
    return jd


def _jd_to_local_dt(jd: float, tz: float) -> datetime:
    """Convert Julian Day to local datetime."""
    y, m, d, h = swe.revjul(jd)
    h_local = h + tz
    # Handle day overflow
    overflow_days = int(h_local // 24)
    h_local %= 24
    base = datetime(y, m, d) + timedelta(days=overflow_days)
    hour = int(h_local)
    minute = int((h_local - hour) * 60)
    return datetime(base.year, base.month, base.day, hour, minute)


def calc_varshphal(
    birth_year: int, birth_month: int, birth_day: int,
    birth_hour: int, birth_minute: int,
    birth_lat: float, birth_lon: float, birth_tz: float,
    target_year: int,
    current_lat: Optional[float] = None,
    current_lon: Optional[float] = None,
    current_tz: Optional[float] = None,
    ayanamsha_mode: str = "lahiri",
) -> Tuple[Optional[dict], Optional[str]]:
    """
    Compute Varshphal (Solar Return) chart for target_year.
    Uses current_lat/lon/tz if provided (person's current residence), else birth location.
    """
    if not SWE_AVAILABLE:
        return None, "pyswisseph not installed"

    swe.set_ephe_path(EPHE_PATH)
    ayan_id = AYANAMSHA_MODES.get(ayanamsha_mode, 1)
    swe.set_sid_mode(ayan_id)

    lat = current_lat if current_lat is not None else birth_lat
    lon = current_lon if current_lon is not None else birth_lon
    tz  = current_tz  if current_tz  is not None else birth_tz

    # 1. Get natal Sun longitude
    natal_sun = _get_natal_sun_lon(
        birth_year, birth_month, birth_day, birth_hour, birth_minute,
        birth_tz, ayanamsha_mode,
    )

    # 2. Find exact JD of solar return in target_year
    sr_jd = _find_solar_return_jd(natal_sun, target_year)

    # 3. Convert to local datetime at return location
    sr_dt = _jd_to_local_dt(sr_jd, tz)

    # 4. Compute full chart at solar return moment
    chart, err = calc_kundali(
        sr_dt.year, sr_dt.month, sr_dt.day,
        sr_dt.hour, sr_dt.minute,
        lat, lon, tz,
        name=f"Varshphal {target_year}",
        place="",
        ayanamsha_mode=ayanamsha_mode,
        calc_options=[],
    )
    if err:
        return None, err

    # 5. Add varshphal metadata
    chart["varshphal_year"]        = target_year
    chart["solar_return_datetime"] = sr_dt.strftime("%d %B %Y, %H:%M")
    chart["natal_sun_longitude"]   = round(natal_sun, 4)
    chart["current_location"]      = {"lat": lat, "lon": lon, "tz": tz}

    # 6. Year themes from chart (simple rules)
    grahas  = chart.get("grahas", {})
    lagna_h = {h["house"]: h for h in chart.get("houses", [])}
    themes  = []

    # Surya in kendra → leadership/recognition year
    if grahas.get("Surya", {}).get("house") in [1, 4, 7, 10]:
        themes.append("Authority & recognition — Surya in kendra brings leadership opportunities")
    # Guru in 1/5/9 → wisdom, growth, fortune
    if grahas.get("Guru", {}).get("house") in [1, 5, 9]:
        themes.append("Growth & fortune — Guru in trikona blesses with expansion and wisdom")
    # Shani in 10 → career discipline and hard work
    if grahas.get("Shani", {}).get("house") == 10:
        themes.append("Career discipline — Shani in 10H demands focused, systematic effort")
    # Chandra strong (1/4/7) → emotional stability
    if grahas.get("Chandra", {}).get("house") in [1, 4, 7]:
        themes.append("Emotional stability — Moon in kendra supports mental peace and relationships")
    # Rahu/Ketu axis 1-7 → major life changes, identity shifts
    rahu_h = grahas.get("Rahu", {}).get("house")
    if rahu_h in [1, 7]:
        themes.append("Transformation — Rahu-Ketu on 1-7 axis brings identity and relationship changes")
    # Shukra in 1/2/7 → relationships, wealth
    if grahas.get("Shukra", {}).get("house") in [1, 2, 7]:
        themes.append("Love & wealth — Shukra placement brings relationship and financial opportunities")
    # Mangal in 1/8/10 → energy and new beginnings
    if grahas.get("Mangal", {}).get("house") in [1, 10]:
        themes.append("Drive & ambition — Mangal energizes career and personal initiative this year")
    if not themes:
        themes.append("A year of steady progress — focus on consolidation and consistent effort")

    chart["year_themes"] = themes

    return chart, None
