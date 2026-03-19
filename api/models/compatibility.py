from typing import List, Optional
from pydantic import BaseModel
from .common import BirthDetails


class GunaScore(BaseModel):
    name: str
    score: int
    max: int
    desc: str
    interpretation: str = ""   # couple-specific text
    alt_score: Optional[int] = None


class MangalDosha(BaseModel):
    person_a: bool
    person_b: bool


class DoshaSummary(BaseModel):
    name: str
    present: bool
    severity: str           # "High" | "Medium" | "Low" | "None"
    cancellation: Optional[str] = None
    note: str


class RajjuDosha(BaseModel):
    present: bool
    rajju_a: str
    rajju_b: str
    severity: str           # "High" | "Medium" | "Low" | "None"
    note: str


class VedhaDosha(BaseModel):
    present: bool
    note: str


class LifeAreaScore(BaseModel):
    area: str
    icon: str
    score: int              # 0–100
    label: str              # "Excellent" | "Good" | "Average" | "Needs Work"


class CompatibilityRequest(BaseModel):
    person_a: BirthDetails
    person_b: BirthDetails
    varna_system: str = "both"   # "nakshatra" | "rashi" | "both"


class CompatibilityResponse(BaseModel):
    total_score: int
    max_score: int = 36
    percentage: int
    verdict: str
    verdict_detail: str
    gunas: List[GunaScore]
    mangal_dosha: MangalDosha
    nakshatra_a: str
    nakshatra_b: str
    rashi_a: str
    rashi_b: str
    gana_a: str
    gana_b: str
    nadi_a: str
    nadi_b: str
    varna_a: str
    varna_b: str
    yoni_a: str
    yoni_b: str
    rajju_dosha: RajjuDosha
    vedha_dosha: VedhaDosha
    life_areas: List[LifeAreaScore]
    strengths: List[str]
    challenges: List[str]
    dosha_summary: List[DoshaSummary]
