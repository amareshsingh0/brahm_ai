from typing import List, Dict, Any, Optional
from pydantic import BaseModel


class ChatMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str


class ChatRequest(BaseModel):
    message: str
    history: List[ChatMessage] = []
    language: str = "hi"              # "hi" | "en" | "sa" | "all"
    page_context: str = "general"     # "kundali" | "panchang" | "compatibility" |
                                      # "sky" | "horoscope" | "palmistry" | "general"
    page_data: Dict[str, Any] = {}    # current page's rendered data
    include_user_chart: bool = False  # load user's kundali from DB
    user_id: Optional[str] = None    # logged-in user ID for chat persistence
    session_id: Optional[str] = None # conversation session UUID (groups messages)


class Source(BaseModel):
    book: str
    source: str
    language: str


class ChatResponse(BaseModel):
    answer: str
    sources: List[Source] = []
