# Brahm AI - Model Training Architecture
# Focus: Model ready karo, modular rakho, future upgrade easy ho

# ============================================================
# DESIGN PRINCIPLE: Har cheez alag module me. Ek cheez upgrade
# karo to baaki kuch na tute. Speed + Accuracy + Understanding.
# ============================================================


## Project Structure
```
brahm-ai/
│
├── config/
│   ├── model_config.yaml          # LLM settings (swap model here only)
│   ├── embedding_config.yaml      # Embedding model settings (swap here only)
│   ├── search_config.yaml         # FAISS, BM25, rerank settings
│   └── ingestion_config.yaml      # Chunking, OCR, language settings
│
├── data/
│   ├── raw/                       # Original books (NEVER modify)
│   │   ├── sanskrit/              # Sanskrit texts
│   │   ├── hindi/                 # Hindi texts
│   │   ├── english/               # English texts
│   │   └── mixed/                 # Multi-language texts
│   │
│   ├── processed/                 # Extracted text from PDFs
│   │   ├── sanskrit/
│   │   ├── hindi/
│   │   ├── english/
│   │   └── mixed/
│   │
│   ├── chunks/                    # Chunked text with metadata (JSON)
│   │   └── {book_id}_chunks.jsonl
│   │
│   ├── embeddings/                # Saved embeddings (numpy arrays)
│   │   └── {book_id}_embeddings.npy
│   │
│   └── finetune/                  # Fine-tuning datasets
│       ├── raw_pairs/             # Raw instruction-output pairs
│       └── formatted/             # Final JSONL for training
│
├── indexes/                       # Vector indexes (separate per language/category)
│   ├── master.index               # Combined FAISS index
│   ├── sanskrit.index             # Sanskrit-only index
│   ├── hindi.index                # Hindi-only index
│   ├── english.index              # English-only index
│   └── metadata.db                # SQLite - chunk metadata mapping
│
├── models/                        # Downloaded model weights (gitignored)
│   ├── llm/                       # Qwen2.5-7B-Instruct (or future model)
│   └── embedding/                 # paraphrase-multilingual-MiniLM-L12-v2
│
├── src/
│   ├── ingestion/                 # DATA LAYER - text extraction + chunking
│   │   ├── __init__.py
│   │   ├── pdf_extractor.py       # pypdf (text) + Tesseract (Indic) + PaddleOCR (English) + auto-router
│   │   ├── text_extractor.py      # TXT, MD, HTML, EPUB
│   │   ├── json_extractor.py      # JSON, JSONL, CSV
│   │   ├── chunker.py             # Smart chunking with overlap + metadata
│   │   ├── normalizer.py          # Sanskrit/Devanagari/IAST normalization
│   │   └── pipeline.py            # Full ingestion orchestrator
│   │
│   ├── embedding/                 # EMBEDDING LAYER - pluggable
│   │   ├── __init__.py
│   │   ├── base.py                # Abstract base class (interface)
│   │   ├── minilm.py              # Current: multilingual-MiniLM-L12-v2
│   │   ├── labse.py               # Future: LaBSE (768 dim)
│   │   └── indic_bert.py          # Future: ai4bharat Indic BERT
│   │
│   ├── indexing/                   # INDEX LAYER - FAISS management
│   │   ├── __init__.py
│   │   ├── faiss_manager.py       # Build, save, load, add to FAISS
│   │   ├── bm25_manager.py        # BM25 keyword index
│   │   └── metadata_store.py      # SQLite metadata (chunk_id -> source, page, lang)
│   │
│   ├── retrieval/                  # RETRIEVAL LAYER - search pipeline
│   │   ├── __init__.py
│   │   ├── hybrid_search.py       # FAISS + BM25 + RRF fusion
│   │   ├── reranker.py            # Cross-encoder reranking
│   │   └── context_builder.py     # Build LLM context from top chunks
│   │
│   ├── llm/                        # LLM LAYER - pluggable
│   │   ├── __init__.py
│   │   ├── base.py                 # Abstract base class (interface)
│   │   ├── qwen.py                 # Current: Qwen2.5-7B-Instruct
│   │   ├── qwen3.py                # Future: Qwen3-7B
│   │   └── loader.py               # Model loader (reads config, loads correct model)
│   │
│   ├── chat/                        # CHAT LAYER - conversation management
│   │   ├── __init__.py
│   │   ├── session.py              # Chat session (history, context window)
│   │   ├── memory.py               # Conversation memory (last N turns)
│   │   └── router.py               # Route query: RAG vs Calculation vs General
│   │
│   ├── special/                     # SPECIAL MODULES - isolated calculations
│   │   ├── __init__.py
│   │   ├── kundali.py              # Birth chart (pyswisseph/kerykeion)
│   │   ├── panchang.py             # Hindu calendar (ephem)
│   │   ├── grahan.py               # Eclipse calculations (ephem)
│   │   └── jyotish.py              # Vedic astrology combined
│   │
│   └── finetune/                    # FINE-TUNING LAYER - optional
│       ├── __init__.py
│       ├── dataset_builder.py      # Build instruction/input/output pairs
│       ├── trainer.py              # QLoRA/PEFT training script
│       └── evaluator.py            # Test fine-tuned model quality
│
├── scripts/                         # One-click scripts
│   ├── 01_ingest_books.py           # Step 1: Extract + chunk all books
│   ├── 02_build_index.py            # Step 2+3: Embed + Build FAISS HNSW indexes (combined)
│   ├── 04_test_search.py            # Step 4: Test retrieval quality
│   ├── 05_test_rag.py               # Step 5: Test full RAG pipeline
│   ├── 06_chat_terminal.py          # Step 6: Interactive ChatGPT-like terminal
│   └── 07_finetune.py               # Step 7: Fine-tune (optional)
│
├── tests/                           # Quality checks
│   ├── test_ingestion.py
│   ├── test_search.py
│   ├── test_rag.py
│   └── test_special.py
│
├── requirements.txt
└── README.md
```


