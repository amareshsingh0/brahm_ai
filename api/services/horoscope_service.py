"""
Horoscope service — Phase 1: static JSON fallback per rashi.
Phase 2: replace with LLM-generated predictions.
"""
import json
from api.config import HOROSCOPES_PATH

_cache: dict = {}


def _load():
    global _cache
    if not _cache:
        with open(HOROSCOPES_PATH) as f:
            _cache = json.load(f)
    return _cache


def get_horoscope(rashi: str, period: str = "daily") -> dict:
    data = _load()
    entry = data.get(rashi)
    if not entry:
        raise ValueError(f"Unknown rashi: {rashi}")
    return {
        "rashi": rashi,
        "period": period,
        "prediction": entry["prediction"],
        "lucky_number": entry["lucky_number"],
        "lucky_color": entry["lucky_color"],
    }
