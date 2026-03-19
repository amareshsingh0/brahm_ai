# Brahm AI - VM Setup & Structure Reference
# Last Updated: 2026-03-14

## VM Connection
```
Google Cloud VM: g2-standard-32
Username: amareshsingh2005
Hostname: brahm
Zone: us-central1-a
IP: 35.224.77.10
OS: Debian 12 / Ubuntu 22.04
GPU: NVIDIA L4 24GB (Driver 595.45.04, CUDA 13.2)
RAM: 128 GB
vCPU: 32
SSD: 250 GB
Cost: ~$0.60/hr (~$14.40/day) - ALWAYS STOP WHEN NOT USING

SSH: gcloud compute ssh amareshsingh2005@brahm --zone=us-central1-a
Python venv: source ~/ai-env/bin/activate   (venv is in ~/, NOT ~/books/)
IMPORTANT: Use python3, NOT python (Debian 12 has no python alias)
```

## VM Folder Structure (~/books/)
```
~/books/
├── config/                     # YAML config files (model, search, embedding)
├── models/                     # [NEW 2026-03-19] Trained model checkpoints
│   └── palmistry/              # U-Net palmistry models
│       ├── best_model.pth      # Best checkpoint by val mIoU
│       └── history.json        # Training loss/mIoU per epoch
├── data/
│   └── palmistry/              # [NEW 2026-03-19] Palm image data (not text)
│       ├── raw/                # Original palm photos (phone camera)
│       ├── processed/          # MediaPipe-warped 512×512 images
│       ├── annotated/
│       │   ├── images/         # Ready-for-training images
│       │   └── masks/          # PNG class masks (pixel = class id 0-7)
│       └── augmented/          # Albumentations output
│   ├── chunks/                 # Processed text chunks
│   ├── embeddings/             # Generated embeddings
│   ├── finetune/               # Fine-tuning datasets
│   ├── github/                 # VedAstro GitHub repo clone
│   ├── hf_datasets/            # HuggingFace datasets
│   │   ├── Bhagwat-Gita-Infinity/  # 36.5 MB, 18 chapters, 719 sloks
│   │   └── (others)
│   ├── nasa_ephe/              # NASA JPL DE441 (3 GB, extreme accuracy)
│   ├── processed/              # Processed/cleaned data
│   ├── processed_text/         # Extracted text from PDFs
│   ├── raw/                    # Raw source files (~13 GB total)
│   │   ├── mixed/              # 2.8G - 80+ Agama/Tamil/Sanskrit manuscripts
│   │   ├── sanskrit/           # 172M - Properly named Sanskrit texts
│   │   ├── vedic_astrology/    # 384M - Jyotish books (cleaned)
│   │   ├── jyotisha_astronomy/ # 2.2G - Major Jyotish classics
│   │   ├── vedas/              # [2026-03-10] 12 PDFs - Rigveda, Atharvaveda, 4-Veda set, 108 Upanishads, Shankara Bhashya
│   │   ├── puranas/            # [2026-03-10] 16 PDFs - Narada, Kurma, Bhagavatam, Vishnu, Shiva, Garuda, Agni, Markandeya, Matsya, 18-summary
│   │   ├── smriti_dharma/      # [2026-03-10] 7 PDFs - Manu Smriti, Yajnavalkya Smriti, Arthashastra, etc.
│   │   ├── itihasa/            # [2026-03-10] 1 PDF - Mahabharata Ganguli
│   │   ├── yoga/               # [2026-03-10] 5 PDFs - Yoga Sutras, Hatha Yoga Pradipika, Gheranda Samhita, Shiva Samhita
│   │   ├── ayurveda/           # [2026-03-10] 5 PDFs - Charaka Samhita 2 vols, Sushruta Samhita 3 vols
│   │   ├── tantra/             # [2026-03-10] 7 PDFs - Tantraloka 2 vols, Vijnana Bhairava, Mahanirvana, Kularnava
│   │   ├── bauddha/            # [2026-03-10] 1 PDF - Dhammapada
│   │   ├── jaina/              # [2026-03-10] 5 PDFs - Acharanga Sutra, Tattvartha Sutra, etc.
│   │   ├── darshana/           # [2026-03-10] 3 PDFs - Brahma Sutra Bhashya, Vivekachudamani
│   │   ├── grammar_literature/ # [2026-03-10] 1 PDF - Ashtadhyayi Panini
│   │   ├── sarit/SARIT-corpus/ # [2026-03-10] 204M - 85 TEI-XML Sanskrit texts
│   │   └── suttacentral/      # [2026-03-10] 306M - Full Buddhist Pali Canon
│   │       └── bilara-data/    #   42K+ JSON files, Pali+English aligned
│   ├── aws_datasets/            # [2026-03-14] Merged from AWS S3 (5.1GB, 146,444 files)
│   │   ├── sanskrit/            # 144,835 files (GRETIL, Open Philology, Wikisource, HF)
│   │   ├── hindi/               # 1,604 files (Wikipedia Hindi, Aya)
│   │   └── multilingual/        # 4 files (Gita, Sanskrit-English parallel)
│   └── swiss_ephe/             # Swiss Ephemeris files
│       ├── seas_18.se1         # 218K - Asteroids 1800-1999
│       ├── semo_18.se1         # 1.3M - Moon 1800-1999
│       ├── sepl_18.se1         # 473K - Planets 1800-1999
│       ├── seas_20.se1         # (pending) Asteroids 2000-2199
│       ├── semo_20.se1         # (pending) Moon 2000-2199
│       ├── sepl_20.se1         # (pending) Planets 2000-2199
│       ├── seplm18.se1         # (pending) Planets ancient
│       ├── semom18.se1         # (pending) Moon ancient
│       ├── seasm18.se1         # (pending) Asteroids ancient
│       ├── sefstars.txt        # (pending) Fixed Stars
│       └── seorbel.txt         # (pending) Orbital elements
├── indexes/                    # FAISS HNSW indexes (1.9 GB total, built 2026-03-14)
│   ├── master.index            # 1,104,518 vectors (all languages combined)
│   ├── sanskrit.index          # 791,691 vectors (1365 MB)
│   ├── hindi.index             # 178,809 vectors (308 MB)
│   ├── english.index           # 134,018 vectors (231 MB)
│   ├── metadata.db             # SQLite: 1,104,518 chunk records
│   └── documents.jsonl         # Full text store for retrieval
├── scripts/                    # Pipeline scripts (01_ingest -> 06_chat + palm_* palmistry)
│   ├── palm_01_preprocess.py   # [NEW] MediaPipe warp → 512×512 canonical palm
│   ├── palm_02_dataset.py      # [NEW] PyTorch Dataset class (8-class segmentation)
│   ├── palm_03_train.py        # [NEW] U-Net ResNet34 training on L4 GPU
│   ├── palm_04_features.py     # [NEW] Geometric feature extraction (length/curve/forks)
│   ├── palm_05_interpret.py    # [NEW] Vedic rules engine + Gemini prompt builder
│   └── palm_06_inference.py    # [NEW] Full end-to-end pipeline test
├── src/
│   ├── chat/                   # Chat session, memory, router
│   ├── embedding/              # Embedding models (MiniLM etc.)
│   ├── indexing/               # FAISS, BM25, SQLite metadata
│   ├── ingestion/              # PDF/JSON/text extractors, chunker
│   ├── llm/                    # Qwen2.5-7B-Instruct loader [DEPRECATED — Qwen deleted, using Gemini API]
│   ├── retrieval/              # Hybrid search, reranker
│   └── special/                # Jyotish calculation modules
│       ├── __init__.py         # Module init
│       ├── kundali.py          # 29KB - Birth chart (DONE, Hindi output)
│       └── palmistry_service.py # [NEW] Inference class (model load + pipeline)
│       ├── panchang.py         # (planned) Tithi, Nakshatra, Yoga, Karana
│       ├── varshaphala.py      # (planned) Solar Return / Annual predictions
│       ├── strength.py         # (planned) Ashtakavarga & Shadbala
│       ├── prashna.py          # (planned) Horary astrology
│       ├── grahan.py           # (planned) Eclipse timing & visibility
│       └── chart_render.py     # (planned) North/South Indian chart styles
├── full_pipeline_test.py       # Quick test: Gita data -> FAISS -> search
├── smart_search.py             # Hybrid search + Qwen RAG answers
├── ingest_pdf.py               # PDF ingestion pipeline
├── test_pipeline.py            # Basic pipeline test
└── download_all_data.sh        # Master data download script
```