## WHY THIS STRUCTURE?

### Problem: Sab mixed ho jata hai
### Solution: Har layer independent hai

```
UPGRADE EXAMPLES:

1. Embedding model badalna hai?
   - Sirf config/embedding_config.yaml change karo
   - Sirf src/embedding/ me naya file add karo
   - Baaki kuch nhi badlega

2. LLM upgrade karna hai (Qwen2.5 -> Qwen3)?
   - Sirf config/model_config.yaml change karo
   - src/llm/qwen3.py already ready hai
   - Search pipeline, chunking, embeddings - kuch nhi badlega

3. FAISS se Milvus pe jaana hai?
   - Sirf src/indexing/faiss_manager.py replace karo
   - Baaki sab same rahega

4. Naya language add karna hai?
   - data/raw/tamil/ folder banao
   - Embedding model already multilingual hai
   - Bas normalizer.py me Tamil rules add karo
```


## DATA STORAGE STRATEGY

### Different data types ko alag store karo:

```
TYPE           | RAW FORMAT    | STORED AS           | INDEX
───────────────|───────────────|─────────────────────|──────────
Sanskrit texts | PDF/TXT       | chunks + metadata   | sanskrit.index
Hindi texts    | PDF/TXT       | chunks + metadata   | hindi.index
English texts  | PDF/EPUB      | chunks + metadata   | english.index
Mixed texts    | PDF           | chunks + lang tag   | master.index
Kundali data   | -             | NOT indexed (calc)  | -
Panchang data  | -             | NOT indexed (calc)  | -
Fine-tune data | JSONL         | instruction pairs   | -
```

