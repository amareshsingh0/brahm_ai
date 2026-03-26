from typing import Optional
from pydantic import BaseModel


class UserProfile(BaseModel):
    session_id: str = ""
    name: str = ""
    date: str = ""        # YYYY-MM-DD (birth_date)
    time: str = ""        # HH:MM     (birth_time)
    lat: float = 0.0
    lon: float = 0.0
    tz: float = 5.5
    place: str = ""
    gender: str = ""      # Male | Female | Other | Prefer not to say
    rashi: str = ""
    nakshatra: str = ""
    language: str = "english"
    plan: str = "free"           # free | standard | premium
    phone: Optional[str] = None
    email: Optional[str] = None
