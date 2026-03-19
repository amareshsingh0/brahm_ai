#!/usr/bin/env python3
"""
Brahm AI - End-to-End RAG Pipeline Test
Combines: FAISS Search + Document Retrieval + Qwen LLM Generation

Run on VM:
    cd ~/books
    source ~/ai-env/bin/activate
    python3 scripts/05_test_rag.py
    python3 scripts/05_test_rag.py --query "भगवद्गीता में कर्म योग क्या है?"
    python3 scripts/05_test_rag.py --lang sanskrit --top-k 3
"""

import os
import sys
import json
import time
import sqlite3
import argparse
import numpy as np

# ============================================================
# CONFIG
# ============================================================
BASE_DIR = os.path.expanduser("~/books")
INDEX_DIR = os.path.join(BASE_DIR, "indexes")
DOCS_JSONL = os.path.join(INDEX_DIR, "documents.jsonl")
METADATA_DB = os.path.join(INDEX_DIR, "metadata.db")

EMBED_MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
LLM_MODEL_NAME = "Qwen/Qwen2.5-7B-Instruct"

# Search defaults
DEFAULT_TOP_K = 5
FAISS_SEARCH_K = 20

# ============================================================
# STEP 1: Load FAISS Indexes
# ============================================================
def load_faiss_indexes():
    """Load all language FAISS indexes"""
    import faiss

    indexes = {}
    for lang in ["sanskrit", "hindi", "english"]:
        path = os.path.join(INDEX_DIR, f"{lang}.index")
        if os.path.exists(path):
            idx = faiss.read_index(path)
            indexes[lang] = idx
            print(f"  {lang}: {idx.ntotal} vectors")
        else:
            print(f"  {lang}: NOT FOUND")

    # Also load master if needed
    master_path = os.path.join(INDEX_DIR, "master.index")
    if os.path.exists(master_path):
        indexes["master"] = faiss.read_index(master_path)
        print(f"  master: {indexes['master'].ntotal} vectors")

    return indexes


# ============================================================
# STEP 2: Load Documents Store
# ============================================================
def load_documents():
    """Load documents.jsonl into a lookup dict keyed by (language, local_faiss_id)"""
    print("Loading documents.jsonl...")

    # First pass: collect all docs per language and sort by faiss_id
    lang_docs = {}  # {language: [(faiss_id, doc), ...]}

    with open(DOCS_JSONL, 'r', encoding='utf-8') as f:
        for line in f:
            doc = json.loads(line)
            lang = doc.get("language", "unknown")
            fid = int(doc.get("faiss_id", -1))
            if lang not in lang_docs:
                lang_docs[lang] = []
            lang_docs[lang].append((fid, doc))

    # Sort each language by faiss_id and create local_id mapping
    # The per-language FAISS index uses 0-based local IDs
    # But documents.jsonl has global faiss_ids (with offsets)
    docs_by_local_id = {}  # {(language, local_idx): doc}

    for lang, items in lang_docs.items():
        items.sort(key=lambda x: x[0])  # sort by global faiss_id
        for local_idx, (global_fid, doc) in enumerate(items):
            docs_by_local_id[(lang, local_idx)] = doc

    total = sum(len(v) for v in lang_docs.values())
    print(f"  Loaded {total} documents")
    for lang, items in lang_docs.items():
        print(f"    {lang}: {len(items)} docs (global IDs {items[0][0]}..{items[-1][0]})")

    return docs_by_local_id


# ============================================================
# STEP 3: Load Embedding Model
# ============================================================
def load_embed_model():
    """Load sentence-transformers embedding model"""
    from sentence_transformers import SentenceTransformer
    print(f"Loading embedding model: {EMBED_MODEL_NAME}")
    model = SentenceTransformer(EMBED_MODEL_NAME)
    return model


# ============================================================
# STEP 4: Load Qwen LLM
# ============================================================
def load_llm():
    """Load Qwen2.5-7B-Instruct with 4-bit quantization"""
    import torch
    from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig

    print(f"Loading LLM: {LLM_MODEL_NAME} (4-bit)")
    start = time.time()

    bnb_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_compute_dtype=torch.float16,
        bnb_4bit_quant_type='nf4'
    )

    tokenizer = AutoTokenizer.from_pretrained(LLM_MODEL_NAME)
    model = AutoModelForCausalLM.from_pretrained(
        LLM_MODEL_NAME,
        quantization_config=bnb_config,
        device_map='auto'
    )

    elapsed = time.time() - start
    vram = torch.cuda.memory_allocated() / 1e9
    print(f"  LLM loaded in {elapsed:.1f}s, VRAM: {vram:.1f} GB")

    return model, tokenizer


