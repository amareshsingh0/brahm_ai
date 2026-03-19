"""
Tool Executor — runs Python calculation services based on Pass 1 decision.
This is the bridge: Gemini decides WHAT to calculate, Python services DO the calculation.
"""
import asyncio
from typing import Optional


def compress_kundali(kundali: dict) -> dict:
    """
    Compress full kundali to only what Gemini needs for reasoning.
    Never send raw kundali — too noisy, wastes context.
    """
    if not kundali:
        return {}

    planets = kundali.get("planets", {})
    dashas = kundali.get("dashas", [])
    lagna = kundali.get("lagna", {})
    yogas = kundali.get("yogas", [])

    current_dasha = next((d for d in dashas if d.get("is_current")), {})
    next_dasha = None
    if current_dasha:
        idx = next((i for i, d in enumerate(dashas) if d.get("is_current")), -1)
        if idx >= 0 and idx + 1 < len(dashas):
            next_dasha = dashas[idx + 1]

    planet_summary = {}
    for name, p in planets.items():
        parts = [p.get("rashi", "?"), f"{p.get('house', '?')}H"]
        if p.get("retro"):
            parts.append("vakri")
        if p.get("combust"):
            parts.append("ast")
        planet_summary[name] = " ".join(parts)

    yoga_names = [y.get("name", "") for y in yogas[:6] if y.get("name")]

    return {
        "lagna": lagna.get("rashi", "?"),
        "lagna_degree": lagna.get("degree", "?"),
        "moon_rashi": planets.get("Chandra", {}).get("rashi", "?"),
        "moon_house": planets.get("Chandra", {}).get("house", "?"),
        "sun_rashi": planets.get("Surya", {}).get("rashi", "?"),
        "current_dasha": f"{current_dasha.get('planet','?')} ({current_dasha.get('start','?')} – {current_dasha.get('end','?')})",
        "next_dasha": f"{next_dasha.get('planet','?')} from {next_dasha.get('start','?')}" if next_dasha else "N/A",
        "planets": planet_summary,
        "yogas": yoga_names,
    }


async def execute_tools(
    decision: dict,
    user_birth_data: Optional[dict],
    page_data: dict,
) -> dict:
    """
    Run calculation services based on Pass 1 decision.
    Returns dict of results keyed by service name.
    """
    results = {}
    services = decision.get("calc_services", [])

    if not services or not user_birth_data:
        return results

    loop = asyncio.get_event_loop()

    if "kundali" in services and user_birth_data:
        try:
            from api.services.kundali_service import calc_kundali
            kundali = await loop.run_in_executor(
                None, lambda: calc_kundali(
                    date=user_birth_data.get("birth_date", ""),
                    time=user_birth_data.get("birth_time", ""),
                    lat=user_birth_data.get("birth_lat", 28.6),
                    lon=user_birth_data.get("birth_lon", 77.2),
                    tz=user_birth_data.get("birth_tz", 5.5),
                    name=user_birth_data.get("name", ""),
                )
            )
            results["kundali"] = compress_kundali(kundali)
        except Exception as e:
            results["kundali_error"] = str(e)

    if "dasha" in services and user_birth_data:
        try:
            from api.services.kundali_service import calc_kundali
            kundali = await loop.run_in_executor(
                None, lambda: calc_kundali(
                    date=user_birth_data.get("birth_date", ""),
                    time=user_birth_data.get("birth_time", ""),
                    lat=user_birth_data.get("birth_lat", 28.6),
                    lon=user_birth_data.get("birth_lon", 77.2),
                    tz=user_birth_data.get("birth_tz", 5.5),
                    name=user_birth_data.get("name", ""),
                )
            )
            dashas = kundali.get("dashas", [])
            current = next((d for d in dashas if d.get("is_current")), {})
            upcoming = [d for d in dashas if not d.get("is_current")][:3]
            results["dasha"] = {
                "current": current,
                "upcoming": upcoming,
            }
        except Exception as e:
            results["dasha_error"] = str(e)

    if "panchang" in services:
        try:
            from api.services.panchang_service import Panchang
            import datetime
            today = datetime.date.today().isoformat()
            lat = user_birth_data.get("lat", 28.6139) if user_birth_data else 28.6139
            lon = user_birth_data.get("lon", 77.2090) if user_birth_data else 77.2090
            tz = user_birth_data.get("tz", 5.5) if user_birth_data else 5.5
            p = await loop.run_in_executor(
                None, lambda: Panchang(date=today, lat=lat, lon=lon, tz=tz).compute()
            )
            results["panchang"] = p
        except Exception as e:
            results["panchang_error"] = str(e)

    if "muhurta" in services:
        try:
            from api.services.muhurta_service import get_muhurta
            params = decision.get("calc_params", {})
            event = params.get("event", "general")
            lat = user_birth_data.get("lat", 28.6) if user_birth_data else 28.6
            lon = user_birth_data.get("lon", 77.2) if user_birth_data else 77.2
            tz = user_birth_data.get("tz", 5.5) if user_birth_data else 5.5
            result = await loop.run_in_executor(
                None, lambda: get_muhurta(event=event, lat=lat, lon=lon, tz=tz)
            )
            results["muhurta"] = result
        except Exception as e:
            results["muhurta_error"] = str(e)

    if "compatibility" in services:
        # Already in page_data — no re-calculation needed
        if page_data:
            results["compatibility"] = {
                k: v for k, v in page_data.items()
                if k in ["total_score", "out_of", "verdict", "nadi_dosha",
                         "gana_mismatch", "weak_areas", "strong_areas", "kutas"]
            }

    return results
