from typing import Optional, List
from pydantic import BaseModel


class VaraData(BaseModel):
    name: str
    hindi: str
    lord: str


class TithiData(BaseModel):
    name: str
    hindi: str
    paksha: str
    end_time: str
    tithi_type: str = "normal"  # "normal" | "vridhi" | "ksheya"


class NakshatraData(BaseModel):
    name: str
    hindi: str
    pada: int
    lord: str
    end_time: str = ""


class YogaData(BaseModel):
    name: str
    hindi: str
    is_auspicious: bool
    end_time: str = ""


class KaranaData(BaseModel):
    name: str
    hindi: str
    is_bhadra: Optional[bool] = False


class MuhurtaSlot(BaseModel):
    start: str
    end: str


class NishitaSlot(BaseModel):
    start: str
    end: str
    midpoint: str = ""


class ChoghadiyaPeriod(BaseModel):
    name: str
    hindi: str
    quality: str
    auspicious: bool
    start: str
    end: str


class ChoghadiyaData(BaseModel):
    day: List[ChoghadiyaPeriod]
    night: List[ChoghadiyaPeriod]


class PanchangRequest(BaseModel):
    date: str        # YYYY-MM-DD
    lat: float
    lon: float
    tz: float = 5.5


class PanchangResponse(BaseModel):
    vara: VaraData
    tithi: TithiData
    nakshatra: NakshatraData
    yoga: YogaData
    karana: KaranaData
    sunrise: str
    sunset: str
    abhijit_muhurta: MuhurtaSlot
    rahukaal: MuhurtaSlot
    yamagandam: Optional[MuhurtaSlot] = None
    gulika_kaal: Optional[MuhurtaSlot] = None
    brahma_muhurta: Optional[MuhurtaSlot] = None
    pradosh_kaal: Optional[MuhurtaSlot] = None
    nishita_kaal: Optional[NishitaSlot] = None
    choghadiya: Optional[ChoghadiyaData] = None
    panchaka: Optional[bool] = None
