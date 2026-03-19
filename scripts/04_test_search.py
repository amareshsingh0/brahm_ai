#!/usr/bin/env python3
"""
Brahm AI - Search Quality Test (No LLM needed)
Tests FAISS semantic search across all language indexes.

Run on VM:
    cd ~/books
    source ~/ai-env/bin/activate
    python3 scripts/04_test_search.py
"""

import os
import sys
import json
import time
import sqlite3
import numpy as np

BASE_DIR = os.path.expanduser("~/books")
INDEX_DIR = os.path.join(BASE_DIR, "indexes")
DOCS_JSONL = os.path.join(INDEX_DIR, "documents.jsonl")
METADATA_DB = os.path.join(INDEX_DIR, "metadata.db")
EMBED_MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"


def load_all():
    """Load indexes, docs, and embedding model"""
    import faiss
    from sentence_transformers import SentenceTransformer

    # FAISS indexes
    print("Loading FAISS indexes...")
    indexes = {}
    for lang in ["sanskrit", "hindi", "english"]:
        path = os.path.join(INDEX_DIR, f"{lang}.index")
        if os.path.exists(path):
            indexes[lang] = faiss.read_index(path)
            print(f"  {lang}: {indexes[lang].ntotal} vectors")

    # Documents
    print("Loading documents.jsonl...")
    lang_docs = {}
    with open(DOCS_JSONL, 'r', encoding='utf-8') as f:
        for line in f:
            doc = json.loads(line)
            lang = doc.get("language", "unknown")
            fid = int(doc.get("faiss_id", -1))
            if lang not in lang_docs:
                lang_docs[lang] = []
            lang_docs[lang].append((fid, doc))

    docs = {}
    for lang, items in lang_docs.items():
        items.sort(key=lambda x: x[0])
        for local_idx, (_, doc) in enumerate(items):
            docs[(lang, local_idx)] = doc

    total = sum(len(v) for v in lang_docs.values())
    print(f"  {total} documents loaded")

    # Embedding model
    print(f"Loading embedding model...")
    embed_model = SentenceTransformer(EMBED_MODEL_NAME)

    return indexes, docs, embed_model


def search(query, embed_model, indexes, docs, lang=None, top_k=5):
    """Search and return results with full text"""
    query_vec = embed_model.encode([query], normalize_embeddings=True).astype(np.float32)

    results = []
    search_langs = [lang] if lang else [l for l in indexes if l != "master"]

    for sl in search_langs:
        if sl not in indexes:
            continue
        D, I = indexes[sl].search(query_vec, top_k)
        for rank, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx >= 0:
                doc = docs.get((sl, int(idx)), {})
                results.append({
                    "rank": rank,
                    "distance": float(dist),
                    "language": sl,
                    "source": doc.get("source", "?"),
                    "category": doc.get("category", "?"),
                    "book_id": doc.get("book_id", "?"),
                    "text": doc.get("text", "[not found]"),
                })

    results.sort(key=lambda x: x["distance"], reverse=True)
    return results[:top_k]


def main():
    indexes, docs, embed_model = load_all()

    test_queries = [
        # Hindi queries
        ("भगवद्गीता में कर्म योग क्या सिखाया गया है?", None),
        ("kundali mein surya ka mahatva", "hindi"),
        ("राहु केतु का प्रभाव कुंडली में", None),

        # English queries
        ("What is the concept of Dharma?", "english"),
        ("Birth chart calculation in Vedic astrology", "english"),
        ("Yoga Sutras of Patanjali", None),

        # Sanskrit queries
        ("गायत्री मन्त्र का अर्थ", "sanskrit"),
        ("ॐ भूर्भुवः स्वः", "sanskrit"),
        ("ब्रह्म सूत्र", "sanskrit"),

        # Cross-language (search all)
        ("What are the four Vedas?", None),
        ("panchang tithi nakshatra", None),
    ]

    print("\n" + "=" * 70)
    print("  BRAHM AI - SEARCH QUALITY TEST")
    print("  Testing across all language indexes")
    print("=" * 70)

    for query, lang in test_queries:
        t0 = time.time()
        results = search(query, embed_model, indexes, docs, lang=lang, top_k=3)
        elapsed = time.time() - t0

        print(f"\n{'─'*70}")
        print(f"  Q: {query}")
        print(f"  Filter: {lang or 'all'} | Time: {elapsed:.3f}s | Results: {len(results)}")
        print(f"{'─'*70}")

        for i, r in enumerate(results, 1):
            text_preview = r["text"][:200].replace('\n', ' ')
            print(f"  [{i}] dist={r['distance']:.4f} | {r['language']} | {r['category']}")
            print(f"      src: {r['source'][:50]}")
            print(f"      {text_preview}...")
            print()

    print("=" * 70)
    print("  SEARCH TEST COMPLETE")
    print("=" * 70)


if __name__ == "__main__":
    main()
