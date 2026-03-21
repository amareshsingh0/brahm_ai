"""
Tool Executor — runs Python calculation services based on Pass 1 decision.
This is the bridge: Gemini decides WHAT to calculate, Python services DO the calculation.
"""
import asyncio
from datetime import date
from typing import Optional


def compress_kundali(kundali: dict) -> dict:
    """
    Compress full kundali to only what Gemini needs for reasoning.
    Never send raw kundali — too noisy, wastes context.

    Note: kundali_service returns keys: grahas, lagna, houses, dashas, yogas
    """
    if not kundali:
        return {}

    grahas = kundali.get("grahas", {})   # correct key (not "planets")
    dashas = kundali.get("dashas", [])
    lagna  = kundali.get("lagna", {})
    yogas  = kundali.get("yogas", [])

    # Find current dasha by date range
    today = date.today().isoformat()
    current_dasha = next(
        (d for d in dashas if d.get("start", "") <= today <= d.get("end", "")),
        dashas[0] if dashas else {},
    )
    idx = next((i for i, d in enumerate(dashas) if d == current_dasha), -1)
    next_dasha = dashas[idx + 1] if idx >= 0 and idx + 1 < len(dashas) else None

    # Planet summary: "Rashi House [vakri] [ast]"
    planet_summary = {}
    for name, p in grahas.items():
        parts = [p.get("rashi", "?"), f"{p.get('house', '?')}H"]
        if p.get("retro"):
            parts.append("vakri")
        if p.get("combust"):
            parts.append("ast")
        planet_summary[name] = " ".join(parts)

    yoga_names = [y.get("name", "") for y in yogas[:6] if y.get("name")]

    # Navamsha (D-9) — Marriage, Dharma, Soul potential
    navamsha_raw = kundali.get("navamsha", {})
    nav_lagna    = kundali.get("navamsha_lagna", {}).get("rashi", "?")
    _NAV_KEY     = ["Surya", "Chandra", "Mangal", "Shukra", "Guru", "Shani", "Budh", "Rahu", "Ketu"]
    nav_summary  = {}
    for name in _NAV_KEY:
        nd = navamsha_raw.get(name)
        if nd:
            parts = [nd.get("rashi", "?"), f"{nd.get('house','?')}H"]
            if nd.get("status") and nd["status"] not in ("Normal", ""):
                parts.append(nd["status"])
            nav_summary[name] = " ".join(parts)

    return {
        "lagna":         lagna.get("rashi", "?"),
        "lagna_degree":  lagna.get("degree", "?"),
        "moon_rashi":    grahas.get("Chandra", {}).get("rashi", "?"),
        "moon_house":    grahas.get("Chandra", {}).get("house", "?"),
        "sun_rashi":     grahas.get("Surya", {}).get("rashi", "?"),
        "current_dasha": (
            f"{current_dasha.get('lord','?')} "
            f"({current_dasha.get('start','?')} – {current_dasha.get('end','?')})"
        ),
        "next_dasha": (
            f"{next_dasha.get('lord','?')} from {next_dasha.get('start','?')}"
            if next_dasha else "N/A"
        ),
        "planets":       planet_summary,
        "yogas":         yoga_names,
        "navamsha_lagna": nav_lagna,
        "navamsha":      nav_summary,
    }


def _parse_birth_data(user_birth_data: dict):
    """Parse birth data dict → (year, month, day, hour, minute, lat, lon, tz, name)"""
    raw_date = user_birth_data.get("birth_date", "")
    raw_time = user_birth_data.get("birth_time", "")
    try:
        parts = raw_date.split("-")
        year, month, day = int(parts[0]), int(parts[1]), int(parts[2])
    except Exception:
        return None

    try:
        tp = raw_time.split(":")
        hour, minute = int(tp[0]), int(tp[1])
    except Exception:
        hour, minute = 12, 0

    lat  = float(user_birth_data.get("birth_lat", 28.6139))
    lon  = float(user_birth_data.get("birth_lon", 77.2090))
    tz   = float(user_birth_data.get("birth_tz",  5.5))
    name = user_birth_data.get("name", "")
    return year, month, day, hour, minute, lat, lon, tz, name