## Installed Libraries (ai-env)
```
# AI/ML
torch, transformers, accelerate, bitsandbytes
sentence-transformers (embedding)
faiss-cpu 1.13.2 (vector search — faiss-gpu not needed, CPU loads GPU-built indexes fine)
rank_bm25 (keyword search)
google-genai (Gemini API — replaces Qwen for all LLM narrative; Qwen model deleted 2026-03-19)
# NOTE: Qwen2.5-7B-Instruct DELETED from VM. GPU now reserved for U-Net training only.
# All LLM calls → Gemini API (GEMINI_API_KEY env var)

# Jyotish/Astronomy
pyswisseph (Swiss Ephemeris Python bindings)
skyfield (modern astronomy)
ephem (astronomical calculations)
kerykeion (astrology charts)
jplephem (NASA JPL ephemeris reader)

# Data Processing
pypdf, cryptography (PDF extraction)
pytesseract, pdf2image (OCR for scanned PDFs)
datasets (HuggingFace loader)
pandas, numpy

# OCR & NLP (updated 2026-03-12)
tesseract-ocr + tesseract-ocr-hin/san/tam/tel (system pkg: primary Indic OCR)
pytesseract (Python binding for Tesseract)
paddleocr==2.9.1 (English OCR fallback - MUST use v2.9.1, NOT v3.4/v5)
paddlepaddle (PaddlePaddle inference engine)
paddlex==3.0.1 (PaddleOCR dependency)
numpy==1.26.4 (upgraded from 1.24.4 for faiss-cpu; paddleocr warning OK since OCR done)
poppler-utils (system pkg: sudo apt install poppler-utils - required by pdf2image)
opencv-python-headless (image processing)
pdf2image (PDF to image conversion for OCR)
pypdf (text-based PDF extraction)
layoutparser (document layout detection)
langdetect (language detection)
indic-nlp-library (Indian language NLP)
aksharamukha (script transliteration - Devanagari/IAST/Tamil/Telugu etc.)
symspellpy (spell correction)
datasketch (MinHash near-duplicate detection)

# Utilities
gdown (Google Drive downloads)
huggingface_hub
```

