"""
Gochar Service — Current Planetary Transits over Natal Chart.

Flow:
  1. Get current positions of all 9 grahas (sidereal, Lahiri)
  2. Calculate which natal house each transiting planet is in
  3. Identify opportunities, cautions, sade sati

This is what a real Pandit checks FIRST for timing predictions.
"""
import swisseph as swe
from datetime import datetime, timezone

RASHIS = [
    "Mesh", "Vrish", "Mithun", "Kark", "Simha", "Kanya",
    "Tula", "Vrischik", "Dhanu", "Makar", "Kumbh", "Meen",
]
RASHI_IDX = {r: i for i, r in enumerate(RASHIS)}

PLANETS = {
    "Surya":   swe.SUN,
    "Chandra": swe.MOON,
    "Mangal":  swe.MARS,
    "Budh":    swe.MERCURY,
    "Guru":    swe.JUPITER,
    "Shukra":  swe.VENUS,
    "Shani":   swe.SATURN,
    "Rahu":    swe.MEAN_NODE,
}

# Transit effects from natal lagna — classical Vedic rules
TRANSIT_EFFECTS = {
    "Guru": {
        "shubh":   [1, 2, 5, 7, 9, 11],
        "ashubh":  [3, 6, 8, 10, 12],
    },
    "Shani": {
        "shubh":   [3, 6, 11],
        "ashubh":  [4, 5, 7, 8, 12],
    },
    "Rahu": {
        "shubh":   [3, 6, 10, 11],
        "ashubh":  [1, 2, 5, 8, 9, 12],
    },
    "Mangal": {
        "shubh":   [3, 6, 11],
        "ashubh":  [1, 2, 4, 7, 8, 12],
    },
}


def _get_current_positions() -> dict:
    """Get current sidereal positions of all 9 grahas."""
    try:
        from api.config import EPHE_PATH
        swe.set_ephe_path(EPHE_PATH)
    except Exception:
        pass

    swe.set_sid_mode(swe.SIDM_LAHIRI)
    now = datetime.now(timezone.utc)
    jd = swe.julday(
        now.year, now.month, now.day,
        now.hour + now.minute / 60.0 + now.second / 3600.0,
    )

    positions = {}
    for name, pid in PLANETS.items():
        flags = swe.FLG_SIDEREAL | swe.FLG_SPEED
        pos, _ = swe.calc_ut(jd, pid, flags)
        lon = pos[0]
        idx = int(lon / 30) % 12
        positions[name] = {
            "rashi": RASHIS[idx],
            "rashi_idx": idx,
            "degree": round(lon % 30, 2),
        }

    # Ketu = opposite Rahu
    rahu_idx = positions["Rahu"]["rashi_idx"]
    ketu_idx = (rahu_idx + 6) % 12
    ketu_deg = round((positions["Rahu"]["degree"]) % 30, 2)
    positions["Ketu"] = {
        "rashi": RASHIS[ketu_idx],
        "rashi_idx": ketu_idx,
        "degree": ketu_deg,
    }

    return positions


def get_current_planet_positions() -> dict:
    """Public API used by the router for current graha positions."""
    return _get_current_positions()


def get_transits(natal_lagna_rashi: str, natal_moon_rashi: str) -> dict:
    """
    Main function — returns complete transit (gochar) analysis.

    natal_lagna_rashi: rashi name string, e.g. "Kanya"
    natal_moon_rashi:  rashi name string, e.g. "Mithun"

    Returns:
      current_positions: {planet: "Rashi (XH)"}
      opportunities: list of positive transit messages
      cautions: list of caution messages
      sade_sati: bool
      summary: one-line summary of key planets
    """
    lagna_idx = RASHI_IDX.get(natal_lagna_rashi, 0)
    moon_idx  = RASHI_IDX.get(natal_moon_rashi, 0)

    current = _get_current_positions()

    # Calculate which natal house each transiting planet occupies
    transits = {}
    for planet, data in current.items():
        house = ((data["rashi_idx"] - lagna_idx) % 12) + 1
        transits[planet] = {
            "current_rashi":  data["rashi"],
            "current_degree": data["degree"],
            "natal_house":    house,
        }

    opportunities = []
    cautions      = []
    sade_sati     = False

    # --- Guru (Jupiter) — most important for timing ---
    guru = transits.get("Guru", {})
    if guru:
        h = guru["natal_house"]
        eff = TRANSIT_EFFECTS["Guru"]
        if h in eff["shubh"]:
            opportunities.append(
                f"Guru {guru['current_rashi']} mein ({h}H) — growth aur expansion ka samay, opportunities aaenge"
            )
        elif h in eff["ashubh"]:
            cautions.append(
                f"Guru {guru['current_rashi']} mein ({h}H) — growth slow ho sakti hai, patience rakho"
            )

    # --- Shani (Saturn) + Sade Sati check ---
    shani = transits.get("Shani", {})
    if shani:
        diff = abs(current["Shani"]["rashi_idx"] - moon_idx)
        if diff > 6:
            diff = 12 - diff
        sade_sati = diff <= 1
        if sade_sati:
            cautions.append(
                f"SADE SATI CHAL RAHI HAI — Shani {shani['current_rashi']} mein, "
                f"natal Chandra ke paas. Discipline, patience aur hard work se hi safalta milegi."
            )

        h = shani["natal_house"]
        eff = TRANSIT_EFFECTS["Shani"]
        if h in eff["shubh"] and not sade_sati:
            opportunities.append(
                f"Shani {shani['current_rashi']} mein ({h}H) — karmic mehnat rewarded hogi, steady progress"
            )
        elif h in eff["ashubh"]:
            cautions.append(
                f"Shani {shani['current_rashi']} mein ({h}H) — obstacles possible, slow & steady approach best"
            )

    # --- Rahu ---
    rahu = transits.get("Rahu", {})
    if rahu:
        h = rahu["natal_house"]
        eff = TRANSIT_EFFECTS["Rahu"]
        if h in eff["shubh"]:
            opportunities.append(
                f"Rahu {rahu['current_rashi']} mein ({h}H) — sudden opportunities, unconventional paths kaam aayenge"
            )
        elif h in eff["ashubh"]:
            cautions.append(
                f"Rahu {rahu['current_rashi']} mein ({h}H) — clarity rakh, illusions aur overconfidence se bacho"
            )

    # --- Mangal (for short-term timing) ---
    mangal = transits.get("Mangal", {})
    if mangal:
        h = mangal["natal_house"]
        eff = TRANSIT_EFFECTS["Mangal"]
        if h in eff["shubh"]:
            opportunities.append(
                f"Mangal {mangal['current_rashi']} mein ({h}H) — energy aur action ke liye good time (short-term)"
            )
        elif h in eff["ashubh"]:
            cautions.append(
                f"Mangal {mangal['current_rashi']} mein ({h}H) — impulsive decisions avoid karo (short-term)"
            )

    return {
        "current_positions": {
            p: f"{d['current_rashi']} ({d['natal_house']}H)"
            for p, d in transits.items()
        },
        "opportunities": opportunities,
        "cautions":      cautions,
        "sade_sati":     sade_sati,
        "summary": (
            f"Guru: {current['Guru']['rashi']} | "
            f"Shani: {current['Shani']['rashi']} | "
            f"Rahu: {current['Rahu']['rashi']} | "
            f"Ketu: {current['Ketu']['rashi']}"
        ),
    }
