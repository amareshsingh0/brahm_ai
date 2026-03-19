"""
Muhurta service — wraps panchang to find auspicious slots for an event.
"""
from datetime import date as _date
from api.services.panchang_service import get_panchang, PANCHANG_AVAILABLE

# Inauspicious yogas to avoid
_INAUSPICIOUS_YOGAS = {"Vishkumbha", "Atiganda", "Shoola", "Ganda", "Vyaghata",
                        "Vajra", "Vyatipata", "Variyan", "Parigha", "Vaidhriti"}


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
