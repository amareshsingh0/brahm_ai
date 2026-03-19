# Brahm AI - Progress Tracker
# Last Updated: 2026-03-19


## PHASE 1: PLANNING & RESEARCH [DONE]
- [x] Read all 4 research PDFs (Master Blueprint + 3 Training guides)
- [x] Understand full architecture (LLM, Embedding, FAISS, Hybrid Search, RAG)
- [x] Create memory files for quick reference
- [x] Design modular architecture (ARCHITECTURE.md)
- [x] Define folder structure (config/, data/, src/, scripts/, indexes/)
- [x] Define config-driven approach (YAML files for model swap)
- [x] Define data storage strategy (language-wise separate indexes)
- [x] Define pipeline steps (01_ingest -> 02_embed -> 03_index -> 04_test -> 05_rag -> 06_chat)


## PHASE 2: VM SETUP & CLEANUP [DONE]
- [x] Google Cloud VM running (g2-standard-32, NVIDIA L4 24GB GPU)
- [x] Python venv `ai-env` active
- [x] Folder structure created on VM
- [x] All libraries installed (pyswisseph, skyfield, ephem, kerykeion, jplephem, rank_bm25, cryptography)
- [x] OCR/NLP tools installed (paddleocr, opencv, layoutparser, langdetect, indic-nlp-library, aksharamukha, datasketch)
- [x] Duplicate PDF scan done (20 dupes found)
- [x] 0-byte corrupted PDFs cleaned (6 files in vedic_astrology/)
- [x] Duplicate PDFs removed (~14 files, ~1.5GB freed)
- [x] PDF library organized: raw/mixed/ (2.8G), raw/sanskrit/ (267M), raw/vedic_astrology/ (384M), raw/jyotisha_astronomy/ (2.2G)


## PHASE 3: DATA COLLECTION [DONE - 162/162 = 100%]

### Previously Downloaded (2026-03-06 to 03-08)
- [x] Hugging Face setup on VM
- [x] Dataset: Modotte/Bhagwat-Gita-Infinity (36.5 MB, 18 chapters, 719 sloks, JSON format)
- [x] Dataset: VedAstro_PlanetData (491 CSV files, planetary ephemeris)
- [x] Dataset: VedAstro_15000_BirthData (famous people birth charts)
- [x] Dataset: VedAstro_15000_MarriageData (compatibility data)
- [x] Dataset: Celestial_SpiritualAI (spiritual Q&A conversations)
- [x] PDF: KamikaAgama_PurvaPada_Part1.pdf (Google Drive upload)
- [x] PDFs: AgamaAcademy (Kamikagama, Yogaja, Chintyagama, PurvaKarana 5 parts)
- [x] PDF: InternetArchive Agama_Tripitaka_Vol02
- [x] NASA JPL DE441 Part1 + Part2 (3 GB, extreme accuracy ephemeris)
- [x] Swiss Ephemeris files (seas, sepl, semo)
- [x] GitHub: VedAstro repo cloned
- [x] Data format checked (JSON with chapter/verse/slok/commentaries)

### New Downloads (2026-03-10) — 7 Sources Researched, 3 Active
Sources Researched:
- [x] GRETIL — 403 Forbidden (blocked wget from VM IP)
- [x] Sacred-Texts.com — Cloudflare blocked wget
- [x] SARIT GitHub — **CLONED** (85 XML files, 204MB)
- [x] SuttaCentral bilara-data — **CLONED** (full Pali Canon, 306MB, JSON)
- [x] Archive.org — **31 items, 62 PDFs, 6.1GB downloaded**
- [x] HuggingFace ai4bharat/sangraha — Researched (251B tokens, web corpus, not curated)
- [x] Muktabodha — Manual only, no bulk download

Archive.org Downloads (6.1GB total):
- [x] Rigveda (143MB), Atharvaveda 2 vols (56MB), 4-Veda set (24MB)
- [x] 108 Upanishads (50MB), 11 Upanishads Shankara Bhashya (1.5GB)
- [x] Narada Purana 2 parts (2.3GB), Kurma Purana (566MB)
- [x] Bhagavatam (34MB), Vishnu Purana Hindi (507MB), Shiva Purana (59MB)
- [x] Mahabharata Ganguli (23MB), Arthashastra (2MB)
- [x] Charaka Samhita 2 vols (221MB), Sushruta Samhita 3 vols (96MB)
- [x] Brahma Sutra Bhashya (148MB), Vivekachudamani (58MB)
- [x] Tantraloka 2 vols (340MB), Vijnana Bhairava (640KB)
- [x] Yoga Sutras (21MB), Hatha Yoga Pradipika (1.6MB), Gheranda Samhita (2.2MB)
- [x] Ashtadhyayi Panini (29MB), Dhammapada (592KB)

Pre-existing Downloads Discovered on VM (11 books already present):
- [x] Manu Smriti, Yajnavalkya Smriti (smriti_dharma/)
- [x] Garuda Purana, Agni Purana, Markandeya Purana, Matsya Purana (puranas/)
- [x] Shiva Samhita (yoga/)
- [x] Mahanirvana Tantra, Kularnava Tantra (tantra/)
- [x] Acharanga Sutra, Tattvartha Sutra (jaina/)

Archive.org Cleanup (2026-03-10):
- [x] 12GB junk deleted (non-PDF files, metadata, thumbs)
- [x] _text.pdf duplicates deleted from all items
- [x] Remaining non-PDF: some .epub and .djvu.txt files (harmless, pipeline skips)

PDFs Organized into 11 Category Folders (2026-03-10):
- [x] Flat archive_org/ → 11 categorized folders: vedas/ puranas/ smriti_dharma/ itihasa/ yoga/ ayurveda/ tantra/ bauddha/ jaina/ darshana/ grammar_literature/
- [x] 63 PDFs across 11 categories (vedas 12, puranas 16, smriti_dharma 7, itihasa 1, yoga 5, ayurveda 5, tantra 7, bauddha 1, jaina 5, darshana 3, grammar_literature 1)

