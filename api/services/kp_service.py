"""
Krishnamurti Paddhati (KP) Astrology Service.
Computes Sub-Lords for all planets and house cusps.

KP Sub-lord method:
  Each nakshatra (13°20') is divided into 9 sub-periods proportional
  to Vimshottari dasha years (total 120 years). The sub-period sequence
  starts from the nakshatra's own lord.
"""
from datetime import datetime, timedelta
from typing import Optional, Tuple

try:
    import swisseph as swe
    SWE_AVAILABLE = True
except ImportError:
    SWE_AVAILABLE = False

from api.config import EPHE_PATH, AYANAMSHA_MODES, RASHI_NAMES, NAKSHATRAS

DASHA_SEQ   = ["Ketu","Shukra","Surya","Chandra","Mangal","Rahu","Guru","Shani","Budh"]
DASHA_YEARS = {"Ketu":7,"Shukra":20,"Surya":6,"Chandra":10,"Mangal":7,"Rahu":18,"Guru":16,"Shani":19,"Budh":17}
TOTAL_YEARS = 120.0
NAK_SPAN    = 360.0 / 27          # 13.333...°
SUB_SPAN    = NAK_SPAN / TOTAL_YEARS  # degrees per year of sub-period

GRAHA_EN = {
    "Surya":"Sun","Chandra":"Moon","Mangal":"Mars","Budh":"Mercury",
    "Guru":"Jupiter","Shukra":"Venus","Shani":"Saturn","Rahu":"Rahu","Ketu":"Ketu",
}

def _sub_lord(longitude: float) -> dict:
    """
    Given a sidereal longitude (0–360), return the KP star lord, sub-lord,
    sub-sub-lord and the precise sub-span boundaries.
    """
    nak_i   = int(longitude / NAK_SPAN)
    nak_i   = min(nak_i, 26)
    nak_pos = longitude - nak_i * NAK_SPAN   # 0 to NAK_SPAN within nakshatra

    # Star lord — start of sequence for this nakshatra
    nak_lord_i = nak_i % 9
    start_idx  = nak_lord_i                  # index in DASHA_SEQ

    # Walk sub-lords
    acc    = 0.0
    sub    = DASHA_SEQ[start_idx]
    sub_sub = DASHA_SEQ[start_idx]
    sub_start_deg = 0.0
    sub_end_deg   = 0.0

    for i in range(9):
        pl   = DASHA_SEQ[(start_idx + i) % 9]
        span = DASHA_YEARS[pl] * SUB_SPAN
        if acc + span > nak_pos:
            sub           = pl
            sub_start_deg = nak_i * NAK_SPAN + acc
            sub_end_deg   = nak_i * NAK_SPAN + acc + span
            # Sub-sub lord within sub-period
            sub_acc   = 0.0
            sub_sub_i = DASHA_SEQ.index(pl)
            for j in range(9):
                spl       = DASHA_SEQ[(sub_sub_i + j) % 9]
                sub_span  = DASHA_YEARS[spl] / TOTAL_YEARS * span
                if sub_acc + sub_span > (nak_pos - acc):
                    sub_sub = spl
                    break
                sub_acc += sub_span
            break
        acc += span

    return {
        "star_lord":   DASHA_SEQ[start_idx],
        "sub_lord":    sub,
        "sub_sub_lord": sub_sub,
        "sub_span_start": round(sub_start_deg, 4),
        "sub_span_end":   round(sub_end_deg, 4),
    }


