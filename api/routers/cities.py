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


@router.get("/geocode")
def geocode(q: str = Query(..., min_length=2)):
    """
    Resolve any city/place name worldwide → {name, lat, lon, tz}.
    Uses geo_service: cities.json first (730 Indian cities), then
    Nominatim/OpenStreetMap (worldwide, free), then timezonefinder.
    """
    from api.services.geo_service import get_coords
    lat, lon, tz = get_coords(q)
    return {"name": q, "lat": lat, "lon": lon, "tz": tz}
