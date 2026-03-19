from typing import Optional
from pydantic import BaseModel


class UserProfile(BaseModel):
    session_id: str
    name: str = ""
    date: str = ""        # YYYY-MM-DD
    time: str = ""        # HH:MM
    lat: float = 0.0
    lon: float = 0.0
    tz: float = 5.5
    place: str = ""
    rashi: str = ""
    nakshatra: str = ""
    language: str = "english"
