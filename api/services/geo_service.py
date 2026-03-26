"""
Geocoding service — city name → (lat, lon, tz_offset)
Strategy:
  1. Check local SQLite cities.db (200K+ worldwide cities, instant)
  2. Check legacy cities.json (730 Indian cities, for backward compat)
  3. Geopy Nominatim (OpenStreetMap, free, no API key, worldwide)
  4. Fallback to Delhi coords if all else fails
"""
import json
import os
import sqlite3
import re
from functools import lru_cache
from typing import Tuple, Optional, List

_DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "data")
_CITIES_DB = os.path.join(_DATA_DIR, "cities.db")
_CITIES_JSON = os.path.join(_DATA_DIR, "cities.json")

_geocode_cache: dict = {}

DELHI = (28.6139, 77.2090, 5.5)


def _tz_offset(tz_name: str) -> float:
    """Convert IANA timezone name to UTC offset hours."""
    try:
        import pytz, datetime
        tz = pytz.timezone(tz_name)
        return round(tz.utcoffset(datetime.datetime.now()).total_seconds() / 3600, 2)
    except Exception:
        return 5.5


def _db_available() -> bool:
    return os.path.exists(_CITIES_DB)


def _search_db(query: str, limit: int = 10) -> List[dict]:
    """Search cities.db by name prefix. Returns list of city dicts."""
    if not _db_available():
        return []
    try:
        conn = sqlite3.connect(_CITIES_DB)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        q = query.strip().lower()
        # Exact match first, then prefix, then contains — ordered by population
        cur.execute("""
            SELECT name, ascii_name, lat, lon, country_code, country_name, timezone, population
            FROM cities
            WHERE ascii_name LIKE ? OR name LIKE ?
            ORDER BY
                CASE WHEN lower(ascii_name) = ? THEN 0
                     WHEN lower(name) = ? THEN 0
                     WHEN lower(ascii_name) LIKE ? THEN 1
                     ELSE 2 END,
                population DESC
            LIMIT ?
        """, (f"{q}%", f"{q}%", q, q, f"{q}%", limit))
        rows = [dict(r) for r in cur.fetchall()]
        conn.close()
        return rows
    except Exception as e:
        print(f"[geo] DB search error: {e}")
        return []


def _get_coords_db(place: str) -> Optional[Tuple[float, float, float]]:
    """Exact/best match from SQLite."""
    rows = _search_db(place, limit=5)
    if not rows:
        return None
    # Prefer exact match
    place_lower = place.strip().lower()
    for row in rows:
        if row["ascii_name"].lower() == place_lower or row["name"].lower() == place_lower:
            offset = _tz_offset(row["timezone"]) if row["timezone"] else 5.5
            return (row["lat"], row["lon"], offset)
    # Return best (highest population)
    row = rows[0]
    offset = _tz_offset(row["timezone"]) if row["timezone"] else 5.5
    return (row["lat"], row["lon"], offset)


@lru_cache(maxsize=1)
def _load_local_cities() -> dict:
    """Load legacy cities.json into lowercase name → dict lookup."""
    try:
        with open(_CITIES_JSON) as f:
            data = json.load(f)
        cities = data.get("cities", data) if isinstance(data, dict) else data
        return {c["name"].lower(): c for c in cities if "name" in c}
    except Exception:
        return {}


def get_coords(place: str) -> Tuple[float, float, float]:
    """
    Resolve city/place name to (lat, lon, tz_offset_hours).
    Returns Delhi coords as ultimate fallback.
    """
    if not place:
        return DELHI

    place = place.strip()
    key = place.lower()

    # 1. Memory cache
    if key in _geocode_cache:
        return _geocode_cache[key]

    # 2. SQLite cities.db (200K+ worldwide)
    result = _get_coords_db(place)
    if result:
        _geocode_cache[key] = result
        return result

    # 3. Legacy cities.json (730 Indian cities)
    local = _load_local_cities()
    if key in local:
        c = local[key]
        result = (float(c["lat"]), float(c["lon"]), float(c.get("tz", 5.5)))
        _geocode_cache[key] = result
        return result
    for name, c in local.items():
        if key in name or name in key:
            result = (float(c["lat"]), float(c["lon"]), float(c.get("tz", 5.5)))
            _geocode_cache[key] = result
            return result

    # 4. Geopy Nominatim (OpenStreetMap — free, worldwide)
    try:
        from geopy.geocoders import Nominatim
        from timezonefinder import TimezoneFinder

        geolocator = Nominatim(user_agent="brahm-ai-astro/1.0", timeout=5)
        location = geolocator.geocode(place, language="en")
        if location:
            lat = location.latitude
            lon = location.longitude
            tf = TimezoneFinder()
            tz_name = tf.timezone_at(lat=lat, lng=lon)
            offset = _tz_offset(tz_name) if tz_name else 5.5
            result = (lat, lon, round(offset, 2))
            _geocode_cache[key] = result
            print(f"[geo] Nominatim '{place}' → {lat:.4f}, {lon:.4f}, tz={offset:.2f}")
            return result
    except Exception as e:
        print(f"[geo] Nominatim failed for '{place}': {e}")

    # 5. Ultimate fallback: Delhi
    _geocode_cache[key] = DELHI
    return DELHI


def search_cities(query: str, limit: int = 20) -> List[dict]:
    """
    Search cities by name prefix — for autocomplete API.
    Returns list of {name, country, lat, lon, tz}.
    """
    if not query or len(query) < 2:
        return []

    results = []

    # From SQLite
    rows = _search_db(query, limit=limit)
    for row in rows:
        offset = _tz_offset(row["timezone"]) if row["timezone"] else 5.5
        label = f"{row['name']}, {row['country_name']}" if row.get("country_name") else row["name"]
        results.append({
            "name": row["name"],
            "label": label,
            "country": row.get("country_name", ""),
            "country_code": row.get("country_code", ""),
            "lat": row["lat"],
            "lon": row["lon"],
            "tz": offset,
            "population": row.get("population", 0),
        })

    if results:
        return results

    # Fallback: legacy cities.json
    local = _load_local_cities()
    q = query.lower()
    for name, c in local.items():
        if name.startswith(q):
            results.append({
                "name": c["name"],
                "label": c["name"],
                "country": "India",
                "country_code": "IN",
                "lat": float(c["lat"]),
                "lon": float(c["lon"]),
                "tz": float(c.get("tz", 5.5)),
                "population": 0,
            })
            if len(results) >= limit:
                break

    return results
