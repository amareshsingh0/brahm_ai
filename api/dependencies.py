"""
Shared state dictionary G{} — loaded once at startup (lazy for RAG).
Use get_rag_state() as a FastAPI Depends() in chat/search routers.
"""
from fastapi import HTTPException

# Single shared state dict (analogous to global G in gradio script)
G: dict = {}


def get_rag_state() -> dict:
    """Dependency: ensure RAG components are loaded before serving chat/search."""
    if not G.get("loaded"):
        raise HTTPException(
            status_code=503,
            detail="RAG engine loading — please retry in ~15 seconds."
        )
    return G
