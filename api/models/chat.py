from typing import List, Optional
from pydantic import BaseModel


class ChatMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str


class ChatRequest(BaseModel):
    message: str
    history: List[ChatMessage] = []
    language: str = "all"  # "all" | "sanskrit" | "hindi" | "english"


class Source(BaseModel):
    book: str
    source: str
    language: str


class ChatResponse(BaseModel):
    answer: str
    sources: List[Source] = []
