from typing import Dict, List, Optional, Any
from pydantic import BaseModel


class GrahaData(BaseModel):
    rashi: str
    rashi_en: Optional[str] = None
    house: int
    degree: float
    longitude: Optional[float] = None
    nakshatra: str
    nakshatra_hindi: Optional[str] = None
    nakshatra_lord: Optional[str] = None
    pada: int
    retro: bool
    status: str
    relationship: Optional[str] = None
    house_lord: Optional[str] = None
    graha_en: Optional[str] = None
    graha_hindi: Optional[str] = None
    karaka: Optional[str] = None
    speed: Optional[float] = None


class NavamshaGraha(BaseModel):
    rashi: str
    house: int
    status: str
    retro: bool


class LagnaData(BaseModel):
    rashi: str
    rashi_en: Optional[str] = None
    nakshatra: str
    nakshatra_hindi: Optional[str] = None
    pada: Optional[int] = None
    degree: float
    full_degree: Optional[float] = None


class HouseData(BaseModel):
    house: int
    rashi: str
    rashi_en: Optional[str] = None
    lord: str
    lord_en: Optional[str] = None
    signification: Optional[str] = None
    planets: Optional[List[str]] = None


class DashaData(BaseModel):
    lord: str
    years: float
    start: str
    end: str
    antardashas: Optional[List[Dict[str, Any]]] = None


class YogaData(BaseModel):
    name: str
    desc: str
    strength: str
    category: Optional[str] = None


class VargaGraha(BaseModel):
    rashi: str
    house: int
    status: str
    retro: bool


class VargaChartData(BaseModel):
    division: int
    name: str
    full_name: str
    signification: str
    lagna: Dict[str, str]
    houses: List[Dict[str, Any]]
    grahas: Dict[str, VargaGraha]


class KundaliRequest(BaseModel):
    name: str = ""
    date: str   # YYYY-MM-DD
    time: str   # HH:MM
    lat: float
    lon: float
    tz: float = 5.5
    place: str = ""
    ayanamsha: str = "lahiri"
    rahu_mode: str = "mean"
    calc_options: List[str] = []


class KundaliResponse(BaseModel):
    name: str
    place: str
    birth_date: str
    lat: float
    lon: float
    tz: float
    ayanamsha: float
    ayanamsha_mode: Optional[str] = "lahiri"
    ayanamsha_label: Optional[str] = "Lahiri/Chitra Paksha"
    rahu_mode: Optional[str] = "mean"
    lagna: LagnaData
    grahas: Dict[str, GrahaData]
    houses: List[HouseData]
    navamsha: Optional[Dict[str, NavamshaGraha]] = None
    navamsha_lagna: Optional[Dict[str, str]] = None
    navamsha_houses: Optional[List[HouseData]] = None
    dashas: List[DashaData]
    yogas: List[YogaData]
    varga_charts: Optional[Dict[str, VargaChartData]] = None
