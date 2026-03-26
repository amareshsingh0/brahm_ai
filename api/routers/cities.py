import json
from fastapi import APIRouter, Query
from api.config import CITIES_PATH

router = APIRouter()
_cache: list = []


def _load():
    global _cache
    if not _cache:
        with open(CITIES_PATH) as f:
            _cache = json.load(f)["cities"]
    return _cache


@router.get("/cities")
def get_cities():
    return {"cities": _load()}


@router.get("/cities/search")
def search_cities(q: str = Query(..., min_length=2), limit: int = Query(20, le=50)):
    """
    Autocomplete search — worldwide cities from GeoNames (200K+).
    Returns [{name, label, country, lat, lon, tz}, ...]
    """
    from api.services.geo_service import search_cities as _search
    return {"results": _search(q, limit=limit)}


@router.get("/geocode")
def geocode(q: str = Query(..., min_length=2)):
    """
    Resolve any city/place name worldwide → {name, lat, lon, tz}.
    Uses geo_service: cities.db (200K+ worldwide) → cities.json → Nominatim.
    """
    from api.services.geo_service import get_coords
    lat, lon, tz = get_coords(q)
    return {"name": q, "lat": lat, "lon": lon, "tz": tz}
