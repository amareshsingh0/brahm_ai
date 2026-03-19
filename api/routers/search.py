import asyncio
from fastapi import APIRouter, Depends, Query
from api.dependencies import get_rag_state
from api.services.rag_service import hybrid_search

router = APIRouter()


@router.get("/search")
async def search(
    q: str = Query(..., description="Search query"),
    lang: str = Query(default="all"),
    limit: int = Query(default=10, le=20),
    state: dict = Depends(get_rag_state),
):
    results = await asyncio.get_event_loop().run_in_executor(
        None, lambda: hybrid_search(q, lang)
    )
    out = []
    for r in results[:limit]:
        out.append({
            "text": r["text"][:500],
            "source": r["source"],
            "language": r["language"],
            "score": round(r.get("rerank_score", r.get("rrf_score", 0)), 4),
        })
    return {"results": out}