## Key Paths on VM
```
Swiss Ephemeris:  ~/books/data/swiss_ephe/
NASA DE441:       ~/books/data/nasa_ephe/
Gita Dataset:     ~/books/data/hf_datasets/Bhagwat-Gita-Infinity/
VedAstro:         ~/books/data/github/VedAstro/
Raw PDFs:         ~/books/data/raw/ (36 GB, 394 PDFs in 26 subfolders)
Processed JSON:   ~/books/data/processed/json/ (pipeline output)
FAISS Index:      ~/books/indexes/
Kundali Module:   ~/books/src/special/kundali.py
Ingestion:        ~/books/src/ingestion/ (pipeline modules)
Config Files:     ~/books/config/
```

## Kundali.py - Current Features
- 9 Grahas: Surya, Chandra, Mangal, Budh, Guru, Shukra, Shani, Rahu, Ketu
- 12 Rashis with Hindi names (Devanagari)
- 27 Nakshatras with Hindi names
- 12 Houses (Bhava) with meanings in Hindi
- Vimshottari Mahadasha (120-year cycle)
- Yogas: Gajakesari, Budhaditya, Chandra-Mangal, 5 Mahapurusha, Mangal Dosha, Raj Yoga, Vipareeta Raj Yoga
- Graha status: Uchcha/Neecha/Svakshetra
- Hindi explanations for all results
- Current dasha highlighted
- Test data: Amaresh Singh, 14 April 2003, 8:30 PM, Robertsganj (24.6917N, 83.0818E)

## Pending Swiss Ephemeris Downloads
```bash
cd ~/books/data/swiss_ephe
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/sefstars.txt"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/seorbel.txt"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/sepl_20.se1"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/semo_20.se1"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/seas_20.se1"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/seplm18.se1"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/semom18.se1"
wget -q --show-progress "https://github.com/aloistr/swisseph/raw/master/ephe/seasm18.se1"
```

## File Upload to VM
SSH/SCP fails (permission issue). Use Google Drive + gdown:
1. Upload file to Google Drive
2. Get shareable link (Anyone with link)
3. On VM: `gdown "LINK" -O ~/books/path/to/file`

## Pending Kundali.py Upload
Local file updated with:
- Name: "Amaresh Singh"
- Coordinates: lat=24.6917104, lon=83.0817920
- Hindi output (Devanagari for all Grahas, Rashis, Nakshatras)
- Key planetary analysis in Hindi

Upload method: Google Drive + gdown to ~/books/src/special/kundali.py

