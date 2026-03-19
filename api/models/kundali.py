from typing import Dict, List, Optional
from pydantic import BaseModel


class GrahaData(BaseModel):
    rashi: str
    house: int
    degree: float
    nakshatra: str
    pada: int
    retro: bool
    status: str


class LagnaData(BaseModel):
    rashi: str
    nakshatra: str
    degree: float


class HouseData(BaseModel):
    house: int
    rashi: str
    lord: str
    degree: float


class DashaData(BaseModel):
    lord: str
    years: float
    start: str
    end: str


class YogaData(BaseModel):
    name: str
    desc: str
    strength: str


class KundaliRequest(BaseModel):
    name: str = ""
    date: str   # YYYY-MM-DD
    time: str   # HH:MM
    lat: float
    lon: float
    tz: float = 5.5
    place: str = ""


class KundaliResponse(BaseModel):
    name: str
    place: str
    birth_date: str
    lat: float
    lon: float
    tz: float
    ayanamsha: float
    lagna: LagnaData
    grahas: Dict[str, GrahaData]
    houses: List[HouseData]
    dashas: List[DashaData]
    yogas: List[YogaData]