Batch 2 Downloads (2026-03-10, second round):
- [x] Puranas: Linga, Padma Part2+3, Bhavishya, DeviBhagavata, Brahma, BrahmaVaivarta, Varaha, Vamana, Brahmanda (~3.3GB)
- [x] Smritis: Narada Smriti, Parasara Smriti, Dharmasutras 4-in-1 (~563MB)
- [x] Ayurveda: Ashtanga Hridayam, Madhava Nidana, Bhavaprakasha, Sharangadhara Samhita (~593MB)
- [x] Tantra: Shiva Sutras, Pratyabhijnahridayam, Spanda Karikas, Tripura Rahasya 1+2, Soundarya Lahari (~195MB)
- [x] Yoga: Yoga Yajnavalkya (7.4MB)
- [x] Itihasa: Ashtavakra Gita (1.2MB)
- [x] Darshana: Samkhya Karika, Tarka Sangraha (~41MB)
- [x] Jain: Kalpa Sutra, Uttaradhyayana Sutra, Samayasara (~339MB)
- [x] Grammar: Natyashastra, Meghaduta (~330MB)
- [x] Vedas: Shatapatha Brahmana, Aitareya Brahmana (~349MB)
- [x] Buddhist: Buddhacharita (37MB)
- [x] Non-PDF junk + _text.pdf duplicates cleaned from all new downloads

Batch 3 Downloads (2026-03-10, third round):
- [x] Smritis: Vishnu Smriti, Nitisara, Dharmasindhu (~658MB)
- [x] Ayurveda: Ashtanga Sangraha, Kashyapa Samhita, Yoga Ratnakara, Dhanvantari Nighantu, Chakradatta (~725MB)
- [x] Tantra: Devi Mahatmya, Mantra Mahodadhi, Yogini Hridaya (~1.1GB)
- [x] Darshana: Panchadashi, Mimamsa Sutra, Drig Drishya Viveka (~162MB)
- [x] Grammar: Panchatantra, Mahabhashya, Kathasaritsagara, Shakuntalam, Raghuvamsha, Siddhanta Kaumudi (~924MB)
- [x] Itihasa: Yoga Vasistha 1+2, Adhyatma Ramayana, Gita Rahasya, Avadhuta Gita (~463MB)
- [x] Yoga: Shiva Svarodaya (52MB)
- [x] Jain: Niyamasara, Pravachanasara, Panchastikaya, Dasavaikalika Sutra (~326MB)
- [x] Buddhist: Jataka Tales, Lankavatara+Heart+Diamond Sutras, Lalitavistara, Vimalakirti Sutra, Milindapanha (~165MB)
- [x] Vedas: Taittiriya Brahmana, Rigveda Max Muller (~76MB)
- [x] Non-PDF junk + _text.pdf duplicates cleaned

Batch 4 Downloads (2026-03-10, fourth round — rare/modern editions):
- [x] Puranas: Harivamsa Purana (564MB), Kalika Purana (126MB)
- [x] Smritis: Nirnayasindhu (147MB)
- [x] Itihasa: Gita Shankaracharya Bhashya (30MB)
- [x] Yoga: Goraksha Samhita (145MB), Hatha Ratnavali (78MB)
- [x] Ayurveda: Rasaratna Samuccaya (146MB), Harita Samhita (391MB), Raj Nighantu (394MB)
- [x] Tantra: Netra Tantra KSTS (170MB), Svacchanda Tantra 1+2 KSTS (398MB), Nityashodashikarnava (203MB), Tantrasara Abhinavagupta (33MB)
- [x] Darshana: Sri Bhashya Ramanuja (430MB), Nyaya Sutras (11MB), Upadesa Sahasri (88MB)
- [x] Jain: Gommathasara (58MB), Adipurana Jinasena (102MB)
- [x] Buddhist: Ashtasahasrika Prajnaparamita (18MB), Mulamadhyamakakarika (244KB)
- [x] Grammar: Vakyapadiya (172MB)
- [x] Non-PDF junk + _text.pdf duplicates: CLEANED (334 PDFs, 28GB final)

**Grand Total on VM: 29 GB raw data, 334 PDFs in 15 subfolders + SARIT 204M + SuttaCentral 306M**
Batch 5 Downloads (2026-03-10, fifth round — final 9 "rare" books found via Google):
- [x] Yoga: Khecarividya Mallinson, 20 Yoga Upanishads (Ayyangar 1938), Vasistha Samhita Yoga Kanda (Kaivalyadhama 2009, 86MB)
- [x] Ayurveda: Rasaratnakara Rasayanakhanda (1939 edition)
- [x] Tantra: Rudrayamala Tantram + Rudrayamala Uttara Tantra (Dr Sudhakar Malaviya)
- [x] Buddhist: Abhidharmakosha Bhashya Vol 1 (Poussin/Pruden English)
- [x] Jain: Yoga Shastra Hemachandra (DLI 2015 edition)
- [x] Darshana: Vaisheshika Sutras (Nandalal Sinha), Madhva Brahma Sutra Bhashya (Jagannatha Dipika, 130MB)
- [x] Non-PDF junk + _text.pdf duplicates: CLEANED (334 PDFs, 29GB final)

**Grand Total on VM: 29 GB raw data, 334 PDFs in 15 subfolders + SARIT 204M + SuttaCentral 306M**
**Coverage: 162/162 wish list books (100%) + SARIT 85 texts + SuttaCentral full Pali Canon**
All 162 books from wish list downloaded. PHASE 3 DATA COLLECTION COMPLETE.


## PHASE 4: CONFIG FILES [NOT STARTED]
- [ ] Create config/model_config.yaml (Qwen2.5-7B-Instruct, 4-bit)
- [ ] Create config/embedding_config.yaml (multilingual-MiniLM-L12-v2, 384 dim)
- [ ] Create config/search_config.yaml (FAISS HNSW M=32, BM25, Reranker)
- [ ] Create config/ingestion_config.yaml (chunk 400 tokens, 50 overlap)


