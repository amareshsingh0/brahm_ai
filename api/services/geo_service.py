"""
Geocoding service — city name → (lat, lon, tz_offset)
Strategy:
  1. Check local cities.json (730 Indian cities, instant)
  2. Geopy Nominatim (OpenStreetMap, free, no API key, worldwide)
  3. Fallback to Delhi coords if all else fails
"""
import json
import os
import re
from functools import lru_cache
from typing import Tuple, Optional

_DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "data")
_CITIES_PATH = os.path.join(_DATA_DIR, "cities.json")

# Cache geocoded results in memory for the session
_geocode_cache: dict = {}


@lru_cache(maxsize=1)
def _load_local_cities() -> dict:
    """Load cities.json into a lowercase name → dict lookup."""
    try:
        with open(_CITIES_PATH) as f:
            data = json.load(f)
        cities = data.get("cities", data) if isinstance(data, dict) else data
        return {c["name"].lower(): c for c in cities if "name" in c}
    except Exception:
        return {}


def get_coords(place: str) -> Tuple[float, float, float]:
    """
    Resolve city/place name to (lat, lon, tz_offset_hours).
    Returns (28.6139, 77.2090, 5.5) for Delhi as ultimate fallback.
    """
    if not place:
        return (28.6139, 77.2090, 5.5)

    place = place.strip()
    key = place.lower()

    # 1. Memory cache
    if key in _geocode_cache:
        return _geocode_cache[key]

    # 2. Local cities.json
    local = _load_local_cities()
    if key in local:
        c = local[key]
        result = (float(c["lat"]), float(c["lon"]), float(c.get("tz", 5.5)))
        _geocode_cache[key] = result
        return result

    # Try partial match in local
    for name, c in local.items():
        if key in name or name in key:
            result = (float(c["lat"]), float(c["lon"]), float(c.get("tz", 5.5)))
            _geocode_cache[key] = result
            return result

    # 3. Geopy Nominatim (OpenStreetMap — free, no API key)
    try:
        from geopy.geocoders import Nominatim
        from timezonefinder import TimezoneFinder
        import pytz

        geolocator = Nominatim(user_agent="brahm-ai-astro/1.0", timeout=5)
        location = geolocator.geocode(place, language="en")
        if location:
            lat = location.latitude
            lon = location.longitude
            # Get timezone from coordinates
            tf = TimezoneFinder()
            tz_name = tf.timezone_at(lat=lat, lng=lon)
            if tz_name:
                import datetime
                tz = pytz.timezone(tz_name)
                offset = tz.utcoffset(datetime.datetime.now()).total_seconds() / 3600
            else:
                offset = 5.5  # IST fallback
            result = (lat, lon, round(offset, 2))
            _geocode_cache[key] = result
            print(f"[geo] Geocoded '{place}' → {lat:.4f}, {lon:.4f}, tz={offset:.2f}")
            return result
    except Exception as e:
        print(f"[geo] Geocode failed for '{place}': {e}")

    # 4. Ultimate fallback: Delhi
    result = (28.6139, 77.2090, 5.5)
    _geocode_cache[key] = result
    return result
