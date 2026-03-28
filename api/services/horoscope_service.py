"""
Horoscope service — dynamic daily predictions via Gemini.
Caches per (rashi, date) — regenerates once per day automatically.
Falls back to static text if Gemini is unavailable.
"""
import os
import json
from datetime import datetime, timezone, timedelta
from api.config import EPHE_PATH, RASHI_NAMES, DATA_DIR

# ── Static rashi metadata (sign ruler, lucky color/number) ───────────────────
_RASHI_META = {
    "Aries":       {"sanskrit": "Mesha",       "ruler": "Mars",    "lucky_color": "Red",       "lucky_number": 9},
    "Taurus":      {"sanskrit": "Vrishabha",   "ruler": "Venus",   "lucky_color": "Green",     "lucky_number": 6},
    "Gemini":      {"sanskrit": "Mithuna",     "ruler": "Mercury", "lucky_color": "Yellow",    "lucky_number": 5},
    "Cancer":      {"sanskrit": "Karka",       "ruler": "Moon",    "lucky_color": "White",     "lucky_number": 2},
    "Leo":         {"sanskrit": "Simha",       "ruler": "Sun",     "lucky_color": "Gold",      "lucky_number": 1},
    "Virgo":       {"sanskrit": "Kanya",       "ruler": "Mercury", "lucky_color": "Blue",      "lucky_number": 5},
    "Libra":       {"sanskrit": "Tula",        "ruler": "Venus",   "lucky_color": "Pink",      "lucky_number": 6},
    "Scorpio":     {"sanskrit": "Vrischika",   "ruler": "Mars",    "lucky_color": "Maroon",    "lucky_number": 8},
    "Sagittarius": {"sanskrit": "Dhanu",       "ruler": "Jupiter", "lucky_color": "Purple",    "lucky_number": 3},
    "Capricorn":   {"sanskrit": "Makara",      "ruler": "Saturn",  "lucky_color": "Black",     "lucky_number": 8},
    "Aquarius":    {"sanskrit": "Kumbha",      "ruler": "Saturn",  "lucky_color": "Blue",      "lucky_number": 4},
    "Pisces":      {"sanskrit": "Meena",       "ruler": "Jupiter", "lucky_color": "Sea Green", "lucky_number": 7},
}

# English rashi name → sidereal index (for matching planet positions)
_RASHI_TO_IDX = {name: i for i, name in enumerate(RASHI_NAMES)}
_IDX_TO_EN = {i: en for en, meta in _RASHI_META.items()
              for i, r in enumerate(RASHI_NAMES) if r == meta["sanskrit"]}

# Day cache: {(rashi, "YYYY-MM-DD"): prediction_str}
_day_cache: dict = {}


def _get_planet_positions() -> dict:
    """Return current sidereal planet positions."""
    try:
        import swisseph as swe
        swe.set_ephe_path(EPHE_PATH)
        swe.set_sid_mode(swe.SIDM_LAHIRI)
        now = datetime.now(timezone.utc)
        jd = swe.julday(now.year, now.month, now.day,
                        now.hour + now.minute / 60.0 + now.second / 3600.0)
        planet_ids = {
            "Sun":    swe.SUN,    "Moon":    swe.MOON,  "Mars":    swe.MARS,
            "Mercury":swe.MERCURY,"Jupiter": swe.JUPITER,"Venus":  swe.VENUS,
            "Saturn": swe.SATURN,
        }
        positions = {}
        for name, pid in planet_ids.items():
            res = swe.calc_ut(jd, pid, swe.FLG_SWIEPH | swe.FLG_SIDEREAL | swe.FLG_SPEED)
            lon = res[0][0]
            speed = res[0][3]
            rashi_idx = int(lon / 30)
            positions[name] = {
                "rashi": RASHI_NAMES[rashi_idx],
                "degree": round(lon % 30, 1),
                "retro": speed < 0,
            }
        # Rahu (mean node)
        res = swe.calc_ut(jd, swe.MEAN_NODE, swe.FLG_SWIEPH | swe.FLG_SIDEREAL)
        rahu_lon = res[0][0]
        positions["Rahu"] = {"rashi": RASHI_NAMES[int(rahu_lon / 30)], "degree": round(rahu_lon % 30, 1), "retro": True}
        return positions
    except Exception:
        return {}


def _build_planet_context(positions: dict) -> str:
    lines = []
    for name, p in positions.items():
        retro_note = " (retrograde)" if p.get("retro") else ""
        lines.append(f"  {name}: {p['rashi']} {p['degree']:.1f}°{retro_note}")
    return "\n".join(lines)


def _generate_prediction(rashi: str, positions: dict) -> str:
    """Generate prediction via Gemini. Returns plain text (2-3 sentences)."""
    try:
        import google.genai as genai
        api_key = os.environ.get("GEMINI_API_KEY", "")
        if not api_key:
            return ""
        client = genai.Client(api_key=api_key)
        meta = _RASHI_META[rashi]
        planet_ctx = _build_planet_context(positions) if positions else "  (positions unavailable)"

        # Which planets are currently in this rashi or aspecting it?
        sanskrit = meta["sanskrit"]
        transiting_here = [n for n, p in positions.items() if p["rashi"] == sanskrit]
        transit_note = (
            f"Planets currently in {sanskrit}: {', '.join(transiting_here)}." if transiting_here
            else f"No planets currently in {sanskrit}."
        )

        prompt = f"""You are a Vedic astrology expert writing a concise daily horoscope.

Today's sidereal planetary positions (Lahiri ayanamsha):
{planet_ctx}

{transit_note}

Write a daily horoscope for {rashi} ({sanskrit}) rashi. Rules:
- 2-3 sentences only. Practical, grounded advice.
- Mention 1-2 specific current transits that most affect {rashi} today.
- No generic filler phrases like "the stars align" or "cosmic energies".
- Write in plain English, warm but direct tone.
- Do NOT mention lucky color or number (those are shown separately).

Reply with ONLY the horoscope text. No labels, no JSON, no preamble."""

        response = client.models.generate_content(
            model="models/gemini-2.5-flash",
            contents=prompt,
        )
        text = response.text.strip() if response.text else ""
        return text
    except Exception:
        return ""


def get_horoscope(rashi: str, period: str = "daily") -> dict:
    meta = _RASHI_META.get(rashi)
    if not meta:
        raise ValueError(f"Unknown rashi: {rashi}")

    today = datetime.now(timezone(timedelta(hours=5, minutes=30))).strftime("%Y-%m-%d")
    cache_key = (rashi, today)

    # Return cached prediction if available for today
    if cache_key in _day_cache:
        prediction = _day_cache[cache_key]
    else:
        # Get live planet positions and generate prediction
        positions = _get_planet_positions()
        prediction = _generate_prediction(rashi, positions)

        # Fallback: static prediction if Gemini failed
        if not prediction:
            try:
                static_path = os.path.join(DATA_DIR, "static_horoscopes.json")
                with open(static_path) as f:
                    static = json.load(f)
                prediction = static.get(rashi, {}).get("prediction", "Consult the stars for guidance today.")
            except Exception:
                prediction = "Consult the stars for guidance today."

        _day_cache[cache_key] = prediction

    return {
        "rashi": rashi,
        "period": period,
        "prediction": prediction,
        "lucky_number": meta["lucky_number"],
        "lucky_color": meta["lucky_color"],
        "sign_ruler": meta["ruler"],
    }