def calc_kp(
    year: int, month: int, day: int,
    hour: int, minute: int,
    lat: float, lon: float, tz: float,
    name: str = "",
) -> Tuple[Optional[dict], Optional[str]]:
    """
    Compute full KP chart: planet sub-lords + house cusp sub-lords.
    Always uses Krishnamurti ayanamsha.
    """
    if not SWE_AVAILABLE:
        return None, "pyswisseph not installed"

    swe.set_ephe_path(EPHE_PATH)
    swe.set_sid_mode(swe.SIDM_KRISHNAMURTI)

    dt_utc = datetime(year, month, day, hour, minute) - timedelta(hours=tz)
    jd     = swe.julday(dt_utc.year, dt_utc.month, dt_utc.day,
                        dt_utc.hour + dt_utc.minute / 60.0)
    ayanamsha = swe.get_ayanamsa_ut(jd)

    PLANET_IDS = [
        ("Surya",   swe.SUN),
        ("Chandra", swe.MOON),
        ("Mangal",  swe.MARS),
        ("Budh",    swe.MERCURY),
        ("Guru",    swe.JUPITER),
        ("Shukra",  swe.VENUS),
        ("Shani",   swe.SATURN),
        ("Rahu",    swe.MEAN_NODE),
    ]

    # ── Planets ────────────────────────────────────────────────────────
    planets = {}
    rahu_lon = 0.0
    for name_g, pid in PLANET_IDS:
        res  = swe.calc_ut(jd, pid, swe.FLG_SWIEPH | swe.FLG_SIDEREAL)
        lon_g = res[0][0]
        if name_g == "Rahu":
            rahu_lon = lon_g
        nak_i    = int(lon_g / NAK_SPAN)
        nak_i    = min(nak_i, 26)
        rashi_i  = int(lon_g / 30)
        sub_data = _sub_lord(lon_g)
        planets[name_g] = {
            "longitude":    round(lon_g, 4),
            "degree":       round(lon_g % 30, 2),
            "rashi":        RASHI_NAMES[rashi_i],
            "rashi_idx":    rashi_i,
            "nakshatra":    NAKSHATRAS[nak_i],
            "nak_idx":      nak_i,
            "retro":        res[0][3] < 0,
            **sub_data,
        }

    # Ketu = 180° from Rahu
    ketu_lon  = (rahu_lon + 180) % 360
    nak_k     = min(int(ketu_lon / NAK_SPAN), 26)
    rashi_k   = int(ketu_lon / 30)
    planets["Ketu"] = {
        "longitude":  round(ketu_lon, 4),
        "degree":     round(ketu_lon % 30, 2),
        "rashi":      RASHI_NAMES[rashi_k],
        "rashi_idx":  rashi_k,
        "nakshatra":  NAKSHATRAS[nak_k],
        "nak_idx":    nak_k,
        "retro":      True,
        **_sub_lord(ketu_lon),
    }

    # ── House cusps (Placidus) ─────────────────────────────────────────
    cusps_trop, ascmc = swe.houses(jd, lat, lon, b"P")
    lagna_sid  = (ascmc[0] - ayanamsha) % 360
    lagna_i    = int(lagna_sid / 30)

    cusps = []
    for i, c in enumerate(cusps_trop[:12]):
        c_sid     = (c - ayanamsha) % 360
        nak_c     = min(int(c_sid / NAK_SPAN), 26)
        rashi_c   = int(c_sid / 30)
        sub_data  = _sub_lord(c_sid)
        cusps.append({
            "house":      i + 1,
            "longitude":  round(c_sid, 4),
            "degree":     round(c_sid % 30, 2),
            "rashi":      RASHI_NAMES[rashi_c],
            "nakshatra":  NAKSHATRAS[nak_c],
            **sub_data,
        })

    # ── Significators ──────────────────────────────────────────────────
    # For each planet: which houses does it signify?
    # Rules: (a) house it occupies, (b) houses it owns (sign lord), (c) houses aspected
    from api.config import RASHI_LORDS
    sigs = {}
    for pname, pdata in planets.items():
        occupied_house = ((pdata["rashi_idx"] - lagna_i) % 12) + 1
        owned_houses   = [((ri - lagna_i) % 12) + 1
                          for ri, lord in RASHI_LORDS.items() if lord == pname]
        sigs[pname] = sorted(set([occupied_house] + owned_houses))

    return {
        "name":       name,
        "ayanamsha":  round(ayanamsha, 4),
        "lagna":      {"rashi": RASHI_NAMES[lagna_i], "longitude": round(lagna_sid, 4),
                       **_sub_lord(lagna_sid)},
        "planets":    planets,
        "cusps":      cusps,
        "significators": sigs,
    }, None