### Metadata Store (SQLite) - har chunk ka track:
```sql
CREATE TABLE chunks (
    chunk_id TEXT PRIMARY KEY,
    book_id TEXT,
    source_file TEXT,
    page_number INTEGER,
    chunk_index INTEGER,
    language TEXT,          -- 'sanskrit', 'hindi', 'english', 'mixed'
    category TEXT,          -- 'veda', 'purana', 'upanishad', 'gita', 'jyotish', etc.
    script TEXT,            -- 'devanagari', 'iast', 'roman'
    text_preview TEXT,      -- first 100 chars
    embedding_file TEXT,    -- path to .npy file
    faiss_id INTEGER,       -- position in FAISS index
    created_at TIMESTAMP
);

CREATE TABLE books (
    book_id TEXT PRIMARY KEY,
    title TEXT,
    author TEXT,
    language TEXT,
    category TEXT,
    total_chunks INTEGER,
    file_path TEXT,
    ingested_at TIMESTAMP
);
```


## CONFIG FILES (Swap without code change)

### model_config.yaml
```yaml
llm:
  name: "Qwen/Qwen2.5-7B-Instruct"    # Change this line to upgrade
  quantization: "4bit"                   # 4bit, 8bit, or none
  max_new_tokens: 1024
  temperature: 0.7
  device_map: "auto"

  # Future: uncomment to switch
  # name: "Qwen/Qwen3-7B"
```

### embedding_config.yaml
```yaml
embedding:
  name: "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
  dimension: 384
  batch_size: 64
  normalize: true

  # Future upgrades (uncomment one):
  # name: "sentence-transformers/LaBSE"
  # dimension: 768

  # name: "ai4bharat/indic-bert"
  # dimension: 768
```

### search_config.yaml
```yaml
faiss:
  index_type: "HNSW"
  M: 32
  ef_construction: 200
  ef_search: 64

bm25:
  enabled: true
  top_k: 20

hybrid:
  faiss_top_k: 20
  bm25_top_k: 20
  rrf_k: 60               # RRF constant
  rerank_top_k: 5

reranker:
  model: "cross-encoder/ms-marco-MiniLM-L-6-v2"
  enabled: true
```

### ingestion_config.yaml
```yaml
chunking:
  chunk_size: 400          # tokens
  overlap: 50              # tokens
  min_chunk_size: 50       # skip tiny chunks

ocr:
  enabled: true
  language: "san+hin+eng"  # tesseract languages
  min_text_threshold: 100  # chars - below this, use OCR

normalization:
  normalize_devanagari: true
  normalize_iast: true
  remove_diacritics: false
```


## TRAINING PIPELINE (Step by Step)

```
STEP 1: INGEST           STEP 2: EMBED           STEP 3: INDEX
┌─────────────┐         ┌──────────────┐        ┌──────────────┐
│ PDF/TXT/... │──────>  │   Chunker    │─────>  │  Embeddings  │
│ (raw books) │  extract│ (400 tok,    │ chunk  │  (MiniLM     │
│             │  + OCR  │  50 overlap) │  +meta │   384 dim)   │
└─────────────┘         └──────────────┘        └──────┬───────┘
                                                       │
                              ┌─────────────────────────┤
                              v                         v
                        ┌──────────┐            ┌──────────────┐
                        │  SQLite  │            │ FAISS HNSW   │
                        │ metadata │            │ + BM25 index │
                        └──────────┘            └──────────────┘

STEP 4: SEARCH (at query time)
┌─────────┐    ┌────────────┐    ┌─────────┐    ┌──────────┐    ┌─────────┐
│  Query  │───>│ FAISS+BM25 │───>│   RRF   │───>│ Reranker │───>│ Top 5   │
│         │    │  (top 20   │    │ fusion  │    │ CrossEnc │    │ chunks  │
│         │    │   each)    │    │         │    │          │    │         │
└─────────┘    └────────────┘    └─────────┘    └──────────┘    └────┬────┘
                                                                     │
STEP 5: GENERATE                                                     │
┌──────────────────────────────────────────────────────────────┐     │
│  Qwen LLM                                                    │<────┘
│  System: "You are Brahm AI, expert in Vedic knowledge..."    │
│  Context: [top 5 chunks]                                     │
│  Chat History: [last N turns]                                │
│  Query: [user question]                                      │
│  ──────────────────────────────────>  Answer                 │
└──────────────────────────────────────────────────────────────┘

STEP 6: SPECIAL (if query is calculation-type)
┌─────────┐    ┌─────────┐    ┌──────────────┐
│  Query  │───>│ Router  │───>│ Kundali /    │──> Direct calculation
│ "meri   │    │ detects │    │ Panchang /   │    (no RAG needed)
│ kundali"│    │ calc    │    │ Grahan       │
└─────────┘    └─────────┘    └──────────────┘
```