## PHASE 5: INGESTION PIPELINE [IN PROGRESS - 363/394 GCloud JSONs + 146K AWS files]
- [x] full_pipeline_test.py (Gita: load → chunk → embed → FAISS → search → source attribution)
- [x] smart_search.py (Hybrid search + Cross-Encoder rerank + Qwen RAG answer)
- [x] ingest_pdf.py (PDF extract → chunk → embed → add to FAISS)
- [x] Tested: 16,518 chunks from Gita, working search with source citation
- [x] Tested: Qwen RAG answers with chapter/verse/author attribution
Modular Pipeline (CREATED 2026-03-09):
- [x] src/ingestion/__init__.py (module init)
- [x] src/ingestion/pdf_extractor.py (pypdf text + PaddleOCR v2.9.1 scanned + hybrid mode)
- [x] src/ingestion/script_detector.py (Unicode range detection: Devanagari/Tamil/Telugu/Kannada/Malayalam/Bengali/Latin)
- [x] src/ingestion/normalizer.py (Unicode NFC, Devanagari OCR fixes, danda cleanup, transliteration via aksharamukha)
- [x] src/ingestion/chunker.py (400 tokens, 50 overlap, fast word-split, JSON dataset chunker)
- [x] src/ingestion/pipeline.py (Master orchestrator: discover → detect → extract → normalize → chunk → save JSON)
- [x] scripts/01_ingest_books.py (CLI: --single, --category, --list-only, --force-ocr, --max-pages)
Pipeline Deployed & Running on VM (2026-03-09):
- [x] Pipeline uploaded to VM and tested
- [x] PaddleOCR v2.9.1 working (downgraded from v3.4/v5 due to API breaks)
- [x] poppler-utils installed (required by pdf2image)
- [x] Sanskrit category: 5 text PDFs processed, 1 scanned (sivagnana-bodha) OCR'd successfully
- [x] 90/121 PDFs processed across all categories, 0 failed
- [x] agama_mixed: 46+ processed | jyotisha: 8+ processed | vedic_astrology: 10+ processed | sanskrit: 6 processed
- [x] Chunker fixed: simplified fast version (old verse-boundary search stuck on 1000+ page books)
- [x] Pipeline saves stats after each book (resume-friendly)
- [x] Local code files synced with VM versions
Pipeline Patch & New Category Processing (2026-03-10):
- [x] pipeline.py patched on VM: FOLDER_CATEGORY_MAP expanded with 11 new categories
- [x] Category detection fixed: `parts[0]` instead of `str(rel_path.parent)` for first path component
- [x] 63 new PDFs detected across 11 categories
- [x] 8-window tmux parallel processing launched for all new categories
- [x] BATCH A completed: vedas, puranas, jaina, tantra, ayurveda, darshana → 48 JSONs
- [x] Total pre-existing + BATCH A: 275 JSONs
OCR Upgrade & BATCH B (2026-03-12):
- [x] Surya OCR ABANDONED (broken API v0.17.1, torch conflicts, transformers incompatibility)
- [x] Tesseract OCR deployed as primary Indic OCR (apt install tesseract-ocr-hin tesseract-ocr-san)
- [x] pdf_extractor.py rewritten: Tesseract (Devanagari 90-95%) + PaddleOCR (English fallback)
- [x] ocr_engine parameter bug fixed (wasn't passing from CLI → pipeline → extractor, 3 levels)
- [x] CLI choices updated: auto|tesseract|paddle (was auto|surya|paddle)
- [x] OCR_CORRECTION.md created: 3-layer hybrid plan (Dict + SymSpell + Google Cloud Vision)
- [x] BATCH B started: 5 processes (grammar_literature, smriti_dharma, bauddha, panchang, special_jyotish)
- [ ] BATCH B+ pending: yoga (2 remaining), samudrika (5), mixed (88 — category mapping issue?)
- [ ] BATCH C pending: 8 Phase 3.5 categories (nakshatra_jyotish, muhurta_prashna, vastu, etc.)
Fixes Applied (2026-03-09 to 2026-03-12):
- [x] PaddleOCR v3.4 → v2.9.1 (fix: PDX reinit error, langchain imports, show_log, .predict() API)
- [x] paddlex langchain imports patched (langchain.docstore → langchain_core.documents)
- [x] Singleton OCR pattern (_ocr_instance global) to avoid PDX reinit
- [x] poppler-utils installed for pdf2image
- [x] numpy downgraded to 1.24.4 (paddleocr v2.9.1 requires numpy<2.0)
- [x] VM scripts path: ~/books/scripts/ (must cd ~/books before running)
Remaining:
- [ ] Fix mixed category (0/88 — FOLDER_CATEGORY_MAP or path issue?)
- [ ] Fix darshana book 1 (960 pages text PDF misclassified as scanned)
- [ ] Complete BATCH B + BATCH C (all 394 PDFs)
- [ ] Run OCR correction pipeline (01b_correct + 01c_gcv_reocr)
- [ ] src/ingestion/xml_extractor.py (SARIT TEI-XML Sanskrit texts)
- [ ] src/ingestion/json_extractor.py (JSON, JSONL, CSV - for SuttaCentral)
- [ ] src/ingestion/text_extractor.py (TXT, MD, HTML, EPUB)


## PHASE 5.5: AWS DATASET COLLECTION & MERGE [DONE - 2026-03-13]
- [x] AWS EC2 m6i.2xlarge setup (8 vCPU, 32GB RAM, 300GB EBS)
- [x] 21 datasets downloaded (GRETIL, Open Philology, Wikisource, 14 HuggingFace, Wikipedia Hindi/Sanskrit)
- [x] Processing script: 679K+ chunks into GCloud-compatible JSON format
- [x] Wikisource streaming parser fix (144K pages, OOM resolved with iterparse)
- [x] S3 sync: 146,444 files, 5.1GB to s3://brahm-ai-datasets/processed/
- [x] GCloud merge: aws s3 sync to ~/books/data/aws_datasets/ (5.1GB pulled)
- [x] FAISS + numpy version fix on GCloud VM (numpy 1.26.4, faiss-cpu reinstalled)
- [x] AWS EC2 stopped (cost saving)
- See AWS_SETUP.md for full details

### Merge Details (AWS → GCloud)
```
On GCloud VM:
1. Installed awscli: sudo apt install awscli && aws configure
2. Pulled from S3: aws s3 sync s3://brahm-ai-datasets/processed/ ~/books/data/aws_datasets/
3. Result: 146,444 files, 5.1GB in ~/books/data/aws_datasets/
   - aws_datasets/sanskrit/: 144,835 files (GRETIL, Open Philology, Wikisource, HF)
   - aws_datasets/hindi/: 1,604 files (Wikipedia Hindi, Aya)
   - aws_datasets/multilingual/: 4 files (Gita, Sanskrit-English parallel)
4. numpy conflict: faiss-cpu needed numpy>=1.26, paddleocr needed numpy==1.24
   - Fix: pip install numpy==1.26.4 faiss-cpu --force-reinstall (OCR already done, paddleocr warning OK)
```

## PHASE 6: EMBEDDING & INDEXING [DONE - 2026-03-14]
- [x] scripts/02_build_index.py created (combined embed + index builder)
- [x] FAISS 1.13.2 + numpy 1.26.4 + sentence-transformers working on VM
- [x] NVIDIA driver fix (see Issues below)
- [x] GPU verified: NVIDIA L4 24GB, driver 595.45.04, CUDA 13.2
- [x] File collection: 146,809 JSON files (366 GCloud + 144,835 AWS Sanskrit + 1,604 Hindi + 4 Multilingual)
- [x] Chunk loading: 1,104,518 chunks loaded in 48 seconds
- [x] Language distribution: sanskrit=791,691, hindi=178,809, english=134,018
- [x] Encoding completed: 1,104,518 chunks encoded in 25 min on L4 GPU (1672 seconds, batch_size=512)
- [x] FAISS HNSW indexes built (total build time: 33.7 min / 2024 seconds):
  - master.index: 1,104,518 vectors (all languages combined)
  - sanskrit.index: 791,691 vectors → 1365.2 MB
  - hindi.index: 178,809 vectors → 308.3 MB
  - english.index: 134,018 vectors → 231.1 MB
  - metadata.db: SQLite with 1,104,518 chunk records
  - documents.jsonl: full text store for retrieval
- [x] Output: ~/books/indexes/ (4 FAISS indexes + metadata.db + documents.jsonl)
- [ ] src/embedding/base.py (abstract interface — later, modular refactor)
- [ ] src/embedding/minilm.py (later)
- [ ] src/indexing/faiss_manager.py (later)
- [ ] src/indexing/bm25_manager.py (keyword index — later)
- [ ] src/indexing/metadata_store.py (later)
- [ ] Test: search quality check

### NVIDIA GPU Driver Issue & Fix (2026-03-14)
```
Problem: nvidia-smi failed — "couldn't communicate with NVIDIA driver"
Root cause: GPU physically present (lspci showed L4) but kernel module not built

Diagnosis steps:
1. lspci | grep -i nvidia → "NVIDIA Corporation AD104GL [L4]" ✓ (hardware present)
2. nvidia-smi → FAILED (driver not communicating)
3. sudo modprobe nvidia → "Module nvidia not found in /lib/modules/6.1.0-44-cloud-amd64"
4. dpkg -l | grep nvidia → nvidia-kernel-dkms 595.45.04 installed BUT module not compiled

Root cause: linux-headers for cloud kernel variant missing
- VM runs kernel: 6.1.0-44-cloud-amd64
- Headers installed were for: 6.1.0-44-amd64 (non-cloud variant)
- DKMS couldn't compile nvidia.ko without matching headers

Fix:
1. sudo apt install -y linux-headers-$(uname -r)   # installed cloud variant headers
2. DKMS auto-triggered: built nvidia 595.45.04 for 6.1.0-44-cloud-amd64
3. sudo modprobe nvidia → SUCCESS
4. nvidia-smi → L4 24GB, Driver 595.45.04, CUDA 13.2

Other attempted fixes that FAILED:
- sudo apt install nvidia-driver-535 → "Unable to locate package" (not in Debian 12 repos)
- /opt/deeplearning/install-driver.sh → path doesn't exist on this VM image
- cuda-keyring from NVIDIA repos → installed but didn't fix the header mismatch
```

### FAISS Index Build Results (2026-03-14)
```
Script: ~/books/scripts/02_build_index.py
Model: sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 (384 dim)
Device: NVIDIA L4 24GB GPU (CUDA 13.2), batch_size=512

Input:
  Files collected: 146,809 JSONs (366 GCloud OCR + 146,443 AWS datasets)
  Chunks loaded: 1,104,518 in 48 seconds
  Language split: Sanskrit 791,691 | Hindi 178,809 | English 134,018

Encoding:
  2,158 batches × 512 chunks = 1,104,518 vectors
  Time: 25 minutes 32 seconds (1672s) on L4 GPU
  Output shape: (1,104,518, 384) float32

FAISS HNSW Indexes Built:
  sanskrit.index → 791,691 vectors → 1365.2 MB
  hindi.index   → 178,809 vectors → 308.3 MB
  english.index → 134,018 vectors → 231.1 MB
  Total index size: ~1.9 GB

Output: ~/books/indexes/ (3 language indexes + metadata.db + documents.jsonl)
```


## PHASE 7: RETRIEVAL + RAG PIPELINE [DONE - 2026-03-14, UPDATED 2026-03-19]
- [x] scripts/04_test_search.py — FAISS semantic search test across all 3 language indexes
- [x] scripts/05_test_rag.py — End-to-end RAG pipeline test
- [x] scripts/06_chat_terminal.py — Interactive ChatGPT-like terminal with chat memory
- [x] FAISS search working: query → embed → search per-language index → retrieve docs
- [x] documents.jsonl offset fix: sort by global faiss_id per language, map to 0-based local IDs
- [x] ~~Qwen2.5-7B-Instruct~~ **DELETED 2026-03-19** — replaced by Gemini API
- [x] **api/services/rag_service.py rewritten (2026-03-19)**:
  - `load_all()`: Qwen loader removed, no GPU usage for LLM
  - `generate_stream()`: Gemini API (`models/gemini-2.5-flash`) replaces Qwen generation
  - BM25 cache: saved to disk after first build → ~30 sec load on restart (was 5-7 min rebuild)
  - **Startup time: 20-25 min → 3-4 min** (no Qwen + BM25 cache)
- [x] Source citations working: book names cited in answers
- [x] Chat memory: last 5 turns passed to LLM for context continuity
- [ ] src/retrieval/hybrid_search.py (modular refactor — not yet)
- [ ] src/retrieval/reranker.py (modular refactor — not yet)

### Search Quality Test Results (2026-03-14)
```
Query                          | Lang    | Quality | Notes
-------------------------------|---------|---------|------
What is concept of Dharma?     | english | GOOD    | Vimalakirti Sutra, Upanishads, relevant results
Yoga Sutras of Patanjali       | all     | OK      | Hindi Wikipedia, not original Sanskrit text
What are the four Vedas?       | all     | BAD     | Sanskrit mono = parsed grammar, not actual Vedas
panchang tithi nakshatra       | all     | BAD     | Got Khecarividya (unrelated)
```

### RAG Answer Quality Test (2026-03-14)
```
Query: "What is the concept of Dharma in Vedic texts?"
- Sources: Purva Mimamsa Sutra, Dashabhumika Sutra, Brahmanda Purana, Dharmasutras, 108 Upanishads
- Answer: Excellent — 5 sources cited with proper quotes, structured response
- Quality: HIGH

Query: "Explain the significance of Gayatri Mantra"
- Sources: Wikipedia Hindi, Brihat Samhita, Kavyamala, Bhartiya Jyotish
- Answer: Good but slight hallucination in Sanskrit text ("Ma什o" garbled from OCR)
- Quality: MEDIUM (OCR artifacts in source data)

Query: "Give me 5 Gita slokas"
- Answer: Qwen HALLUCINATED fake Sanskrit verses (not from context)
- Quality: LOW — actual Gita text not in FAISS index (Gita Infinity dataset not indexed)
- Fix needed: Add Gita dataset to FAISS, lower temperature to 0.3

Query: "Explain it" (follow-up)
- Answer: Lost context — searched for "Explain it" literally, got unrelated results
- Fix needed: Follow-up query rewriting (prepend previous question)
```

### Known Issues
1. **Hallucinated Sanskrit slokas**: Qwen makes up verses when context lacks actual slokas
2. **Follow-up context loss**: Vague queries like "Explain it" search for wrong docs
3. **Sanskrit mono dataset noise**: Grammar/morphology data pollutes Sanskrit search results
4. **OCR artifacts in Hindi results**: Scanned PDF text has garbled characters
5. **Temperature too high**: 0.7 allows hallucination, need 0.3 for factual answers


## PHASE 8: QUALITY IMPROVEMENT [IN PROGRESS - 2026-03-14]

### Done
- [x] Temperature 0.7 → 0.3 (no more hallucination)
- [x] Stricter system prompt ("NEVER generate Sanskrit verse not in context")
- [x] Follow-up query rewriting (pronoun-based: "Explain it" → "What is Dharma? — Explain it")
- [x] BM25 hybrid search (rank_bm25, FAISS top-20 + BM25 top-20 → RRF fusion)
- [x] Cross-encoder reranker (ms-marco-MiniLM-L-6-v2, RRF top-20 → reranked top-5)
- [x] Repetition penalty 1.1 → 1.3
- [x] OCR correction Layer 2 — dictionaries built (Hindi+Sanskrit: 11.7M words, English: 128K words), 879 chunks corrected
- [x] BhagavadGita.json added (16,518 chunks: 719 Sanskrit, 2,095 Hindi, 13,704 English)
- [x] FAISS rebuild with Gita — 1,121,036 total chunks (was 1,104,518)
  - sanskrit.index: 792,410 | hindi.index: 180,904 | english.index: 147,722
  - BG chapters verified in search results ✅
- [x] Auto language routing in hybrid_search — Devanagari query → Sanskrit+Hindi only (skip English index)
- [x] Source quality boost — date-stamped scanned PDFs penalized 0.75x in reranking
- [x] OCR quality multiplier — correction_report.json scores loaded at startup, applied as rerank multiplier
  - 250 files scored | 17 BAD (<0.30) | 62 FIXABLE (0.30-0.45) | penalized in search
  - Quality: `min(1.0, avg_score / 0.80)` — Kathasaritsagara (0.0) fully excluded, good PDFs (0.8+) full weight

### OCR Quality Report (GCloud JSONs — 92,079 chunks scored)
```
GOOD (80%+):       6,142 chunks (6.7%)
FIXABLE (50-80%): 45,318 chunks (49.2%)
GCV_NEEDED (30-50%): 31,729 chunks (34.5%)
BAD (<30%):        8,890 chunks (9.7%)

Worst files (avg_score): Kathasaritsagara 0.000, matsya_purana 0.243,
  Ashtanga Sangraha 0.250, Sri Bhashya Ramanuja 0.279
```

- [x] hf/sanskrit_mono heavily penalized (0.30) — grammar analysis noise removed from results
- [x] BhagavadGita/ source boosted (1.1) — actual slokas now prioritized
- [x] Transliteration stripped from context (Devanagari-only sent to Qwen)
- [x] Language routing: Devanagari query → Sanskrit+Hindi indexes only
- [x] System prompt: NEVER respond in Chinese (was outputting Chinese)
- [x] Layer 2 rescore with 11.7M dict — 367 files scored, GCV_needed=49,547

### Test Results After All Fixes
```
"Give me 3 BG slokas on karma yoga" → BG 3.3, 3.28, 3.29 (actual slokas!) ✅
"/sources" → all 5 from BhagavadGita/ ✅  (was: all 5 from hf/sanskrit_mono)
"What are four Vedas?" → GRETIL dictionary ✅  (was: OCR garbage from scanned PDFs)
No Chinese output ✅
No hallucination ✅
```

- [x] Streaming output — TextIteratorStreamer, token-by-token display in terminal
- [x] Gradio Web UI — scripts/07_gradio_app.py, running on port 7860
  - URL: http://34.135.70.190:7860 (requires GCloud firewall rule brahm-ai-gradio)
  - Features: Chat + Retrieved Sources panel + Language Filter dropdown
  - Gradio 6.0 compatible (dict message format)
  - Streaming responses in browser ✅
- [x] scripts/08_gradio_kundali.py — Gradio v2 with Kundali tab (2026-03-14)
  - Tab 1: RAG Chat (same as 07 + Kundali query routing hint)
  - Tab 2: Kundali Calculator — city dropdown (40 cities), form input, pyswisseph calculation
  - Output: Markdown table with Graha positions, Bhava Chakra, Vimshottari Dasha, Yogas
  - Auto-fill lat/lon when city selected
  - DONE: Uploaded + running on VM ✅
  - Kundali tab: full chart output verified ✅
  - Chat tab: Dharma answer working ✅
  - share=False fix (share=True was causing hang)

### Pending
- [ ] OCR correction Layer 3 (GCV) — scripts/03_gcv_layer3.py created, needs VM setup
- [ ] Keep VM running cost: stop when not using (~$0.60/hr)


## PHASE 9: CHAT ENHANCEMENTS [PARTIAL]
- [x] Streaming output — TextIteratorStreamer in both terminal + Gradio
- [x] Web UI — Gradio 6.9.0, port 7860, live at http://34.135.70.190:7860
- [x] Kundali query routing — detects kundali/dasha/lagna → hints Kundali tab
- [ ] src/chat/router.py (auto-route: RAG vs Kundali vs Panchang vs General)
- [ ] Query rewriting for follow-ups
- [ ] Response quality scoring

## OCR LAYER 3: GCV [RUNNING - 2026-03-15]
- [x] scripts/03_gcv_layer3.py created (796 lines)
  - Target: books with score 0.15-0.55 (confident improvement zone)
  - Per-page GCV document_text_detection (best Devanagari support)
  - Page cache: each PDF page sent to GCV only once
  - Cost estimate shown before processing (~$1.50/1000 pages)
  - Dry-run mode, --limit N for testing
  - Merges improved scores back into correction_report.json
- [x] VM setup: pip install google-cloud-vision ✅
- [x] Bugs fixed:
  - RAW_DIR: "raw" → "data/raw" (PDFs are in ~/books/data/raw/)
  - book_scores key: report uses "book_scores" not "books"
  - avg_score field: report uses "avg_score" not "avg_score_after"
  - JSON lookup: source has .pdf extension → use Path(source).stem + space→underscore fallback
  - OAuth scope: VM had insufficient scopes → Edit VM → "Allow full access to all Cloud APIs" → restart
- [x] Dry-run tested: 132 target books, 38,748 pages, $58.12 estimated
- [x] RUNNING LIVE: --max-score 0.30 --limit 5 (5 worst books, ~$5.24)
  - Nyaya Sutras of Gautama (0.153), BhavaniSoundaryaLahari (0.162),
    Surya_Siddhanta_English (0.173), Yogavasishtha-2 (0.174), Yogavasishtha-1 (0.179)
- [ ] After test: run --max-score 0.40 (all books score 0.15-0.40, ~$22)
- [ ] After that: full run --max-score 0.55 (~$58 total)


## PHASE 10: SPECIAL MODULES [IN PROGRESS]
- [x] NASA DE441 loaded and tested (±0.001 arcsecond accuracy)
- [x] Graha positions working: Surya/Chandra/Mangal/Guru/Shani/Shukra/Budh/Rahu/Ketu
- [x] Sidereal (Vedic) positions with Lahiri Ayanamsha
- [x] Rashi + Nakshatra calculation working
- [x] All libraries installed: pyswisseph, skyfield, ephem, kerykeion, jplephem
- [x] Swiss Ephemeris basic files downloaded (seas/sepl/semo_18.se1)
- [x] src/special/kundali.py DONE (29KB - full birth chart)
  - 9 Grahas, 12 Rashis, 27 Nakshatras, 12 Houses
  - Vimshottari Mahadasha (120-year cycle)
  - 7 Yogas detection (Gajakesari, Budhaditya, Mahapurusha, Mangal Dosha, Raj Yoga etc.)
  - Graha status: Uchcha/Neecha/Svakshetra
  - Full Hindi/Devanagari output
  - DONE: Tested on VM ✅ — Lagna Tula, 3 Uchcha planets, Raj Yoga confirmed
  - Integrated in 08_gradio_kundali.py Tab 2 ✅
- [x] src/special/panchang.py DONE (2026-03-15, ~29KB)
  - 5 Angas: Tithi, Vara, Nakshatra, Yoga, Karana
  - Sunrise/Sunset via pyswisseph rise_trans (fixed: CALC_RISE/SET at pos 3, FLG_SWIEPH at end)
  - Rahukaal calculation (8 segments by weekday)
  - Abhijit Muhurta (solar noon ±24 min)
  - Full Hindi names + auspiciousness flags
  - DONE: Tested on VM ✅ — New Delhi 2026-03-15: Ravivara, Krishna Dvadashi, Shravana, Shiva Yoga ✓
  - DONE: Integrated as Tab 3 in 08_gradio_kundali.py ✅ (2026-03-15)
    - Date picker + Hour/Minute inputs + City dropdown + Lat/Lon + TZ
    - Auto-fill lat/lon from city
    - Panchang query routing in Chat tab (tithi/nakshatra/rahukaal → hint to Panchang tab)
    - Markdown table output: all 5 angas + Rahukaal + Abhijit Muhurta
    - Verified: Robertsganj 2026-03-15 — Ravivara, Krishna Dvadashi, Shravana, Shiva ✅
- [ ] Swiss Ephemeris extra files (pending download on VM - see VM_SETUP.md)
- [ ] src/special/varshaphala.py (solar return charts, annual predictions)
- [ ] src/special/strength.py (ashtakavarga & shadbala calculations)
- [ ] src/special/prashna.py (horary/prashna astrology)
- [ ] src/special/grahan.py (eclipse timing & visibility)
- [ ] src/special/chart_render.py (North Indian + South Indian chart styles)
- [ ] src/special/compatibility.py (kundali milan, 36 guna matching)


## PHASE 10.5: PALMISTRY AI [LIVE - 2026-03-19]
- [x] VM folder structure: `~/books/data/palmistry/`, `~/books/models/palmistry/`
- [x] 6 pipeline scripts uploaded: `palm_01_preprocess.py` to `palm_06_inference.py`
- [x] mediapipe v0.10.14 installed (solutions API works)
- [x] MediaPipe warp pipeline: 4/4 reference images warped to 512×512 ✅
- [x] Vedic rules engine: `palm_05_interpret.py` — JSON + scores (love/career/health 1-10)
- [x] **Gemini Vision API working**: `models/gemini-2.5-flash`, full Vedic palm reading in one call ✅
- [x] **FastAPI endpoint live**: `POST /api/palmistry/analyze` — multipart → Gemini → structured JSON
- [x] Router registered in `api/main.py`
- [x] GEMINI_API_KEY set permanently in `~/.bashrc` on VM
- [x] **Frontend**: `PalmistryPage.tsx` — "AI Palm Reading (Gemini Vision)" button → `ai_report` step
- [x] End-to-end tested: `hand1.jpg` → 200 OK → full Vedic reading with 6 lines, life areas, remedies ✅
- [ ] `ai_report` step render in PalmistryPage.tsx (show Gemini results in UI) ⬜
- [ ] U-Net segmentation model (Phase 2 — needs 200+ annotated images)
- [ ] Label Studio annotation setup (Phase 2)

**Key Decision**: Gemini Vision replaces U-Net for MVP — reads palm image directly, no segmentation needed.
**Reading includes**: hand_type (Vedic + element), overview, 6 lines (Heart/Head/Life/Fate/Sun/Mercury), dominant mounts, life_areas (6 scores), strengths, challenges, remedies, summary with Sanskrit shloka, auspicious_note.


## PHASE 10.6: FRONTEND — SKY PAGE REDESIGN [DONE - 2026-03-19]
- [x] **ZodiacWheel**: wrapped in `React.memo` — only re-renders on rashi change (not every second)
- [x] **ZodiacWheel**: occupied rashi segments highlighted (brighter, colored border, bold symbol)
- [x] **PlanetCards**: new 3×3 grid replacing boring table — each card has symbol glow, Hindi name, rashi badge, plain-language effect (PLANET_DOMAIN + RASHI_QUALITY maps)
- [x] **CosmicSummary**: new card showing today's overall mood (Auspicious/Reflective/Intense/Balanced) based on Moon rashi + retro/combust status
- [x] **Critical bug fixed**: `AnimatePresence mode="wait" key={snapshot.localTime.getSeconds()}` — was unmounting/remounting entire table every second with animation. Removed.
- [x] `PLANET_DOMAIN` + `RASHI_QUALITY` lookup maps added for plain-language meanings
- [x] Legend simplified


## PHASE 10.7: PRODUCTION DEPLOYMENT [DONE - 2026-03-19]

### What was done
- [x] Nginx installed on VM
- [x] Frontend built on VM: `git clone https://github.com/amareshsingh0/brahm_ai.git` → `pnpm install` → `pnpm run build`
- [x] `dist/` copied to `/var/www/brahm-ai/`
- [x] Nginx config: serves frontend + proxies `/api/` → `localhost:8000`
- [x] SSL certificate via Let's Encrypt certbot
- [x] **Live at**: https://brahmasmi.bimoraai.com ✅
- [x] `brahm-api.service` systemd service — auto-starts FastAPI on VM boot
- [x] `GEMINI_API_KEY` added to systemd service via override.conf

### Failures & Fixes

| Problem | Root Cause | Fix |
|---|---|---|
| `ERR_INCOMPLETE_CHUNKED_ENCODING` on chatbot | Nginx buffering SSE stream | Added `proxy_buffering off`, `proxy_http_version 1.1`, `proxy_set_header Connection ""`, `chunked_transfer_encoding on` |
| `--reload` flag in production | File watcher interferes with long streaming connections | Removed `--reload` — use plain `uvicorn` in production |
| `503 Service Unavailable` on chat | RAG not loaded yet (BM25 + FAISS take 3-4 min on startup) | Wait for `[RAG] All components loaded!` before using chatbot |
| Port 8000 always occupied | `brahm-api.service` systemd service auto-restarts uvicorn | Must use `sudo systemctl stop brahm-api.service` to stop, NOT pkill |
| `GEMINI_API_KEY` not available in service | systemd doesn't inherit `~/.bashrc` env vars | Added via `sudo systemctl edit brahm-api.service` → override.conf |
| certbot timeout on first attempt | GCP firewall ports 80/443 not open | `gcloud compute firewall-rules create allow-http/https` + add tags to VM |
| `npm install -g pnpm` permission denied | No sudo | Use `sudo npm install -g pnpm` |

### Future Deployment Workflow (after any code change)
```bash
# On VM:
cd ~/brahm_ai
git pull
pnpm run build
sudo cp -r dist/* /var/www/brahm-ai/
# No server restart needed — frontend is static
```

### Service Management
```bash
sudo systemctl status brahm-api.service   # check status
sudo systemctl restart brahm-api.service  # restart API
sudo systemctl stop brahm-api.service     # stop API
sudo journalctl -u brahm-api.service -f   # live logs
```

### Nginx Management
```bash
sudo nginx -t                             # test config
sudo systemctl restart nginx              # restart nginx
sudo nano /etc/nginx/sites-available/brahm-ai  # edit config
```

### Key Production Rules
1. **Never use `--reload` in production** — breaks SSE streaming
2. **Wait 3-4 min after restart** before using chatbot (RAG loading)
3. **GEMINI_API_KEY must be in systemd override.conf** — not just ~/.bashrc
4. **Frontend deploy = git pull + pnpm build + cp dist/*** — no server restart
5. **SSL auto-renews** via certbot timer — no manual action needed


## PHASE 11: FINE-TUNING (Optional) [NOT STARTED]
- [ ] src/finetune/dataset_builder.py (instruction/input/output pairs)
- [ ] src/finetune/trainer.py (QLoRA/PEFT)
- [ ] src/finetune/evaluator.py (test quality)
- [ ] scripts/07_finetune.py
- [ ] Build fine-tune dataset from Gita Q&A pairs


## PHASE 12: SCALING [NOT STARTED]
- [ ] Add more books (target: 100+ books)
- [ ] Index sharding if needed
- [ ] Performance benchmarks
- [ ] Redis semantic cache setup


---
## DATASETS COLLECTED
| # | Dataset Name | Source | Size | Languages | Status |
|---|---|---|---|---|---|
| 1 | Bhagwat-Gita-Infinity | Modotte (HuggingFace) | 36.5 MB | San/Hin/Eng | Ingested + Indexed |
| 2 | VedAstro Planet Data | VedAstro (HuggingFace) | ~5 GB | English/CSV | Downloaded |
| 3 | VedAstro 15000 Birth Data | VedAstro (HuggingFace) | ~50 MB | English | Downloaded |
| 4 | VedAstro Marriage Data | VedAstro (HuggingFace) | ~50 MB | English | Downloaded |
| 5 | Celestial Spiritual AI | dp1812 (HuggingFace) | ~100 MB | English | Downloaded |
| 6 | Kamika Agama Purva Pada | Google Drive | 4.9 MB | Sanskrit | Downloaded |
| 7 | Agama Academy PDFs (8) | agamaacademy.com | ~50 MB | Sanskrit | Downloaded |
| 8 | Agama & Tripitaka Vol 2 | Internet Archive | ~10 MB | Sanskrit/Eng | Downloaded |
| 9 | NASA JPL DE441 | NASA | 3 GB | Ephemeris | Loaded + Tested |
| 10 | Swiss Ephemeris (basic) | GitHub mirror | 2 MB | Ephemeris | Downloaded |
| 11 | Swiss Ephemeris (extra) | GitHub mirror | ~6 MB | Ephemeris | Pending |
| 12 | VedAstro GitHub Repo | GitHub | ~50 MB | Code | Cloned |
| 13 | Agama Academy mixed PDFs (80+) | agamaacademy.com | 2.8 GB | San/Tam/Mixed | Downloaded |
| 14 | Jyotisha/Astronomy PDFs (12+) | Internet Archive | 2.2 GB | San/Eng | Downloaded |
| 15 | Vedic Astrology PDFs (15+) | Various | 384 MB | San/Hin/Eng | Downloaded (cleaned) |
| 16 | SARIT Sanskrit Corpus | GitHub | 204 MB | Sanskrit (XML) | **Cloned (2026-03-10)** |
| 17 | SuttaCentral bilara-data | GitHub | 306 MB | Pali+English (JSON) | **Cloned (2026-03-10)** |
| 18 | Archive.org Vedas | archive.org/ia | 223 MB | San/Eng (PDF) | **Downloaded (2026-03-10)** |
| 19 | Archive.org Upanishads | archive.org/ia | 1.55 GB | San/Hin/Eng (PDF) | **Downloaded (2026-03-10)** |
| 20 | Archive.org Puranas | archive.org/ia | 3.47 GB | San/Hin/Eng (PDF) | **Downloaded (2026-03-10)** |
| 21 | Archive.org Epics | archive.org/ia | 23 MB | English (PDF) | **Downloaded (2026-03-10)** |
| 22 | Archive.org Ayurveda | archive.org/ia | 317 MB | Hin/Eng (PDF) | **Downloaded (2026-03-10)** |
| 23 | Archive.org Darshana | archive.org/ia | 206 MB | San/Eng (PDF) | **Downloaded (2026-03-10)** |
| 24 | Archive.org Tantra | archive.org/ia | 341 MB | San/Hindi (PDF) | **Downloaded (2026-03-10)** |
| 25 | Archive.org Yoga | archive.org/ia | 25 MB | San/Eng (PDF) | **Downloaded (2026-03-10)** |
| 26 | Archive.org Grammar | archive.org/ia | 29 MB | Sanskrit (PDF) | **Downloaded (2026-03-10)** |
| 27 | Archive.org Buddhist | archive.org/ia | 592 KB | Pali/Eng (PDF) | **Downloaded (2026-03-10)** |

## KEY DECISIONS LOG
| Date | Decision | Reason |
|---|---|---|
| 2026-03-06 | Modular architecture with config files | Future upgrades easy, no code change needed |
| 2026-03-06 | Language-wise separate FAISS indexes | No cross-language noise in search |
| 2026-03-06 | SQLite for chunk metadata | Track every chunk's source, page, language |
| 2026-03-06 | Separate ingestion for PDF vs JSON | HF datasets are structured, PDFs need extraction |
| 2026-03-07 | NASA DE441 for extreme Jyotish accuracy | ±0.001 arcsecond, 13000 BC to 17000 AD |
| 2026-03-07 | Triple verification (NASA + Swiss Ephe + VedAstro) | Cross-verify for 100% confidence |
| 2026-03-07 | Google Drive for PDF upload to VM | SSH/SCP had permission issues, gdown works |
| 2026-03-08 | kundali.py with full Hindi/Devanagari output | User wanted readable Sanskrit + Hindi explanation |
| 2026-03-08 | Swiss Ephe from GitHub mirror (not astro.com) | astro.com wget gives 0 byte files, GitHub works |
| 2026-03-08 | Extra Swiss Ephe files for Varshaphala/Ashtakavarga | Need centuries 2000-2199 + ancient + fixed stars |
| 2026-03-08 | VM_SETUP.md created | Full VM structure + paths + pending tasks reference |
| 2026-03-09 | PaddleOCR for Indic scripts (not Tesseract) | Better accuracy for Devanagari, Tamil, Telugu |
| 2026-03-09 | Modular ingestion pipeline built (6 files) | pdf_extractor, script_detector, normalizer, chunker, pipeline, CLI |
| 2026-03-09 | VM duplicate cleanup done | 6 empty + 14 duplicate files removed, ~1.5GB freed |
| 2026-03-09 | raw/ organized: mixed/ sanskrit/ vedic_astrology/ jyotisha_astronomy/ | Category-based folder structure for 110+ PDFs |
| 2026-03-09 | PaddleOCR v2.9.1 (not v3.4/v5) | v3.4 has broken langchain imports + PDX reinit; v5 has NotImplementedError |
| 2026-03-09 | Simplified chunker (no verse-boundary search) | Old chunker O(n²) on 1000+ page books, caused stuck processing |
| 2026-03-09 | Pipeline saves stats after each book | Resume-friendly: re-run skips already processed books |
| 2026-03-09 | OCR accuracy: Text 99%+, English scan 95-98%, Devanagari 85-92% | Old manuscripts 60-80% depending on scan quality |
| 2026-03-10 | 7 text sources researched for bulk download | GRETIL (403 blocked), Sacred-texts (Cloudflare), SARIT/SuttaCentral/Archive.org worked |
| 2026-03-10 | Archive.org `ia` CLI for bulk PDF download | 31 items, 62 PDFs, 6.1GB in ~30 min. Sequential better than parallel (throttling) |
| 2026-03-10 | SARIT + SuttaCentral git cloned | 85 Sanskrit XML + full Pali Canon JSON, needs new extractors |
| 2026-03-10 | Need new extractors: xml_extractor, json_extractor, html_extractor | Existing pipeline only handles PDF; new data is XML/JSON/HTML |
| 2026-03-10 | Archive.org PDFs organized into 11 category folders | vedas/ puranas/ smriti_dharma/ itihasa/ yoga/ ayurveda/ tantra/ bauddha/ jaina/ darshana/ grammar_literature/ |
| 2026-03-10 | pipeline.py FOLDER_CATEGORY_MAP patched + parts[0] fix | Old code used str(rel_path.parent) which gave full path, not first component |
| 2026-03-10 | 8-window tmux parallel ingestion for new categories | Each window processes 1-3 categories simultaneously |
| 2026-03-10 | 11 pre-existing downloads discovered on VM | Manu Smriti, Yajnavalkya, 4 Puranas, Shiva Samhita, 2 Tantras, 2 Jain texts |
