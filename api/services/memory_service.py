"""
Memory Service — Long-term cross-session memory for Brahm AI chat.

Fetches relevant past conversations from Supabase chat_messages table
using BM25 keyword relevance scoring. No schema changes needed.

Flow:
  1. Fetch user's last 14 days of chat pairs (user msg + AI reply)
  2. BM25 score each pair against current query
  3. Return top N most relevant past exchanges
  4. Injected into Pass 2 prompt as [MEMORY] section
"""
import re
from datetime import datetime, timedelta, timezone
from typing import List, Dict

# BM25 constants
_BM25_K1 = 1.5
_BM25_B = 0.75
_MEMORY_DAYS = 14      # Look back 14 days
_MAX_FETCH = 150       # Max messages to fetch from DB
_TOP_N = 3             # Top N relevant exchanges to inject


def _tokenize(text: str) -> List[str]:
    """Simple tokenizer: lowercase + split on non-alphanumeric."""
    return re.findall(r"[a-z0-9\u0900-\u097f]+", text.lower())


def _bm25_score(query_tokens: List[str], doc_tokens: List[str], avg_dl: float) -> float:
    """BM25 score for a single document."""
    from collections import Counter
    tf = Counter(doc_tokens)
    dl = len(doc_tokens)
    score = 0.0
    for token in query_tokens:
        if token not in tf:
            continue
        f = tf[token]
        score += f * (_BM25_K1 + 1) / (f + _BM25_K1 * (1 - _BM25_B + _BM25_B * dl / max(avg_dl, 1)))
    return score


def get_relevant_memory(
    user_id: str,
    query: str,
    current_session_id: str = "",
    top_n: int = _TOP_N,
) -> List[Dict]:
    """
    Fetch top N relevant past exchanges for this user.

    Returns list of dicts:
        {"user_msg": str, "ai_reply": str, "date": str, "context": str}
    Empty list if no user_id, DB error, or no relevant results.
    """
    if not user_id:
        return []

    try:
        from api.supabase_client import get_supabase
        sb = get_supabase()
        if not sb:
            return []

        # Date cutoff — last N days
        cutoff = (datetime.now(timezone.utc) - timedelta(days=_MEMORY_DAYS)).isoformat()

        result = sb.table("chat_messages") \
            .select("role, content, created_at, session_id, page_context") \
            .eq("user_id", user_id) \
            .gte("created_at", cutoff) \
            .order("created_at", desc=False) \
            .limit(_MAX_FETCH) \
            .execute()

        rows = result.data or []
        if not rows:
            return []

        # Pair up user+assistant messages into exchanges
        exchanges = _pair_messages(rows, current_session_id)
        if not exchanges:
            return []

        # BM25 relevance scoring
        query_tokens = _tokenize(query)
        if not query_tokens:
            return []

        # Compute avg doc length
        doc_token_lists = [_tokenize(ex["user_msg"]) for ex in exchanges]
        avg_dl = sum(len(t) for t in doc_token_lists) / max(len(doc_token_lists), 1)

        scored = []
        for i, ex in enumerate(exchanges):
            score = _bm25_score(query_tokens, doc_token_lists[i], avg_dl)
            if score > 0.3:  # Minimum relevance threshold
                scored.append((score, ex))

        # Sort by score descending, return top N
        scored.sort(key=lambda x: x[0], reverse=True)
        return [ex for _, ex in scored[:top_n]]

    except Exception:
        return []


def _pair_messages(rows: List[dict], exclude_session: str) -> List[Dict]:
    """
    Pair consecutive user + assistant messages into exchanges.
    Skip current session (it's already in history).
    """
    exchanges = []
    i = 0
    while i < len(rows) - 1:
        current = rows[i]
        nxt = rows[i + 1]

        # Skip current session
        if current.get("session_id") == exclude_session:
            i += 1
            continue

        if current.get("role") == "user" and nxt.get("role") == "assistant":
            user_msg = (current.get("content") or "").strip()
            ai_reply = (nxt.get("content") or "").strip()

            # Skip very short or empty messages
            if len(user_msg) > 5 and len(ai_reply) > 10:
                # Format date nicely
                raw_date = current.get("created_at", "")
                try:
                    dt = datetime.fromisoformat(raw_date.replace("Z", "+00:00"))
                    date_str = dt.strftime("%d %b %Y")
                except Exception:
                    date_str = "earlier"

                exchanges.append({
                    "user_msg": user_msg[:300],
                    "ai_reply": ai_reply[:500],
                    "date": date_str,
                    "context": current.get("page_context", "general"),
                })
            i += 2  # Skip both messages
        else:
            i += 1

    return exchanges


def format_memory_for_prompt(exchanges: List[Dict]) -> str:
    """Format memory exchanges into a prompt section."""
    if not exchanges:
        return ""

    lines = ["[MEMORY — Pehle Ki Relevant Baatein (do not repeat, use as context)]"]
    for ex in exchanges:
        lines.append(f"\n  [{ex['date']} — {ex['context']} page]")
        lines.append(f"  User: {ex['user_msg']}")
        lines.append(f"  Brahm AI: {ex['ai_reply'][:300]}{'...' if len(ex['ai_reply']) > 300 else ''}")

    lines.append(
        "\n  [Agar user in topics ka reference kare ya continue karna chahein, "
        "toh memory use karo. Auto-inject mat karo — sirf jab relevant ho.]"
    )
    return "\n".join(lines)
