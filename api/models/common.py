from pydantic import BaseModel, Field


class BirthDetails(BaseModel):
    name: str = ""
    date: str = Field(..., description="YYYY-MM-DD")
    time: str = Field(..., description="HH:MM")
    lat: float
    lon: float
    tz: float = 5.5
    place: str = ""


class Coordinates(BaseModel):
    lat: float
    lon: float
    tz: float = 5.5