## Missing Jyotish Modules (To Build)
1. **panchang.py** - Daily Panchang (Tithi, Nakshatra, Yoga, Karana, Var)
2. **varshaphala.py** - Solar Return Charts (annual predictions, Muntha, yearly dashas)
3. **strength.py** - Ashtakavarga (8-fold strength) + Shadbala (6 strengths)
4. **prashna.py** - Horary/Prashna Kundali (instant question charts)
5. **grahan.py** - Eclipse calculations (timing, visibility, duration)
6. **chart_render.py** - North Indian (diamond) and South Indian (grid) chart styles
7. **compatibility.py** - Kundali Milan (36 Guna matching for marriage)

## Regional Ayanamsha Support (Planned)
- Lahiri (North Indian default) - DONE
- KP (Krishnamurti Paddhati) - config ready
- Raman - config ready
- Drik (South Indian) - planned

## House Systems (Planned)
- Placidus (current default) - DONE
- Equal House
- Whole Sign (most traditional Vedic)
- Koch
- Campanus

## Ingestion Pipeline Modules (Created 2026-03-09)
```
src/ingestion/
├── __init__.py              # Module init
├── pdf_extractor.py         # pypdf (text) + Tesseract (Indic scanned) + PaddleOCR (English) + hybrid mode
├── script_detector.py       # Unicode range detection: Devanagari/Tamil/Telugu/Kannada/Malayalam/Bengali/Latin
├── normalizer.py            # Unicode NFC, Devanagari OCR fixes, danda cleanup, aksharamukha transliteration
├── chunker.py               # 400-token chunks, 50 overlap, fast word-split, JSON dataset chunker
└── pipeline.py              # Master orchestrator: discover → dedup → detect → extract → normalize → chunk → JSON (resume-friendly)

scripts/
└── 01_ingest_books.py       # CLI: --single, --category, --list-only, --force-ocr, --max-pages, --ocr-engine auto|tesseract|paddle
```

### Pipeline Output Format
Each book → `~/books/data/processed/json/BookName.json`:
```json
{
  "source": "filename.pdf",
  "category": "sanskrit",
  "primary_script": "devanagari",
  "total_pages": 100,
  "total_chunks": 250,
  "chunks": [
    {
      "chunk_id": "filename.pdf::p1::c0",
      "text": "...",
      "page_num": 1,
      "language": "hi",
      "category": "sanskrit",
      "word_count": 380
    }
  ]
}
```

### Pipeline Usage
```bash
# List all PDFs
python3 scripts/01_ingest_books.py --list-only

# Test with one small PDF (first 5 pages)
python3 scripts/01_ingest_books.py --single ~/books/data/raw/sanskrit/AgamaAcademy_Chintyagama.pdf --max-pages 5

# Process one category
python3 scripts/01_ingest_books.py --category sanskrit

# Process everything
python3 scripts/01_ingest_books.py
```

## OCR Setup (Multi-Engine Router)
**Primary: Tesseract** (best for Indic/Devanagari) | **Fallback: PaddleOCR** (English)

### Tesseract OCR (Primary - Indic scripts)
```bash
sudo apt install -y tesseract-ocr tesseract-ocr-hin tesseract-ocr-san tesseract-ocr-tam tesseract-ocr-tel
pip install pytesseract
```
API: `pytesseract.image_to_string(img, lang="hin+san+eng", config="--psm 6")`
Languages: eng, hin, san, tam, tel | DPI: 300 recommended
Quality: 90-95% for printed Devanagari, far better than PaddleOCR for Hindi/Sanskrit

### PaddleOCR v2.9.1 (Fallback - English)
**MUST use v2.9.1** - newer versions are broken:
- v3.4: `langchain.docstore` import error, `PDX reinit` error, `show_log` unknown arg
- v5: `NotImplementedError: ConvertPirAttribute2RuntimeAttribute not support`
- v2.9.1 API: `ocr.ocr(img_array, cls=True)` → result[0] → line[1][0] text, line[1][1] confidence

Install commands:
```bash
pip install paddleocr==2.9.1 paddlex==3.0.1
sudo apt-get install -y poppler-utils
```

Singleton pattern required (avoid PDX reinit error):
```python
_ocr_instance = None
def _get_paddle(lang="en"):
    global _ocr_instance
    if _ocr_instance is None:
        from paddleocr import PaddleOCR
        _ocr_instance = PaddleOCR(use_angle_cls=True, lang=lang, show_log=False)
    return _ocr_instance
```