# ============================================================
# STEP 5: FAISS Search
# ============================================================
def faiss_search(query, embed_model, indexes, lang=None, top_k=FAISS_SEARCH_K):
    """
    Search FAISS indexes.
    If lang specified, search that language index only.
    Otherwise search master index.
    """
    # Encode query
    query_vec = embed_model.encode([query], normalize_embeddings=True).astype(np.float32)

    results = []

    if lang and lang in indexes:
        # Search specific language index
        D, I = indexes[lang].search(query_vec, top_k)
        for rank, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx >= 0:
                results.append({
                    "language": lang,
                    "local_id": int(idx),
                    "distance": float(dist),
                    "rank": rank,
                })
    else:
        # Search all language indexes and merge
        for search_lang, idx_obj in indexes.items():
            if search_lang == "master":
                continue
            D, I = idx_obj.search(query_vec, top_k)
            for rank, (dist, idx) in enumerate(zip(D[0], I[0])):
                if idx >= 0:
                    results.append({
                        "language": search_lang,
                        "local_id": int(idx),
                        "distance": float(dist),
                        "rank": rank,
                    })

        # Sort by distance (lower = more similar for L2, higher for IP)
        # Our embeddings are normalized so HNSW uses inner product
        results.sort(key=lambda x: x["distance"], reverse=True)
        results = results[:top_k]

    return results


# ============================================================
# STEP 6: Retrieve Full Documents
# ============================================================
def retrieve_documents(search_results, docs_store):
    """Fetch full text for search results"""
    retrieved = []
    for r in search_results:
        key = (r["language"], r["local_id"])
        doc = docs_store.get(key)
        if doc:
            retrieved.append({
                **r,
                "text": doc.get("text", ""),
                "source": doc.get("source", "unknown"),
                "book_id": doc.get("book_id", "unknown"),
                "category": doc.get("category", "unknown"),
                "chunk_id": doc.get("chunk_id", ""),
            })
        else:
            retrieved.append({
                **r,
                "text": f"[Document not found: {key}]",
                "source": "unknown",
                "book_id": "unknown",
                "category": "unknown",
                "chunk_id": "",
            })
    return retrieved


# ============================================================
# STEP 7: Build RAG Prompt + Generate
# ============================================================
SYSTEM_PROMPT = """You are Brahm AI, an expert in Sanatan Dharma, Vedic scriptures, Sanskrit texts, Jyotish (Vedic Astrology), and Hindu philosophy.

RULES:
1. Answer ONLY based on the provided context from authentic scriptures.
2. If context is insufficient, say so honestly — do NOT make up information.
3. Always cite the source (book name, chapter/verse if available).
4. Respond in the same language as the user's question (Hindi question = Hindi answer, English = English).
5. For Sanskrit verses, provide the original text and explain its meaning.
6. Be respectful and scholarly in tone."""


def build_rag_prompt(query, retrieved_docs, max_context_tokens=2000):
    """Build the RAG prompt with retrieved context"""

    # Build context from retrieved documents
    context_parts = []
    total_chars = 0
    max_chars = max_context_tokens * 4  # rough token-to-char estimate

    for i, doc in enumerate(retrieved_docs, 1):
        text = doc["text"]
        source = doc["source"]
        lang = doc["language"]

        # Truncate individual chunks if too long
        if len(text) > 800:
            text = text[:800] + "..."

        chunk_text = f"[Source {i}: {source} ({lang})]\n{text}"

        if total_chars + len(chunk_text) > max_chars:
            break

        context_parts.append(chunk_text)
        total_chars += len(chunk_text)

    context = "\n\n---\n\n".join(context_parts)

    user_message = f"""Context from scriptures:
{context}

---

Question: {query}

Based on the above context, provide a detailed answer with source citations."""

    return [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_message},
    ]


def generate_answer(messages, model, tokenizer, max_new_tokens=512):
    """Generate answer using Qwen LLM"""
    import torch

    text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(text, return_tensors="pt").to(model.device)

    start = time.time()
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            temperature=0.7,
            top_p=0.9,
            do_sample=True,
            repetition_penalty=1.1,
        )
    elapsed = time.time() - start

    # Decode only the generated part
    generated = outputs[0][inputs["input_ids"].shape[1]:]
    answer = tokenizer.decode(generated, skip_special_tokens=True)

    tokens_generated = len(generated)
    tokens_per_sec = tokens_generated / elapsed if elapsed > 0 else 0

    return answer, {
        "tokens": tokens_generated,
        "time": elapsed,
        "tokens_per_sec": tokens_per_sec,
    }