async def execute_tools(
    decision: dict,
    user_birth_data: Optional[dict],
    page_data: dict,
) -> dict:
    """
    Run calculation services based on Pass 1 decision.
    Returns dict of results keyed by service name.
    """
    results  = {}
    services = decision.get("calc_services", [])
    qtype    = decision.get("query_type", "")

    # Auto-add gochar for prediction/chart queries even if not in calc_services
    if qtype in ("CHART_ANALYSIS", "RECOMMENDATION") and "gochar" not in services:
        services = list(services) + ["gochar"]

    if not services or not user_birth_data:
        return results

    loop = asyncio.get_event_loop()

    # ── Helper: calc kundali once and reuse ─────────────────────────────────
    _kundali_cache = {}

    async def _get_kundali():
        if _kundali_cache:
            return _kundali_cache.get("data")
        parsed = _parse_birth_data(user_birth_data)
        if not parsed:
            return None
        year, month, day, hour, minute, lat, lon, tz, name = parsed
        from api.services.kundali_service import calc_kundali
        result, err = await loop.run_in_executor(
            None, lambda: calc_kundali(year, month, day, hour, minute, lat, lon, tz, name)
        )
        _kundali_cache["data"] = result
        return result

    # ── Kundali ─────────────────────────────────────────────────────────────
    if "kundali" in services:
        try:
            raw = await _get_kundali()
            if raw:
                results["kundali"] = compress_kundali(raw)
        except Exception as e:
            results["kundali_error"] = str(e)

    # ── Dasha ────────────────────────────────────────────────────────────────
    if "dasha" in services:
        try:
            raw = await _get_kundali()
            if raw:
                dashas  = raw.get("dashas", [])
                today   = date.today().isoformat()
                current = next(
                    (d for d in dashas if d.get("start","") <= today <= d.get("end","")),
                    dashas[0] if dashas else {},
                )
                upcoming = [d for d in dashas if d != current][:4]
                results["dasha"] = {"current": current, "upcoming": upcoming}
        except Exception as e:
            results["dasha_error"] = str(e)

    # ── Gochar (current transits) ─────────────────────────────────────────────
    if "gochar" in services:
        try:
            raw = await _get_kundali()
            if raw:
                lagna_rashi = raw.get("lagna", {}).get("rashi", "Mesh")
                moon_rashi  = raw.get("grahas", {}).get("Chandra", {}).get("rashi", "Mesh")
                from api.services.gochar_service import get_transits
                results["gochar"] = await loop.run_in_executor(
                    None, lambda: get_transits(lagna_rashi, moon_rashi)
                )
        except Exception as e:
            results["gochar_error"] = str(e)

    # ── Panchang ─────────────────────────────────────────────────────────────
    if "panchang" in services:
        try:
            from api.services.panchang_service import Panchang
            import datetime as dt_mod
            today_str = dt_mod.date.today().isoformat()
            lat = float(user_birth_data.get("birth_lat", 28.6139))
            lon = float(user_birth_data.get("birth_lon", 77.2090))
            tz  = float(user_birth_data.get("birth_tz",  5.5))
            p = await loop.run_in_executor(
                None, lambda: Panchang(date=today_str, lat=lat, lon=lon, tz=tz).compute()
            )
            results["panchang"] = p
        except Exception as e:
            results["panchang_error"] = str(e)

    # ── Muhurta ──────────────────────────────────────────────────────────────
    if "muhurta" in services:
        try:
            from api.services.muhurta_service import get_muhurta
            params = decision.get("calc_params", {})
            event  = params.get("event", "general")
            lat = float(user_birth_data.get("birth_lat", 28.6)) if user_birth_data else 28.6
            lon = float(user_birth_data.get("birth_lon", 77.2)) if user_birth_data else 77.2
            tz  = float(user_birth_data.get("birth_tz",  5.5))  if user_birth_data else 5.5
            result = await loop.run_in_executor(
                None, lambda: get_muhurta(event=event, lat=lat, lon=lon, tz=tz)
            )
            results["muhurta"] = result
        except Exception as e:
            results["muhurta_error"] = str(e)

    # ── Compatibility (already in page_data) ─────────────────────────────────
    if "compatibility" in services and page_data:
        results["compatibility"] = {
            k: v for k, v in page_data.items()
            if k in ["total_score", "out_of", "verdict", "nadi_dosha",
                     "gana_mismatch", "weak_areas", "strong_areas", "kutas"]
        }

    return results
