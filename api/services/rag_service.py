"""
RAG service — lazy-loaded on first /api/chat request.
FAISS + BM25 + Embedding + Reranker for retrieval.
Gemini API for answer generation (Qwen removed 2026-03-19).
"""
import os, json, re
from typing import List, Generator

import numpy as np

from api.config import (
    INDEXES_DIR, DOCS_PATH, LOG_DIR,
    EMBED_MODEL, RERANKER_MODEL,
    FAISS_K, BM25_K, FINAL_K, MAX_HISTORY,
    SYSTEM_PROMPT
)
from api.dependencies import G

GEMINI_MODEL = "models/gemini-2.5-flash"


def load_all():
    """Load FAISS, BM25, embedder, and reranker into shared G dict."""
    import faiss
    from sentence_transformers import SentenceTransformer, CrossEncoder
    from rank_bm25 import BM25Okapi

    print("[RAG] Loading FAISS indexes...")
    G["indexes"] = {}
    for lang in ["sanskrit", "hindi", "english"]:
        p = os.path.join(INDEXES_DIR, f"{lang}.index")
        if os.path.exists(p):
            G["indexes"][lang] = faiss.read_index(p)
            print(f"  {lang}: {G['indexes'][lang].ntotal}")

    print("[RAG] Loading documents + BM25...")
    import pickle
    BM25_CACHE = os.path.join(INDEXES_DIR, "bm25_cache.pkl")
    docs = {}
    lang_docs: dict = {"sanskrit": [], "hindi": [], "english": []}
    with open(DOCS_PATH) as f:
        for line in f:
            d = json.loads(line)
            lang_docs[d.get("language", "sanskrit")].append(d)
    all_keys, all_texts = [], []
    for lang in lang_docs:
        for local_idx, d in enumerate(sorted(lang_docs[lang], key=lambda x: x.get("faiss_id", 0))):
            docs[(lang, local_idx)] = d
            all_keys.append((lang, local_idx))
            all_texts.append(d.get("text", "").lower().split())
    G["docs"] = docs
    G["all_keys"] = all_keys
    if os.path.exists(BM25_CACHE):
        print("  BM25 loading from cache...")
        with open(BM25_CACHE, "rb") as f:
            G["bm25"] = pickle.load(f)
    else:
        print("  BM25 building index (first time, will cache)...")
        G["bm25"] = BM25Okapi(all_texts)
        with open(BM25_CACHE, "wb") as f:
            pickle.dump(G["bm25"], f)
        print("  BM25 cached to disk.")
    print(f"  {len(docs)} docs, BM25 ready")

    print("[RAG] Loading embedding model...")
    G["embed"] = SentenceTransformer(EMBED_MODEL)

    print("[RAG] Loading cross-encoder...")
    G["reranker"] = CrossEncoder(RERANKER_MODEL)

    G["ocr_quality"] = {}
    try:
        cr = json.load(open(os.path.join(LOG_DIR, "correction_report.json")))
        for b in cr.get("book_scores", []):
            G["ocr_quality"][b["source"]] = min(1.0, b["avg_score"] / 0.80)
        print(f"  OCR quality: {len(G['ocr_quality'])} files")
    except Exception:
        pass

    G["loaded"] = True
    print("[RAG] All components loaded!")


def _src_boost(src: str) -> float:
    s = src.lower()
    if any(s.startswith(p) for p in ["bhagavadgita/", "sarit/", "gretil/", "suttacentral/"]):
        return 1.1
    if s.startswith("openphilology/"): return 1.0
    if any(s.startswith(p) for p in ["wikisource/", "wikipedia_"]): return 0.95
    if "sanskrit_mono" in s or "hf/mono" in s: return 0.30
    if s.startswith("hf/"): return 0.85
    if s.endswith(".pdf") and re.search(r"\d{4}", s): return 0.75
    return 0.90