### Surya OCR (ABANDONED)
- surya-ocr v0.17.1: `RecognitionPredictor` API broken (needs `FoundationPredictor`), `pad_token_id` error
- Torch version conflicts: surya wants >=2.7, torchvision needs ==2.5.1
- transformers incompatibility with surya model configs
- **Do not use** - Tesseract is simpler and produces better results

## Pipeline Run Commands
```bash
# Process all PDFs (background, won't stop on SSH disconnect)
nohup python3 scripts/01_ingest_books.py > ~/ingestion_full.log 2>&1 &

# Process single category
python3 scripts/01_ingest_books.py --category sanskrit

# Test single file (5 pages)
python3 scripts/01_ingest_books.py --single ~/books/data/raw/sanskrit/somefile.pdf --max-pages 5

# Monitor background run
tail -f ~/ingestion_full.log
```

## Pipeline Patch (2026-03-10)
- `pipeline.py` FOLDER_CATEGORY_MAP expanded: added vedas, puranas, smriti_dharma, itihasa, yoga, ayurveda, tantra, bauddha, jaina, darshana, grammar_literature
- Category detection fix: `parts = rel_path.parts; folder = parts[0]` instead of `str(rel_path.parent)` (old code gave full relative path like "vedas/rigvedacomplete" instead of just "vedas")
- Patched on VM via line-number based Python script (string-replace patches fail due to whitespace)
- 63 new PDFs properly detected across 11 categories

## 8-Window tmux Parallel Processing (2026-03-10)
```bash
tmux new-session -s ingest
# Window 1: vedas | Window 2: puranas | Window 3: smriti_dharma
# Window 4: itihasa+bauddha+grammar | Window 5: yoga | Window 6: ayurveda
# Window 7: tantra | Window 8: jaina+darshana
# Each window: source ~/ai-env/bin/activate && cd ~/books && python3 scripts/01_ingest_books.py --category <name>
```

## NVIDIA GPU Driver Fix (2026-03-14)
After VM reboot/kernel update, `nvidia-smi` may fail. Root cause: cloud kernel headers missing.
```bash
# Check if GPU hardware present
lspci | grep -i nvidia    # Should show "NVIDIA Corporation AD104GL [L4]"

# If nvidia-smi fails, fix kernel headers:
sudo apt install -y linux-headers-$(uname -r)
# DKMS auto-builds nvidia.ko for cloud kernel
sudo modprobe nvidia
nvidia-smi    # Should show L4 24GB, Driver 595.45.04, CUDA 13.2

# Key: VM kernel is 6.1.0-44-CLOUD-amd64 (not regular amd64)
# Regular headers won't match. Must install cloud variant.
```

## FAISS Index Build (2026-03-14)
```bash
# Build command (run from ~/books with ai-env activated):
cd ~/books && python3 scripts/02_build_index.py

# Result: 1,104,518 chunks from 146,809 JSON files
# Encoding: 25 min on L4 GPU (batch_size=512)
# Total build: 33.7 min
# Output: ~/books/indexes/ (master + sanskrit + hindi + english .index + metadata.db + documents.jsonl)
```

## RAG Pipeline (2026-03-14)
```bash
# Search test (no LLM needed):
python3 scripts/04_test_search.py

# Full RAG test (Search + Qwen LLM):
python3 scripts/05_test_rag.py

# Interactive chat terminal (ChatGPT-like):
python3 scripts/06_chat_terminal.py

# Chat commands: /lang [sanskrit|hindi|english|all], /search [query], /sources, /clear, /quit
```

### RAG Performance
```
Component              | Metric
-----------------------|--------
Qwen2.5-7B-Instruct   | 4-bit nf4, 6.3 GB VRAM, 8s load (cached), 78s cold start
Generation speed       | 15-16 tok/s, ~33s for 512 tokens
FAISS search           | <0.2s per query across all 3 language indexes
documents.jsonl lookup | Sort by global faiss_id per language, map to 0-based local IDs
Chat memory            | Last 5 turns passed to LLM
Temperature            | 0.7 (needs 0.3 for less hallucination)
```

## VM Cleanup Done (2026-03-09)
- 6 empty (0-byte) PDFs deleted from vedic_astrology/
- 14 duplicate PDFs removed (~1.5GB freed)
- Folders: mixed/ (2.8G), sanskrit/ (267M), vedic_astrology/ (384M), jyotisha_astronomy/ (2.2G)
