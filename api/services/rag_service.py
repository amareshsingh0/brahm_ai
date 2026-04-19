"""
RAG service — lazy-loaded on first /api/chat request.
FAISS + BM25 + Embedding + Reranker for retrieval.
Gemini API for answer generation (Qwen removed 2026-03-19).
"""
import os, json, re, time
from typing import List, Generator

# ── Clean book name from raw source path ──────────────────────────────────────
_SOURCE_OVERRIDES = {
    # Folder-level overrides (source.split("/")[0])
    "bhagavadgita":   "Bhagavad Gita",
    "sarit":          "SARIT Sanskrit Corpus",
    "gretil":         "GRETIL Sanskrit Texts",
    "suttacentral":   "Sutta Central",
    "openphilology":  "Open Philology",
    "wikisource":     "Wikisource",
    "hf":             "Sanskrit Texts",
    # Common PDF/file name fragments → proper names
    "brihat_parashara": "Brihat Parashara Hora Shastra",
    "bphs":             "Brihat Parashara Hora Shastra",
    "jataka_parijata":  "Jataka Parijata",
    "phaladeepika":     "Phala Deepika",
    "saravali":         "Saravali",
    "uttara_kalamrita": "Uttara Kalamrita",
    "hora_sara":        "Hora Sara",
    "brihat_jataka":    "Brihat Jataka",
    "chamatkar_chintamani": "Chamatkar Chintamani",
    "sarvartha_chintamani": "Sarvartha Chintamani",
    "mansagari":        "Mansagari",
    "laghu_parashari":  "Laghu Parashari",
    "jaimini":          "Jaimini Sutras",
    "mahabharata":      "Mahabharata",
    "ramayana":         "Ramayana",
    "srimad_bhagavatam": "Srimad Bhagavatam",
    "vishnu_purana":    "Vishnu Purana",
    "garuda_purana":    "Garuda Purana",
    "agni_purana":      "Agni Purana",
    "manu_smriti":      "Manu Smriti",
    "arthashastra":     "Arthashastra",
    "yoga_sutras":      "Yoga Sutras",
    "upanishad":        "Upanishads",
    "wikipedia":        "Wikipedia",
}


def _clean_source_name(source: str) -> str:
    """Convert raw source path to a clean, readable book name."""
    # Get filename without extension
    filename = source.split("/")[-1]
    name_raw = re.sub(r"\.(pdf|txt|htm|html|json|xml)$", "", filename, flags=re.I)
    # Remove trailing year/numbers like _2003, _v2
    name_raw = re.sub(r"[_-]?\d{4}[a-z]?$", "", name_raw)
    name_raw_lower = name_raw.lower()

    # Check filename-level overrides
    for key, val in _SOURCE_OVERRIDES.items():
        if key in name_raw_lower:
            return val

    # Check folder-level override
    folder = source.split("/")[0].lower()
    if folder in _SOURCE_OVERRIDES:
        # If we have a clean filename, use it; else use folder override
        if name_raw and name_raw_lower not in ("", folder):
            clean = re.sub(r"[_\-]+", " ", name_raw).strip().title()
            # Remove common noise words
            clean = re.sub(r"\b(Vol|Part|Chapter|Book|Text|Doc|File)\b", "", clean, flags=re.I).strip()
            return clean if len(clean) > 3 else _SOURCE_OVERRIDES[folder]
        return _SOURCE_OVERRIDES[folder]

    # Generic clean: replace separators, title case
    clean = re.sub(r"[_\-]+", " ", name_raw).strip().title()
    return clean if clean else source

import numpy as np

from api.config import (
    INDEXES_DIR, DOCS_PATH, LOG_DIR,
    EMBED_MODEL, RERANKER_MODEL,
    FAISS_K, BM25_K, FINAL_K, MAX_HISTORY,
    SYSTEM_PROMPT
)
from api.dependencies import G

GEMINI_MODEL         = "models/gemini-2.5-flash"
GEMINI_FALLBACK_MODEL = "models/gemini-1.5-flash"


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


def _is_retryable_error(e: Exception) -> bool:
    """Check if the exception is a transient 503/overload error from Gemini."""
    msg = str(e).lower()
    return any(x in msg for x in ["503", "service unavailable", "overloaded", "resource exhausted", "429", "too many requests"])


