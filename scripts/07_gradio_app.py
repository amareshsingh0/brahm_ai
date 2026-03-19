python3 << 'WRITE'
script = r'''#!/usr/bin/env python3
"""Brahm AI - Gradio Web Interface"""

import os, json, time, threading, numpy as np, gradio as gr

BASE_DIR    = os.path.expanduser("~/books")
INDEXES_DIR = os.path.join(BASE_DIR, "indexes")
DOCS_PATH   = os.path.join(INDEXES_DIR, "documents.jsonl")
LOG_DIR     = os.path.join(BASE_DIR, "data/processed/logs")
EMBED_MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
LLM_MODEL   = "Qwen/Qwen2.5-7B-Instruct"
FAISS_K, BM25_K, FINAL_K = 20, 20, 5
MAX_HISTORY = 4

SYSTEM_PROMPT = """You are Brahm AI, expert in Sanatan Dharma, Vedic scriptures, Sanskrit, Jyotish, Hindu philosophy.
STRICT RULES:
1. Answer ONLY from the provided context chunks. Do NOT use your training knowledge.
2. NEVER generate or quote Sanskrit/Hindi verses not word-for-word in the context.
3. If context lacks the answer, say: "I could not find relevant context for this question."
4. Always cite source name.
5. Reply in same language as the question.
6. NEVER respond in Chinese. Use only English, Hindi, or Sanskrit.
7. Max 300 words unless asked for detail."""

# Global state
G = {}

def load_all():
    import faiss
    from sentence_transformers import SentenceTransformer
    from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
    from rank_bm25 import BM25Okapi
    from sentence_transformers import CrossEncoder
    import torch

    print("Loading FAISS indexes...")
    G["indexes"] = {}
    for lang in ["sanskrit", "hindi", "english"]:
        p = os.path.join(INDEXES_DIR, f"{lang}.index")
        if os.path.exists(p):
            G["indexes"][lang] = faiss.read_index(p)
            print(f"  {lang}: {G['indexes'][lang].ntotal}")

    print("Loading documents + BM25...")
    docs = {}
    lang_docs = {"sanskrit": [], "hindi": [], "english": []}
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
    G["bm25"] = BM25Okapi(all_texts)
    print(f"  {len(docs)} docs, BM25 ready")

    print("Loading embedding model...")
    G["embed"] = SentenceTransformer(EMBED_MODEL)

    print("Loading cross-encoder...")
    G["reranker"] = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")

    print("Loading Qwen LLM...")
    bnb = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_compute_dtype=torch.float16, bnb_4bit_quant_type="nf4")
    G["tokenizer"] = AutoTokenizer.from_pretrained(LLM_MODEL)
    G["llm"] = AutoModelForCausalLM.from_pretrained(LLM_MODEL, quantization_config=bnb, device_map="auto")
    vram = torch.cuda.memory_allocated() / 1e9
    print(f"  Loaded, VRAM: {vram:.1f} GB")

    # OCR quality
    G["ocr_quality"] = {}
    try:
        cr = json.load(open(os.path.join(LOG_DIR, "correction_report.json")))
        for b in cr.get("book_scores", []):
            G["ocr_quality"][b["source"]] = min(1.0, b["avg_score"] / 0.80)
        print(f"  OCR quality: {len(G['ocr_quality'])} files")
    except: pass

    print("All components loaded!")


def src_boost(src):
    import re
    s = src.lower()
    if any(s.startswith(p) for p in ["bhagavadgita/", "sarit/", "gretil/", "suttacentral/"]):
        return 1.1
    if s.startswith("openphilology/"): return 1.0
    if any(s.startswith(p) for p in ["wikisource/", "wikipedia_"]): return 0.95
    if "sanskrit_mono" in s or "hf/mono" in s: return 0.30
    if s.startswith("hf/"): return 0.85
    if s.endswith(".pdf") and re.search(r"\d{4}", s): return 0.75
    return 0.90


def hybrid_search(query, lang_filter=None):
    indexes, docs = G["indexes"], G["docs"]
    all_keys, bm25 = G["all_keys"], G["bm25"]
    embed, reranker = G["embed"], G["reranker"]
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
        if lang_filter and lang_filter != "all" and key[0] != lang_filter: continue
        if bm25_scores[idx] > 0:
            bm25_res[key] = {"bm25_rank": rank}

    k = 60
    rrf = {}
    for key in set(faiss_res) | set(bm25_res):
        score = 0
        if key in faiss_res: score += 1.0 / (k + faiss_res[key]["faiss_rank"])
        if key in bm25_res:  score += 1.0 / (k + bm25_res[key]["bm25_rank"])
        rrf[key] = score

    top_keys = sorted(rrf, key=lambda x: rrf[x], reverse=True)[:FAISS_K]
    candidates = []
    for key in top_keys:
        doc = docs.get(key)
        if doc:
            candidates.append({"key": key, "text": doc.get("text", ""), "source": doc.get("source", "?"),
                                "language": key[0], "rrf_score": rrf[key]})

    if candidates:
        pairs = [(query, c["text"][:512]) for c in candidates]
        scores = reranker.predict(pairs)
        for i, c in enumerate(candidates):
            c["rerank_score"] = float(scores[i])
        # Apply boosts
        for c in candidates:
            q = ocr_quality.get(c["source"].split("/")[-1], ocr_quality.get(c["source"], 1.0))
            c["rerank_score"] *= src_boost(c["source"]) * q
        candidates.sort(key=lambda x: x["rerank_score"], reverse=True)

    return candidates[:FINAL_K]


def generate_stream(query, retrieved, history):
    import torch, re as _re
    from transformers import TextIteratorStreamer
    def _clean(t): return _re.sub(r"Transliteration:[^\n]*\n?", "", t).strip()

    ctx = "\n\n---\n\n".join(
        f"[Source {i}: {d['source']} ({d['language']})]\n{_clean(d['text'][:1000])}"
        for i, d in enumerate(retrieved, 1)
    )
    msgs = [{"role": "system", "content": SYSTEM_PROMPT}]
    for u, a in history[-MAX_HISTORY:]:
        msgs.append({"role": "user", "content": u})
        msgs.append({"role": "assistant", "content": a})
    msgs.append({"role": "user", "content": f"Context:\n{ctx}\n\n---\nQuestion: {query}\n\nAnswer strictly from context above."})

    tok = G["tokenizer"]
    llm = G["llm"]
    text = tok.apply_chat_template(msgs, tokenize=False, add_generation_prompt=True)
    inputs = tok(text, return_tensors="pt").to(llm.device)
    streamer = TextIteratorStreamer(tok, skip_prompt=True, skip_special_tokens=True)
    gen_kwargs = dict(**inputs, max_new_tokens=768, temperature=0.3, top_p=0.9,
                      do_sample=True, repetition_penalty=1.3, streamer=streamer)
    threading.Thread(target=llm.generate, kwargs=gen_kwargs).start()
    for token in streamer:
        yield token


def respond(message, chat_history, lang_filter):
    if not message.strip():
        yield chat_history, ""
        return
    lang = None if lang_filter == "all" else lang_filter
    retrieved = hybrid_search(message, lang)
    sources_txt = "\n".join(f"[{i+1}] {r['source']} ({r['language']})" for i, r in enumerate(retrieved))

    partial = ""
    chat_history = chat_history + [[message, ""]]
    history_for_llm = [[u, a] for u, a in chat_history[:-1] if a]

    for token in generate_stream(message, retrieved, history_for_llm):
        partial += token
        chat_history[-1][1] = partial
        yield chat_history, sources_txt


# ── Gradio UI ────────────────────────────────────────────
with gr.Blocks(title="Brahm AI", theme=gr.themes.Soft()) as demo:
    gr.Markdown("# 🙏 Brahm AI — Vedic Knowledge Assistant\n*Search 1.1M+ chunks from Vedas, Upanishads, Gita, Puranas, Jyotish*")

    with gr.Row():
        with gr.Column(scale=3):
            chatbot = gr.Chatbot(height=480, label="Chat")
            with gr.Row():
                msg_box = gr.Textbox(placeholder="Ask about Dharma, Karma, Yoga, Gita, Jyotish...",
                                     show_label=False, scale=4)
                send_btn = gr.Button("Send", variant="primary", scale=1)
            with gr.Row():
                lang_dd = gr.Dropdown(["all", "sanskrit", "hindi", "english"],
                                      value="all", label="Language Filter", scale=1)
                clear_btn = gr.Button("Clear Chat", scale=1)
        with gr.Column(scale=1):
            sources_box = gr.Textbox(label="Retrieved Sources", lines=12, interactive=False)

    send_btn.click(respond, [msg_box, chatbot, lang_dd], [chatbot, sources_box])
    msg_box.submit(respond, [msg_box, chatbot, lang_dd], [chatbot, sources_box])
    clear_btn.click(lambda: ([], ""), outputs=[chatbot, sources_box])

if __name__ == "__main__":
    load_all()
    demo.launch(server_name="0.0.0.0", server_port=7860, share=True, show_error=True)
'''

with open('/home/amareshsingh2005/books/scripts/07_gradio_app.py', 'w') as f:
    f.write(script)
print("Created: scripts/07_gradio_app.py")
WRITE
