#!/usr/bin/env python3
"""
Brahm AI - Interactive ChatGPT-like Terminal
Full RAG chat with conversation memory.

Run on VM:
    cd ~/books
    source ~/ai-env/bin/activate
    python3 scripts/06_chat_terminal.py

Commands:
    /lang [sanskrit|hindi|english|all]  - Set language filter
    /topk [N]                           - Set number of chunks to retrieve
    /sources                            - Show sources from last answer
    /clear                              - Clear chat history
    /search [query]                     - Search only (no LLM)
    /quit or /exit                      - Exit
"""

import os
import sys
import json
import time
import sqlite3
import numpy as np
import readline  # enables arrow keys in input

# ============================================================
# CONFIG
# ============================================================
BASE_DIR = os.path.expanduser("~/books")
INDEX_DIR = os.path.join(BASE_DIR, "indexes")
DOCS_JSONL = os.path.join(INDEX_DIR, "documents.jsonl")

EMBED_MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
LLM_MODEL_NAME = "Qwen/Qwen2.5-7B-Instruct"

MAX_HISTORY_TURNS = 5
DEFAULT_TOP_K = 5

SYSTEM_PROMPT = """You are Brahm AI, an expert in Sanatan Dharma, Vedic scriptures, Sanskrit texts, Jyotish (Vedic Astrology), and Hindu philosophy.

RULES:
1. Answer ONLY based on the provided context from authentic scriptures.
2. If context is insufficient, say "मुझे इस प्रश्न का उत्तर संदर्भ में नहीं मिला" — do NOT make up information.
3. Always cite the source (book name, chapter/verse if available).
4. Respond in the same language as the user's question (Hindi → Hindi, English → English).
5. For Sanskrit verses, provide the original text and explain its meaning.
6. Be respectful and scholarly in tone.
7. Keep answers concise but complete (max 300 words unless asked for detail)."""


# ============================================================
# LOADING
# ============================================================
def load_components():
    """Load all RAG components"""
    import faiss
    import torch
    from sentence_transformers import SentenceTransformer
    from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig

    print("\n  Loading Brahm AI components...\n")

    # 1. FAISS indexes
    print("  [1/4] FAISS indexes...")
    indexes = {}
    for lang in ["sanskrit", "hindi", "english"]:
        path = os.path.join(INDEX_DIR, f"{lang}.index")
        if os.path.exists(path):
            indexes[lang] = faiss.read_index(path)
            print(f"         {lang}: {indexes[lang].ntotal} vectors")

    # 2. Documents
    print("  [2/4] Document store...")
    lang_docs = {}
    with open(DOCS_JSONL, 'r', encoding='utf-8') as f:
        for line in f:
            doc = json.loads(line)
            lang = doc.get("language", "unknown")
            fid = int(doc.get("faiss_id", -1))
            if lang not in lang_docs:
                lang_docs[lang] = []
            lang_docs[lang].append((fid, doc))

    docs_store = {}
    for lang, items in lang_docs.items():
        items.sort(key=lambda x: x[0])
        for local_idx, (_, doc) in enumerate(items):
            docs_store[(lang, local_idx)] = doc

    total = sum(len(v) for v in lang_docs.values())
    print(f"         {total} documents")

    # 3. Embedding model
    print("  [3/4] Embedding model...")
    embed_model = SentenceTransformer(EMBED_MODEL_NAME)
    print(f"         {EMBED_MODEL_NAME.split('/')[-1]}")

    # 4. Qwen LLM
    print("  [4/4] Qwen LLM (4-bit quantization)...")
    t0 = time.time()
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
    vram = torch.cuda.memory_allocated() / 1e9
    print(f"         Loaded in {time.time()-t0:.0f}s, VRAM: {vram:.1f} GB")

    return indexes, docs_store, embed_model, model, tokenizer


# ============================================================
# SEARCH + RETRIEVE
# ============================================================
def search_and_retrieve(query, embed_model, indexes, docs_store, lang_filter=None, top_k=DEFAULT_TOP_K):
    """FAISS search + document retrieval"""
    query_vec = embed_model.encode([query], normalize_embeddings=True).astype(np.float32)

    results = []
    search_langs = [lang_filter] if lang_filter else [l for l in indexes if l != "master"]

    for sl in search_langs:
        if sl not in indexes:
            continue
        D, I = indexes[sl].search(query_vec, top_k * 2)
        for rank, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx >= 0:
                doc = docs_store.get((sl, int(idx)), {})
                results.append({
                    "distance": float(dist),
                    "language": sl,
                    "source": doc.get("source", "?"),
                    "category": doc.get("category", "?"),
                    "text": doc.get("text", "[not found]"),
                })

    results.sort(key=lambda x: x["distance"], reverse=True)
    return results[:top_k]