# ── Thinking budget per query type ────────────────────────────────────────────
# Gemini 2.5 Flash is a "thinking" model — it does internal chain-of-thought
# before generating visible tokens. This causes a long pause before streaming
# starts. We control this with ThinkingConfig.thinking_budget:
#   0     = disable thinking → instant streaming (greetings, small talk)
#   1024  = minimal thinking  (simple facts, general knowledge)
#   4096  = moderate thinking  (chart analysis, predictions)
#   -1    = unlimited (default, very slow — we avoid this)
_THINKING_BUDGET = {
    "CONVERSATIONAL":     0,
    "SMALL_TALK":         0,
    "GENERAL_KNOWLEDGE":  1024,
    "SIMPLE_FACT":        1024,
    "REPORT_ANALYSIS":    2048,
    "DEEP_VEDIC":         4096,
    "CHART_ANALYSIS":     4096,
    "RECOMMENDATION":     4096,
}


def generate_stream(
    query: str,
    retrieved: List[dict],
    history: List[dict],
    decision: dict = {},
    tool_results: dict = {},
    kundali_summary: dict = None,
    page_context: str = "general",
    page_data: dict = {},
    language: str = "hi",
    memory_section: str = "",
) -> Generator[str, None, None]:
    """Pass 2: Build final prompt from all context and stream from Gemini.
    
    - Uses ThinkingConfig to control thinking budget per query type
    - Retries up to 3 times with exponential backoff on 503/overload errors
    - Falls back to gemini-1.5-flash if gemini-2.5-flash is persistently unavailable
    """
    from google import genai
    from google.genai import types
    from api.services.prompt_builder import build_pass2_prompt

    prompt = build_pass2_prompt(
        query=query,
        decision=decision,
        tool_results=tool_results,
        kundali_summary=kundali_summary,
        page_context=page_context,
        page_data=page_data,
        rag_docs=retrieved,
        language=language,
        history=history,
        memory_section=memory_section,
    )

    api_key = os.environ.get("GEMINI_API_KEY", "")
    client = genai.Client(api_key=api_key)

    # Determine thinking budget based on query type
    query_type = decision.get("query_type", "CHART_ANALYSIS")
    thinking_budget = _THINKING_BUDGET.get(query_type, 4096)

    # Build generation config with thinking control
    try:
        gen_config = types.GenerateContentConfig(
            thinking_config=types.ThinkingConfig(
                thinking_budget=thinking_budget,
            ),
        )
    except Exception:
        # SDK doesn't support ThinkingConfig — proceed without it
        gen_config = None

    # Try primary model with retries, then fallback model
    # Note: fallback model (1.5-flash) doesn't support thinking, so no config
    models_to_try = [
        (GEMINI_MODEL, 3, gen_config),            # (model, max_attempts, config)
        (GEMINI_FALLBACK_MODEL, 1, None),          # fallback — no thinking config
    ]

    last_error = None
    for model, max_attempts, config in models_to_try:
        for attempt in range(1, max_attempts + 1):
            try:
                stream_kwargs = {"model": model, "contents": prompt}
                if config:
                    stream_kwargs["config"] = config

                for chunk in client.models.generate_content_stream(**stream_kwargs):
                    # Filter out internal thinking tokens (thought=True)
                    # Only emit actual response text
                    if chunk.text:
                        # Check if this is a thinking chunk (internal reasoning)
                        is_thought = False
                        if hasattr(chunk, 'candidates') and chunk.candidates:
                            for candidate in chunk.candidates:
                                if hasattr(candidate, 'content') and candidate.content:
                                    for part in (candidate.content.parts or []):
                                        if getattr(part, 'thought', False):
                                            is_thought = True
                                            break
                        if not is_thought:
                            yield json.dumps({"type": "token", "content": chunk.text})
                # Success — emit sources and done
                sources = [
                    {"book": _clean_source_name(d["source"]), "source": d["source"], "language": d["language"]}
                    for d in retrieved
                ]
                yield json.dumps({"type": "sources", "sources": sources})
                yield json.dumps({"type": "done"})
                return
            except Exception as e:
                last_error = e
                if _is_retryable_error(e):
                    if attempt < max_attempts:
                        wait = 2 ** attempt  # 2s, 4s
                        time.sleep(wait)
                        continue  # retry same model
                    # All attempts on this model exhausted — try fallback
                    break
                else:
                    # Non-retryable error (bad API key, malformed request, etc.)
                    yield json.dumps({"type": "token", "content": f"\n[Error: {str(e)}]"})
                    yield json.dumps({"type": "sources", "sources": []})
                    yield json.dumps({"type": "done"})
                    return

    # Both models failed — emit friendly error
    yield json.dumps({"type": "token", "content": "Abhi AI thoda busy hai, thodi der mein dobara try karein. 🙏"})
    yield json.dumps({"type": "sources", "sources": []})
    yield json.dumps({"type": "done"})
