from typing import Dict, List, Optional, Any
from pydantic import BaseModel


class GrahaData(BaseModel):
    rashi: str
    house: int
    degree: float
    nakshatra: str
    nakshatra_lord: Optional[str] = None
    pada: int
    retro: bool
    status: str
    house_lord: Optional[str] = None


class NavamshaGraha(BaseModel):
    rashi: str
    house: int
    status: str
    retro: bool


class LagnaData(BaseModel):
    rashi: str
    nakshatra: str
    pada: Optional[int] = None
    degree: float


class HouseData(BaseModel):
    house: int
    rashi: str
    lord: str


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
    navamsha: Optional[Dict[str, NavamshaGraha]] = None
    navamsha_lagna: Optional[Dict[str, str]] = None
    navamsha_houses: Optional[List[HouseData]] = None
    dashas: List[DashaData]
    yogas: List[YogaData]
