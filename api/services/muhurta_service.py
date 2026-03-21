"""
Muhurta service — wraps panchang to find auspicious slots for an event.
"""
from datetime import date as _date, timedelta
from api.services.panchang_service import get_panchang, PANCHANG_AVAILABLE

# Inauspicious yogas to avoid
_INAUSPICIOUS_YOGAS = {"Vishkumbha", "Atiganda", "Shoola", "Ganda", "Vyaghata",
                        "Vajra", "Vyatipata", "Variyan", "Parigha", "Vaidhriti"}

# Choghadiya quality scores
_CHOG_SCORE = {"Amrit": 5, "Shubh": 4, "Labh": 3, "Chal": 2, "Char": 1,
               "Kaal": -4, "Rog": -4, "Udveg": -2}

# Activity-specific good nakshatras
_ACTIVITY_NAKS = {
    "marriage":   {"Rohini","Mrigashira","Magha","Uttara Phalguni","Hasta","Swati",
                   "Anuradha","Uttara Ashadha","Uttara Bhadrapada","Revati"},
    "travel":     {"Ashwini","Mrigashira","Punarvasu","Pushya","Hasta","Anuradha",
                   "Shravana","Dhanishtha","Revati"},
    "business":   {"Rohini","Mrigashira","Revati","Pushya","Hasta","Uttara Phalguni",
                   "Uttara Ashadha","Uttara Bhadrapada","Dhanishtha"},
    "medical":    {"Ashwini","Mrigashira","Pushya","Hasta","Anuradha","Shravana","Revati"},
    "education":  {"Ashwini","Rohini","Mrigashira","Punarvasu","Pushya","Hasta",
                   "Swati","Anuradha","Shravana","Revati"},
    "property":   {"Rohini","Uttara Phalguni","Hasta","Uttara Ashadha","Uttara Bhadrapada","Shravana"},
    "general":    {"Rohini","Mrigashira","Punarvasu","Pushya","Hasta","Swati",
                   "Anuradha","Shravana","Revati","Uttara Phalguni"},
}

# Activity-specific good weekdays (1=Sun…7=Sat)
_ACTIVITY_VARA = {
    "marriage":   {2, 4, 5, 6},   # Mon, Wed, Thu, Fri
    "travel":     {2, 4, 5, 6},
    "business":   {4, 5, 6},      # Wed, Thu, Fri
    "medical":    {2, 4, 5},
    "education":  {2, 4, 5, 6},
    "property":   {2, 4, 5},
    "general":    {1, 2, 3, 4, 5, 6, 7},
}

_VARA_IDX = {"Sunday":1,"Monday":2,"Tuesday":3,"Wednesday":4,
             "Thursday":5,"Friday":6,"Saturday":7}


def find_activity_muhurta(
    activity: str,
    start_date: str,
    days: int,
    lat: float,
    lon: float,
    tz: float,
) -> list:
    """
    Scan `days` days from start_date and return top-scored choghadiya slots
    for the given activity. Returns list of slot dicts, sorted by score desc.
    """
    if not PANCHANG_AVAILABLE:
        return []

    activity = activity.lower() if activity else "general"
    good_naks = _ACTIVITY_NAKS.get(activity, _ACTIVITY_NAKS["general"])
    good_vara = _ACTIVITY_VARA.get(activity, _ACTIVITY_VARA["general"])

    try:
        parts = start_date.split("-")
        base  = _date(int(parts[0]), int(parts[1]), int(parts[2]))
    except Exception:
        base = _date.today()

    all_slots = []
    for offset in range(min(days, 14)):
        d = base + timedelta(days=offset)
        try:
            p = get_panchang(d.year, d.month, d.day, lat, lon, tz)
        except Exception:
            continue

        nak_name  = p.get("nakshatra", {}).get("name", "")
        vara_name = p.get("vara", {}).get("name", "")
        vara_num  = _VARA_IDX.get(vara_name, 0)
        yoga_name = p.get("yoga", {}).get("name", "")
        rahukaal  = p.get("rahukaal", {})

        chog = p.get("choghadiya")
        if not chog:
            continue

        for period_type in ("day", "night"):
            for slot in chog.get(period_type, []):
                name = slot.get("name", "")
                score = _CHOG_SCORE.get(name, 0)

                # Nakshatra bonus
                if nak_name in good_naks:
                    score += 2

                # Vara bonus
                if vara_num in good_vara:
                    score += 1

                # Yoga bonus / penalty
                if yoga_name and yoga_name not in _INAUSPICIOUS_YOGAS:
                    score += 0.5
                else:
                    score -= 1

                # Rahu Kaal overlap penalty
                rk_start = rahukaal.get("start", "")
                rk_end   = rahukaal.get("end", "")
                if rk_start and slot.get("start", "") == rk_start:
                    score -= 3

                if score < 0:
                    continue

                all_slots.append({
                    "date":          d.strftime("%Y-%m-%d"),
                    "weekday":       vara_name,
                    "period":        period_type,
                    "choghadiya":    name,
                    "start":         slot.get("start", ""),
                    "end":           slot.get("end", ""),
                    "score":         round(score, 1),
                    "quality":       "Excellent" if score >= 7 else "Good" if score >= 4 else "Acceptable",
                    "nakshatra":     nak_name,
                    "yoga":          yoga_name,
                    "avoid_rahukaal": f"{rk_start}–{rk_end}" if rk_start else "",
                })

    all_slots.sort(key=lambda x: x["score"], reverse=True)
    return all_slots[:20]


def get_muhurta(event: str, date_str: str, lat: float = 28.6139, lon: float = 77.209, tz: float = 5.5) -> dict:
    """Return auspicious time slots for a given event on a date."""
    try:
        parts = date_str.split("-")
        year, month, day = int(parts[0]), int(parts[1]), int(parts[2])
    except Exception:
        today = _date.today()
        year, month, day = today.year, today.month, today.day

    if not PANCHANG_AVAILABLE:
        return {"event": event, "date": date_str, "slots": [], "note": "Panchang unavailable"}

    p = get_panchang(year, month, day, lat, lon, tz)
    slots = []

    # Abhijit is always auspicious
    slots.append({
        "name": "Abhijit Muhurta",
        "start": p["abhijit_muhurta"]["start"],
        "end": p["abhijit_muhurta"]["end"],
        "quality": "Excellent",
        "note": "Best universal muhurta (midday)",
    })

    # If today's yoga is auspicious, add full day note
    yoga_name = p["yoga"]["name"]
    if p["yoga"]["is_auspicious"] and yoga_name not in _INAUSPICIOUS_YOGAS:
        slots.append({
            "name": f"Yoga: {yoga_name}",
            "start": p["sunrise"],
            "end": p["sunset"],
            "quality": "Good",
            "note": f"Entire day blessed by {yoga_name} yoga",
        })

    # Avoid Rahukaal
    return {
        "event": event,
        "date": date_str,
        "slots": slots,
        "avoid": [{"name": "Rahukaal", "start": p["rahukaal"]["start"], "end": p["rahukaal"]["end"]}],
        "panchang_summary": {
            "tithi": p["tithi"]["name"],
            "vara": p["vara"]["name"],
            "nakshatra": p["nakshatra"]["name"],
            "yoga": p["yoga"]["name"],
        },
    }