# ============================================================
# STEP 8: Full RAG Pipeline
# ============================================================
def rag_query(query, embed_model, indexes, docs_store, llm_model, llm_tokenizer,
              lang=None, top_k=DEFAULT_TOP_K):
    """Full RAG pipeline: Search -> Retrieve -> Generate"""

    print(f"\n{'='*60}")
    print(f"  QUERY: {query}")
    if lang:
        print(f"  LANGUAGE FILTER: {lang}")
    print(f"{'='*60}")

    # 1. Search
    t0 = time.time()
    search_results = faiss_search(query, embed_model, indexes, lang=lang, top_k=top_k)
    search_time = time.time() - t0
    print(f"\n[Search] {len(search_results)} results in {search_time:.3f}s")

    # 2. Retrieve
    retrieved = retrieve_documents(search_results, docs_store)

    # Show retrieved sources
    print("\n[Retrieved Sources]")
    for i, doc in enumerate(retrieved, 1):
        src = doc["source"]
        lang_tag = doc["language"]
        preview = doc["text"][:100].replace('\n', ' ')
        print(f"  [{i}] {src} ({lang_tag}) — {preview}...")

    # 3. Generate
    print("\n[Generating RAG answer...]")
    messages = build_rag_prompt(query, retrieved)
    answer, stats = generate_answer(messages, llm_model, llm_tokenizer)

    print(f"\n[Answer] ({stats['tokens']} tokens, {stats['time']:.1f}s, {stats['tokens_per_sec']:.1f} tok/s)")
    print(f"{'─'*60}")
    print(answer)
    print(f"{'─'*60}")

    return {
        "query": query,
        "answer": answer,
        "sources": retrieved,
        "stats": stats,
        "search_time": search_time,
    }


# ============================================================
# MAIN
# ============================================================
def main():
    parser = argparse.ArgumentParser(description="Brahm AI - RAG Pipeline Test")
    parser.add_argument("--query", "-q", type=str, help="Single query to test")
    parser.add_argument("--lang", "-l", type=str, choices=["sanskrit", "hindi", "english"],
                        help="Filter search to specific language")
    parser.add_argument("--top-k", "-k", type=int, default=DEFAULT_TOP_K,
                        help=f"Number of chunks to retrieve (default: {DEFAULT_TOP_K})")
    parser.add_argument("--no-llm", action="store_true",
                        help="Skip LLM generation, only show search results")
    args = parser.parse_args()

    print("=" * 60)
    print("  BRAHM AI - RAG Pipeline Test")
    print("=" * 60)

    # Load all components
    print("\n[1/4] Loading FAISS indexes...")
    indexes = load_faiss_indexes()

    print("\n[2/4] Loading document store...")
    docs_store = load_documents()

    print("\n[3/4] Loading embedding model...")
    embed_model = load_embed_model()

    if not args.no_llm:
        print("\n[4/4] Loading Qwen LLM...")
        llm_model, llm_tokenizer = load_llm()
    else:
        print("\n[4/4] Skipping LLM (--no-llm)")
        llm_model, llm_tokenizer = None, None

    print("\n" + "=" * 60)
    print("  All components loaded. Ready for queries.")
    print("=" * 60)

    # Single query mode
    if args.query:
        if args.no_llm:
            # Just search, no generation
            results = faiss_search(args.query, embed_model, indexes, lang=args.lang, top_k=args.top_k)
            retrieved = retrieve_documents(results, docs_store)
            print(f"\n[Search Results for: {args.query}]")
            for i, doc in enumerate(retrieved, 1):
                print(f"\n  [{i}] {doc['source']} ({doc['language']})")
                print(f"      Category: {doc['category']}")
                print(f"      Text: {doc['text'][:300]}")
                print(f"      {'─'*50}")
        else:
            rag_query(args.query, embed_model, indexes, docs_store,
                     llm_model, llm_tokenizer, lang=args.lang, top_k=args.top_k)
        return

    # Default: run test queries
    test_queries = [
        ("भगवद्गीता में कर्म योग क्या है?", None),
        ("What is the concept of Dharma in Vedic texts?", None),
        ("kundali mein surya ka mahatva kya hai?", "hindi"),
        ("कुण्डली में राहु-केतु का प्रभाव", None),
        ("Explain the significance of Gayatri Mantra", "sanskrit"),
    ]

    results = []
    for query, lang_filter in test_queries:
        lang = args.lang or lang_filter  # CLI arg overrides

        if args.no_llm:
            search_results = faiss_search(query, embed_model, indexes, lang=lang, top_k=args.top_k)
            retrieved = retrieve_documents(search_results, docs_store)
            print(f"\n{'='*60}")
            print(f"  QUERY: {query} (lang={lang or 'all'})")
            print(f"{'='*60}")
            for i, doc in enumerate(retrieved[:3], 1):
                print(f"  [{i}] {doc['source']} ({doc['language']}) — {doc['text'][:150]}...")
        else:
            result = rag_query(query, embed_model, indexes, docs_store,
                             llm_model, llm_tokenizer, lang=lang, top_k=args.top_k)
            results.append(result)

    # Summary
    if results:
        print("\n" + "=" * 60)
        print("  RAG TEST SUMMARY")
        print("=" * 60)
        for r in results:
            print(f"\n  Q: {r['query']}")
            print(f"  Search: {r['search_time']:.3f}s | Gen: {r['stats']['time']:.1f}s | "
                  f"Tokens: {r['stats']['tokens']} ({r['stats']['tokens_per_sec']:.1f} tok/s)")
            print(f"  Sources: {', '.join(d['source'][:30] for d in r['sources'][:3])}")
            print(f"  Answer: {r['answer'][:150]}...")


if __name__ == "__main__":
    main()