## QUERY ROUTER - Smart Routing

```
User Query
    │
    v
┌──────────────────────┐
│   ROUTER (router.py) │
│                      │
│ Keywords detect:     │
│ "kundali","janam     │──── Calculation ──> special/kundali.py
│  patri","birth chart"│
│                      │
│ "panchang","tithi",  │──── Calculation ──> special/panchang.py
│  "nakshatra"         │
│                      │
│ "grahan","eclipse"   │──── Calculation ──> special/grahan.py
│                      │
│ Everything else      │──── RAG Pipeline ──> retrieval + LLM
└──────────────────────┘
```


## CHAT MEMORY (ChatGPT-like)

```python
# session.py - Har user ki chat history maintain karo

class ChatSession:
    - session_id
    - history: list of {role, content}  # last 10 turns
    - context_window: 4096 tokens       # kitna history LLM ko dena hai

# Flow:
# 1. User asks question
# 2. Router decides: RAG or Calculation
# 3. If RAG: retrieve chunks + add chat history + generate
# 4. Save turn to history
# 5. Next question me purani history bhi jaayegi

# Prompt template:
SYSTEM = """You are Brahm AI, an expert in Sanatan Dharma, Vedic scriptures,
Sanskrit texts, Jyotish, and Hindu philosophy. Answer based on the provided
context from authentic scriptures. If context is insufficient, say so honestly.
Respond in the same language as the user's question."""
```


## EXECUTION ORDER (VM pe yeh sequence follow karo)

```bash
# 1. Setup
cd ~/books
source ~/ai-env/bin/activate

# 2. Run pipeline step by step
python3 scripts/01_ingest_books.py      # Extract text, chunk, save JSON per book
python3 scripts/02_build_index.py       # Embed all chunks + Build FAISS HNSW indexes (combined)
python3 scripts/04_test_search.py       # Test FAISS search quality (no LLM needed)
python3 scripts/05_test_rag.py          # Test full RAG pipeline (Search + Qwen LLM)
python3 scripts/06_chat_terminal.py     # Interactive ChatGPT-like terminal

# Optional: Fine-tune
python3 scripts/07_finetune.py
```

### Current Pipeline Status (2026-03-14)
```
Script                    | Status | Notes
--------------------------|--------|------
01_ingest_books.py        | DONE   | 366 GCloud JSONs + 146K AWS datasets
02_build_index.py         | DONE   | 1.1M chunks → 4 FAISS HNSW indexes (33.7 min)
04_test_search.py         | DONE   | FAISS semantic search working, quality mixed
05_test_rag.py            | DONE   | Full RAG: Search + Retrieve + Qwen Generate (15 tok/s)
06_chat_terminal.py       | DONE   | Interactive chat with memory, /lang /search /sources
```


## SPEED + ACCURACY OPTIMIZATIONS

```
SPEED:
- 4-bit quantization (bitsandbytes) = 4x less VRAM, fast inference
- FAISS HNSW = O(log n) search, not O(n)
- BM25 pre-computed = instant keyword match
- Batch embedding generation (64 chunks at a time)
- Semantic cache (Redis) = repeat queries instant

ACCURACY:
- Hybrid search (semantic + keyword) catches both meaning and exact terms
- Cross-encoder reranking = much better relevance than raw FAISS scores
- Language-specific indexes = no cross-language noise
- Sanskrit normalization = consistent matching despite script variations
- Metadata filtering = search within specific book/category/language

UNDERSTANDING:
- 400 token chunks with 50 overlap = enough context per chunk
- Top 5 chunks to LLM = rich context without noise
- Chat history = follow-up questions understand previous context
- System prompt = model knows its domain expertise
```