# ============================================================
# RAG GENERATION
# ============================================================
def generate_rag_answer(query, retrieved_docs, chat_history, model, tokenizer):
    """Build prompt with context + history and generate"""
    import torch

    # Build context
    context_parts = []
    total_chars = 0
    for i, doc in enumerate(retrieved_docs, 1):
        text = doc["text"][:800]
        chunk = f"[Source {i}: {doc['source']} ({doc['language']})]\n{text}"
        if total_chars + len(chunk) > 6000:
            break
        context_parts.append(chunk)
        total_chars += len(chunk)

    context = "\n\n---\n\n".join(context_parts)

    # Build messages with history
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    # Add recent chat history
    for turn in chat_history[-MAX_HISTORY_TURNS:]:
        messages.append({"role": "user", "content": turn["user"]})
        messages.append({"role": "assistant", "content": turn["assistant"]})

    # Current query with context
    user_msg = f"""Context from scriptures:
{context}

---

Question: {query}

Answer based on the above context. Cite sources."""

    messages.append({"role": "user", "content": user_msg})

    # Generate
    text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(text, return_tensors="pt").to(model.device)

    t0 = time.time()
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=512,
            temperature=0.7,
            top_p=0.9,
            do_sample=True,
            repetition_penalty=1.1,
        )
    elapsed = time.time() - t0

    generated = outputs[0][inputs["input_ids"].shape[1]:]
    answer = tokenizer.decode(generated, skip_special_tokens=True)
    tokens = len(generated)

    return answer, {"tokens": tokens, "time": elapsed, "tok_per_sec": tokens / elapsed if elapsed > 0 else 0}


# ============================================================
# CHAT LOOP
# ============================================================
def chat_loop(indexes, docs_store, embed_model, llm_model, llm_tokenizer):
    """Interactive chat with memory"""

    chat_history = []
    last_sources = []
    lang_filter = None
    top_k = DEFAULT_TOP_K

    print("\n" + "=" * 60)
    print("  🙏 Brahm AI — Vedic Knowledge Assistant")
    print("  Type your question. /help for commands. /quit to exit.")
    print("=" * 60)

    while True:
        try:
            query = input("\n  You: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n\n  Namaste! 🙏")
            break

        if not query:
            continue

        # Commands
        if query.startswith("/"):
            cmd = query.split()
            if cmd[0] in ("/quit", "/exit", "/q"):
                print("\n  Namaste! 🙏")
                break
            elif cmd[0] == "/clear":
                chat_history = []
                print("  Chat history cleared.")
                continue
            elif cmd[0] == "/lang":
                if len(cmd) > 1 and cmd[1] in ("sanskrit", "hindi", "english", "all"):
                    lang_filter = None if cmd[1] == "all" else cmd[1]
                    print(f"  Language filter: {lang_filter or 'all'}")
                else:
                    print(f"  Current: {lang_filter or 'all'}. Use: /lang [sanskrit|hindi|english|all]")
                continue
            elif cmd[0] == "/topk":
                if len(cmd) > 1 and cmd[1].isdigit():
                    top_k = int(cmd[1])
                    print(f"  Top-K: {top_k}")
                else:
                    print(f"  Current: {top_k}. Use: /topk [N]")
                continue
            elif cmd[0] == "/sources":
                if last_sources:
                    print("\n  Last query sources:")
                    for i, s in enumerate(last_sources, 1):
                        print(f"  [{i}] {s['source']} ({s['language']}) — {s['category']}")
                        print(f"      {s['text'][:200]}...")
                else:
                    print("  No sources yet.")
                continue
            elif cmd[0] == "/search":
                search_query = " ".join(cmd[1:])
                if search_query:
                    results = search_and_retrieve(search_query, embed_model, indexes, docs_store,
                                                  lang_filter=lang_filter, top_k=top_k)
                    print(f"\n  Search results for: {search_query}")
                    for i, r in enumerate(results, 1):
                        print(f"\n  [{i}] {r['source']} ({r['language']})")
                        print(f"      {r['text'][:200]}...")
                else:
                    print("  Usage: /search [query]")
                continue
            elif cmd[0] == "/help":
                print("""
  Commands:
    /lang [sanskrit|hindi|english|all]  - Set language filter
    /topk [N]                           - Set chunks to retrieve
    /sources                            - Show last answer's sources
    /search [query]                     - Search without LLM
    /clear                              - Clear chat history
    /quit                               - Exit""")
                continue
            else:
                print(f"  Unknown command: {cmd[0]}. Type /help")
                continue

        # RAG Pipeline
        t_total = time.time()

        # 1. Search
        retrieved = search_and_retrieve(query, embed_model, indexes, docs_store,
                                        lang_filter=lang_filter, top_k=top_k)
        last_sources = retrieved

        # 2. Generate
        answer, stats = generate_rag_answer(query, retrieved, chat_history, llm_model, llm_tokenizer)

        total_time = time.time() - t_total

        # 3. Display
        print(f"\n  Brahm AI: {answer}")
        print(f"\n  [{stats['tokens']} tokens | {stats['time']:.1f}s | {stats['tok_per_sec']:.0f} tok/s | "
              f"{len(retrieved)} sources | /sources for details]")

        # 4. Save to history
        chat_history.append({"user": query, "assistant": answer})


# ============================================================
# MAIN
# ============================================================
def main():
    indexes, docs_store, embed_model, llm_model, llm_tokenizer = load_components()
    chat_loop(indexes, docs_store, embed_model, llm_model, llm_tokenizer)


if __name__ == "__main__":
    main()