def hybrid_search(query: str, lang_filter: str = "all") -> List[dict]:
    """FAISS + BM25 → RRF → cross-encoder rerank. Returns top FINAL_K docs."""
    indexes = G["indexes"]
    docs = G["docs"]
    all_keys = G["all_keys"]
    bm25 = G["bm25"]
    embed = G["embed"]
    reranker = G["reranker"]
    ocr_quality = G.get("ocr_quality", {})

    qv = embed.encode([query], normalize_embeddings=True).astype(np.float32)
    deva = sum(1 for c in query if "\u0900" <= c <= "\u097F")
    if lang_filter and lang_filter != "all":
        search_langs = [lang_filter]
    elif deva / max(len(query.replace(" ", "")), 1) > 0.2:
        search_langs = ["sanskrit", "hindi"]
    else:
        search_langs = list(indexes.keys())

    faiss_res = {}
    for sl in search_langs:
        if sl not in indexes: continue
        D, I = indexes[sl].search(qv, FAISS_K)
        for rank, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx >= 0:
                faiss_res[(sl, int(idx))] = {"faiss_rank": rank, "faiss_dist": float(dist)}

    tokens = query.lower().split()
    bm25_scores = bm25.get_scores(tokens)
    top_idx = np.argsort(bm25_scores)[::-1][:BM25_K]
    bm25_res = {}
    for rank, idx in enumerate(top_idx):
        key = all_keys[idx]
        if lang_filter != "all" and key[0] != lang_filter: continue
        if bm25_scores[idx] > 0:
            bm25_res[key] = {"bm25_rank": rank}

    k = 60
    rrf = {}
    for key in set(faiss_res) | set(bm25_res):
        score = 0.0
        if key in faiss_res: score += 1.0 / (k + faiss_res[key]["faiss_rank"])
        if key in bm25_res:  score += 1.0 / (k + bm25_res[key]["bm25_rank"])
        rrf[key] = score

    top_keys = sorted(rrf, key=lambda x: rrf[x], reverse=True)[:FAISS_K]
    candidates = []
    for key in top_keys:
        doc = docs.get(key)
        if doc:
            candidates.append({
                "key": key,
                "text": doc.get("text", ""),
                "source": doc.get("source", "?"),
                "language": key[0],
                "rrf_score": rrf[key],
            })

    if candidates:
        pairs = [(query, c["text"][:512]) for c in candidates]
        scores = reranker.predict(pairs)
        for i, c in enumerate(candidates):
            c["rerank_score"] = float(scores[i])
        for c in candidates:
            q = ocr_quality.get(c["source"].split("/")[-1], ocr_quality.get(c["source"], 1.0))
            c["rerank_score"] *= _src_boost(c["source"]) * q
        candidates.sort(key=lambda x: x["rerank_score"], reverse=True)

    return candidates[:FINAL_K]


def generate_stream(query: str, retrieved: List[dict], history: List[dict]) -> Generator[str, None, None]:
    """Stream response from Gemini API. Yields JSON strings: token, sources, done."""
    from google import genai

    def _clean(t: str) -> str:
        return re.sub(r"Transliteration:[^\n]*\n?", "", t).strip()

    ctx = "\n\n---\n\n".join(
        f"[Source {i}: {d['source']} ({d['language']})]\n{_clean(d['text'][:1000])}"
        for i, d in enumerate(retrieved, 1)
    )

    history_text = ""
    for turn in history[-MAX_HISTORY:]:
        history_text += f"{turn['role'].capitalize()}: {turn['content']}\n"

    prompt = (
        f"{SYSTEM_PROMPT}\n\n"
        f"{history_text}"
        f"Context:\n{ctx}\n\n---\n"
        f"Question: {query}\n\n"
        f"Answer strictly from the context above."
    )

    api_key = os.environ.get("GEMINI_API_KEY", "")
    client = genai.Client(api_key=api_key)

    try:
        for chunk in client.models.generate_content_stream(
            model=GEMINI_MODEL,
            contents=prompt,
        ):
            if chunk.text:
                yield json.dumps({"type": "token", "content": chunk.text})
    except Exception as e:
        yield json.dumps({"type": "token", "content": f"\n[Error: {str(e)}]"})

    sources = [
        {"book": d["source"].split("/")[0], "source": d["source"], "language": d["language"]}
        for d in retrieved
    ]
    yield json.dumps({"type": "sources", "sources": sources})
    yield json.dumps({"type": "done"})
