import json
from fastapi import APIRouter
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
