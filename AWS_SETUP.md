# AWS Dataset Collection Hub — Setup Guide

## Purpose
Parallel AWS setup for collecting **pre-built datasets and modernized/digitized books** (HuggingFace, DCS, GRETIL, Wikisource, etc.). This data will eventually **merge with Google Cloud VM** (which handles OCR of raw PDFs). Two separate pipelines → one combined knowledge base.

```
AWS EC2 (pre-built datasets, clean text) ──┐
                                            ├──→ Final merged Brahm AI knowledge base
GCloud VM (OCR from 394 raw PDFs) ─────────┘
```

---

## Infrastructure

| Resource | Detail |
|----------|--------|
| **Instance** | `m6i.2xlarge` (8 vCPU, 32GB RAM) |
| **Instance ID** | `i-00fe84780437b1d2d` |
| **Name** | `brahm-ai-dataset-hub` |
| **Elastic IP** | `100.51.126.239` |
| **Region** | `us-east-1` (Availability Zone: `us-east-1d`) |
| **OS** | Ubuntu 22.04 LTS |
| **Root Disk** | 8GB gp2 (OS only) |
| **Data Disk** | 300GB gp3 (`/dev/nvme1n1` → mounted at `/data`) |
| **Swap** | 16GB on `/data/swapfile` (moved from root due to space) |
| **Key Pair** | `test` (old `.pem` at `C:\desktop\PhotoGenius AI\ai-pipeline\test.pem`) |
| **Security Group** | `brahm-ai-sg` (SSH port 22, My IP only) |
| **S3 Bucket** | `brahm-ai-datasets` |

---

## SSH Access

```bash
ssh -i "C:\desktop\PhotoGenius AI\ai-pipeline\test.pem" ubuntu@100.51.126.239
```

---

## Server Setup (Completed)

### What's Installed
- **System packages**: python3-pip, python3-venv, git, htop, tmux, unzip, aria2, awscli
- **Python venv**: `/data/env` (auto-activates on login via `.bashrc`)
- **Pip packages**: huggingface_hub, datasets, boto3, requests, beautifulsoup4, lxml, tqdm, gdown, pandas
- **AWS CLI**: configured with access keys, region `us-east-1`

### Folder Structure
```
/data/
├── downloads/          ← raw downloads
│   ├── huggingface/    ← HF datasets (sangraha, cltk, aya, wiki)
│   ├── wikisource/     ← Sanskrit Wikisource dump
│   ├── dcs/            ← Digital Corpus of Sanskrit
│   ├── gutenberg/      ← Project Gutenberg texts
│   ├── gretil/         ← GRETIL Sanskrit collection
│   └── openphilology/  ← Open Philology Sanskrit repos
├── processed/          ← cleaned, ready to merge
│   ├── sanskrit/
│   ├── hindi/
│   ├── english/
│   └── multilingual/
├── scripts/            ← download + processing scripts
├── logs/               ← run logs
├── temp/               ← intermediate files
├── env/                ← Python virtual environment
└── swapfile            ← 16GB swap
```

### Download Script
- **Location**: `/data/scripts/download_all.sh`
- **Run command**: `tmux new -s downloads && cd /data && bash scripts/download_all.sh 2>&1 | tee logs/master.log`
- **Tmux disconnect**: `Ctrl+B`, then `D`
- **Tmux reconnect**: `tmux attach -t downloads`

---

## Datasets — Final Status (as of 2026-03-13)

### Batch 1 — Core Sources (GitHub + Wikimedia)

| # | Source | What | Size | Status | GCloud Overlap? |
|---|--------|------|------|--------|-----------------|
| 1 | GRETIL (GitHub mirror) | 1000+ Sanskrit/Indic texts | 242MB | DONE | NO (GCloud blocked 403) |
| 2 | Open Philology (GitHub) | Sanskrit raw etexts + data repos | 5.6GB | DONE | PARTIAL (SARIT similar but different source) |
| 3 | Wikisource Sanskrit | Sanskrit Wikisource XML dump | 217MB | DONE | NO |

### Batch 1 — HuggingFace Large

| # | Source | What | Size | Status | GCloud Overlap? |
|---|--------|------|------|--------|-----------------|
| 4 | Aya Dataset | Multilingual instruct (Hindi) | 134MB | DONE | NO |
| 5 | Sangraha (Hindi Deva only) | Hindi synthetic corpus (OCR verify) | 18GB | DONE | NO (synthetic) |
| 6 | Hindi Wikipedia | Full Hindi Wikipedia (163K articles) | 642MB | DONE | NO |
| 7 | Sanskrit Wikipedia | Sanskrit Wikipedia | 67MB | DONE | NO |

### Batch 2 — HuggingFace Specialized (Sanskrit/Vedic/Epics)

| # | Source | What | Size | GCloud Overlap? |
|---|--------|------|------|-----------------|
| 8 | JDhruv14/Bhagavad-Gita_Dataset | Gita structured (most popular, 61 likes) | 804KB | YES but format different (clean text vs OCR'd PDF) |
| 9 | JDhruv14/Bhagavad-Gita-QA | Gita Q&A pairs (RAG training) | 7.8MB | NO (Q&A format unique) |
| 10 | Voider22/bhagavad-gita-verses-sanskrit-translations | Sanskrit + multi-lang translations | 35MB | NO (parallel corpus unique) |
| 11 | JDhruv14/Ramayana | Ramayana text | 9.5MB | YES but clean text vs OCR'd PDF |
| 12 | JDhruv14/Mahabharata | Mahabharata text (16 likes) | 40MB | YES but clean text vs OCR'd PDF |
| 13 | sanganaka/ramayana-anvaya | Ramayana word-by-word analysis | 3.9MB | NO (unique annotation) |
| 14 | shunyasea/vedic-sanskrit | Vedic Sanskrit texts | 31MB | PARTIAL (vedas/ PDFs on GCloud) |
| 15 | shunyasea/vedic-sanskrit-sources | Vedic source texts | 11MB | PARTIAL |
| 16 | surajp/sanskrit_classic | Classical Sanskrit | 44KB | NO |
| 17 | chronbmm/sanskrit-monolingual-pretraining | Sanskrit corpus (pretraining) | 812MB | NO |
| 18 | PP04/Sanskrit-Text-Summary | Sanskrit summarization | 3.4MB | NO |
| 19 | mangeshdiyewar/sanskrit_eng | Sanskrit-English parallel | 22MB | NO |
| 20 | ai4bharat/IndicQA | Hindi/Indic QA pairs | 25MB | NO |
| 21 | ai4bharat/IndicParaphrase | Hindi paraphrase pairs | 762MB | NO |

### Deleted (Duplicates / Not Useful)

| Source | Reason |
|--------|--------|
| buddhist-nlp/pali-english-devanagari (25MB) | GCloud has SuttaCentral (306MB, 42K files) — much bigger/better |
| OEvortex/Bhagavad_Gita | Duplicate of JDhruv14 version |
| snskrt/Shrimad_Bhagavad_Gita | Duplicate of JDhruv14 version |
| SatyaSanatan/shrimad-bhagavad-gita-dataset-alpaca | Fine-tuning format only, not RAG useful |
| rahulnyk/mahabharata | Duplicate of JDhruv14 version (smaller) |
| Pretam/ramayana | Duplicate of JDhruv14 version |

### GCloud vs AWS Overlap Analysis

| Content | GCloud (VM) | AWS (EC2) | Both Useful? |
|---------|-------------|-----------|-------------|
| Gita/Ramayana/Mahabharata | Scanned PDFs → OCR'd (original source ref) | Clean structured text + verse numbers | YES — GCloud=authority, AWS=clean RAG chunks |
| Vedic texts | 18 PDFs in vedas/ | HF vedic-sanskrit datasets | YES — complementary |
| Sanskrit corpus | SARIT 85 TEI-XML + 394 PDFs | GRETIL 1000+ texts + Open Philology | YES — different collections |
| Pali/Buddhist | SuttaCentral 306MB (42K JSONs) | Deleted (smaller duplicate) | GCloud only |
| Hindi synthetic | None | Sangraha 18GB | AWS only — OCR verification use |
| Wikipedia | None | Hindi + Sanskrit Wikipedia | AWS only |
| NLP datasets | None | IndicQA, IndicParaphrase, Aya | AWS only |

**Total on disk: ~32GB (48GB with env+swap+processed) | 232GB free | S3 backed up (downloads + processed)**

### Skipped / Failed

| Source | Reason |
|--------|--------|
| DCS (GitHub) | Repo `OliverHellworthy/dcs-data` not found / private — needs alt source |
| CLTK Sanskrit (HuggingFace) | Repo `cltk/sanskrit_texts_raw` doesn't exist on HF (404) |
| CLTK (pip) | Requires PyTorch + CUDA (~700MB+) — root disk too small (8GB) |
| Sangraha (other languages) | Deleted 211GB of non-Hindi langs (Bengali, Tamil, etc.) — not in Brahm AI scope |
| Wikisource (first attempt) | 503 server error, succeeded on retry |

### Sangraha Decision
- Original download: 229GB (22+ languages, all synthetic/AI-generated)
- **Kept only `hin_Deva` (17GB)** — Hindi Devanagari
- Deleted: asm, ben, guj, kan, mal, mar, npi, ory (both Deva and Latn scripts)
- **Why keep Hindi synthetic**: OCR correction/word verification, Hindi knowledge base enrichment, embedding quality improvement
- **Why not for RAG directly**: synthetic = no authentic source references, Brahm AI needs original text citations

---

## Decisions Made & Why

### Why m6i.2xlarge (not t3.large)?
- **t3 = burstable**: sustained CPU usage → throttle after credits exhaust
- **8GB RAM on t3.large**: HuggingFace datasets library loads into RAM → OOM crash on 50GB datasets
- **m6i = dedicated CPU**: no throttle, consistent performance
- **32GB RAM**: safe for large dataset processing with streaming
- **12.5 Gbps network**: 3x faster downloads than t3

### Why 300GB (not 1TB)?
- Realistic dataset total: ~120-150GB extracted
- 300GB = 2x headroom, enough for processing
- EBS resize is online (can increase anytime without downtime)
- Saves ~$56/month vs 1TB

### Why Swap on /data (not root)?
- Root disk is only 8GB — 16GB swap didn't fit
- `/data` is 300GB gp3 — plenty of space
- Swap created at `/data/swapfile` (16GB)
- 32GB RAM + 16GB swap = 48GB effective memory

### Why Elastic IP?
- Without it, public IP changes on every instance stop/start
- Fixed IP = same SSH command every time
- **WARNING**: unattached Elastic IP = $3.65/month charge. Release if deleting instance.

### Why old .pem key (not new)?
- Old `test.pem` already existed and worked
- No security benefit in creating new one for same user
- Key location: `C:\desktop\PhotoGenius AI\ai-pipeline\test.pem`

---

## Issues Encountered & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| Volume won't attach | Created in `us-east-1a`, instance in `us-east-1d` | Deleted volume, recreated in `us-east-1d` |
| `/dev/sdb` already in use | NVMe instances map differently | Used `/dev/sdf` instead |
| `apt update` — No space left | 8GB root disk full (swap was on root) | Moved swap to `/data`, ran `apt clean` |
| `python3 -m venv` — Permission denied | `/data` owned by root after mount | `sudo chown ubuntu:ubuntu /data` |
| `aws` command not found | awscli not pre-installed on Ubuntu 22.04 | `sudo apt install awscli` |
| Swap only 5.8GB (wanted 16GB) | Root disk had no space for 16GB | After moving swap to `/data`, got full 16GB |
| DCS repo not found | `OliverHellworthy/dcs-data` private or deleted | Skip — find alternative source |
| CLTK HF 404 | `cltk/sanskrit_texts_raw` doesn't exist on HuggingFace | Skip |
| CLTK pip install fails | Needs PyTorch+CUDA (700MB+), root disk 8GB full | Skip — root disk too small |
| HuggingFace 401 Unauthorized | Datasets need auth token | `huggingface-cli login` with token |
| Sangraha 229GB disk full (91%) | Dataset much larger than expected (22 langs) | Kept only hin_Deva (17GB), deleted rest |
| Wikisource 503 first attempt | Server temporarily unavailable | Retry succeeded |
| Wikisource OOM during processing | 217MB bz2 → 1-2GB XML loaded into memory at once | Created `fix_wikisource.py` with streaming `iterparse` + `elem.clear()` |
| `ls` argument list too long | 144K wikisource files in one dir | Use `find` instead of `ls` for counting |
| GCloud numpy/FAISS import fail | `numpy._core` not found (version mismatch) | `pip install numpy==1.26.4 faiss-cpu --force-reinstall` |
| GCloud nvidia-smi failed | Cloud kernel headers missing → DKMS couldn't build nvidia.ko | `apt install linux-headers-$(uname -r)` + `modprobe nvidia` |
| 02_build_index.py CUDA crash | `device="cuda"` hardcoded, GPU driver not loaded | Fixed driver (see above), script patched for auto-detect |

---

## Processing — COMPLETED (2026-03-13)

### Processing Script
- **Main**: `/data/scripts/process_all.py` — 7 processors (GRETIL, Open Philology, Wikisource, HF, Wikipedia, GRETIL extras, Aya)
- **Wikisource fix**: `/data/scripts/fix_wikisource.py` — streaming iterparse (original OOM'd on 217MB bz2)
- **Format**: GCloud-compatible JSON (400 token chunks, 50 overlap, word-split)
- **Output**: `/data/processed/{language}/source_name.json`

### Processing Results

| Language | Files | Size | Sources |
|----------|-------|------|---------|
| Sanskrit | ~144,600+ | 4.3GB | GRETIL (9), Open Philology (274), HF vedic/mono (17), Wikisource (144,403) |
| Hindi | ~1,726 | 744MB | Wikipedia Hindi (1,724 batches), Aya (2) |
| Multilingual | 4 | 54MB | Bhagavad Gita, Sanskrit-English parallel (3) |
| English | 0 | 0 | (no English-only datasets) |
| **Total** | **~146,330** | **5.1GB** | **679,037+ chunks** |

### Chunk Stats by Source
```
gretil:             17,470 chunks
openphilology:      47,067 chunks
hf (all datasets):  473,021 chunks
wikipedia_hindi:    132,152 chunks
wikipedia_sanskrit:  8,698 chunks
aya:                   629 chunks
wikisource:         ~144K pages (processed separately)
```

### S3 Sync — DONE
- `aws s3 sync /data/processed/ s3://brahm-ai-datasets/processed/ --storage-class STANDARD_IA`
- 5.1GB uploaded to S3

---

## What's Remaining

### Merge Plan (AWS + GCloud) — DONE (2026-03-13)
1. ~~On GCloud VM: install awscli, configure keys~~ DONE
2. ~~Pull: `aws s3 sync s3://brahm-ai-datasets/processed/ ~/books/data/aws_datasets/`~~ DONE (5.1GB, 146,444 files)
3. ~~Combine AWS datasets (146K files) + OCR'd PDFs (366 JSONs) into unified index~~ DONE
4. ~~FAISS index build~~ DONE (1.1M chunks → 4 indexes in 33.7 min on L4 GPU)
5. Test RAG quality with combined data — NEXT

### GCloud Merge Issues & Fixes (2026-03-13 to 2026-03-14)

| Issue | Cause | Fix |
|-------|-------|-----|
| `aws s3 ls` showed only 1 object | Missing `--recursive` flag | Added `--recursive` to see all 146K files |
| numpy/FAISS import error on GCloud | `numpy._core` module not found (version mismatch) | `pip install numpy==1.26.4 faiss-cpu --force-reinstall` |
| PaddleX numpy warning | paddlex requires 1.24.4, we installed 1.26.4 | Ignored — OCR processing already complete |
| `gcloud compute scp` from VM | User ran scp FROM inside VM (wrong direction) | Must run from local PowerShell, not VM SSH |
| 02_build_index.py CUDA crash | `device="cuda"` hardcoded but GPU driver not loaded | See GPU driver fix below |
| nvidia-smi failed | Kernel module not built (missing cloud kernel headers) | `apt install linux-headers-$(uname -r)` → DKMS auto-built nvidia.ko |
| `nvidia-driver-535` not found | Package not in Debian 12 repos | Used `cuda-keyring` from NVIDIA repos + cloud headers fix |
| tmux duplicate session | `tmux new -s index` when session already exists | `tmux attach -t index` or kill old session first |

### NVIDIA GPU Driver Fix (2026-03-14)
```
Problem: GPU physically present but nvidia-smi couldn't communicate with driver
- lspci showed: "NVIDIA Corporation AD104GL [L4]" (hardware OK)
- modprobe nvidia → "Module not found in /lib/modules/6.1.0-44-cloud-amd64"
- Root cause: linux-headers-6.1.0-44-amd64 installed, but kernel is 6.1.0-44-CLOUD-amd64

Fix:
  sudo apt install -y linux-headers-$(uname -r)   # gets cloud variant
  # DKMS auto-triggered → built nvidia 595.45.04 for cloud kernel
  sudo modprobe nvidia && nvidia-smi              # SUCCESS: L4 24GB, CUDA 13.2
```

### FAISS Index Build — COMPLETED (2026-03-14)
```
Script: ~/books/scripts/02_build_index.py
Model: sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 (384 dim)
Device: NVIDIA L4 24GB GPU (CUDA 13.2), batch_size=512

Files collected: 146,809 JSONs
  - GCloud OCR'd PDFs: 366 files
  - AWS Sanskrit: 144,835 files
  - AWS Hindi: 1,604 files
  - AWS Multilingual: 4 files

Chunks loaded: 1,104,518 (in 48 seconds)
  - Sanskrit: 791,691
  - Hindi: 178,809
  - English: 134,018
  - Multilingual: 0

Encoding: 2,158 batches, 25 min 32 sec (1672s) on GPU
Total build time: 33.7 min (2024 seconds)

Output: ~/books/indexes/
  master.index:   1,104,518 vectors (all languages)
  sanskrit.index: 791,691 vectors (1365.2 MB)
  hindi.index:    178,809 vectors (308.3 MB)
  english.index:  134,018 vectors (231.1 MB)
  metadata.db:    SQLite with 1,104,518 chunk records
  documents.jsonl: full text store for retrieval
  Total index size: ~1.9 GB

Status: DONE ✓
```

### GCloud OCR Status (as of 2026-03-14)
- **363+ JSONs processed** out of 394 PDFs
- BATCH A/B/C: DONE (special_jyotish, grammar, smriti, bauddha, panchang, yoga, samudrika, muhurta, vastu, festivals, mantra, dharma)
- BATCH C+ (RUNNING): gotra, lal_kitab, mixed, nakshatra_jyotish + remaining smriti/bauddha
- **Page-by-page OCR fix**: pdf2image now converts 1 page at a time (prevents OOM on large PDFs)
- SARIT XML extractor (85 TEI-XML files) — TODO
- SuttaCentral JSON extractor (42K files) — TODO

### Future Datasets to Add
- OSCAR Hindi corpus (~20GB web-crawled Hindi)
- Sanskrit Heritage Dictionary (digital)
- Monier-Williams Sanskrit Dictionary
- DCS (Digital Corpus of Sanskrit) — find working mirror
- Vedabase texts
- More HuggingFace Indic datasets as they release

---

## Cost Management

| Resource | Monthly Cost | When to Pay |
|----------|-------------|-------------|
| EC2 m6i.2xlarge | $0.38/hr (only when running) | Stop when not using |
| EBS 300GB gp3 | ~$24/month | Always (even when stopped) |
| S3 (est. 100GB) | ~$2.30/month | Always |
| Elastic IP | $0 (attached) / $3.65 (unattached) | Release if deleting instance |
| **Total (active 8hr/day)** | **~$117/month** | |
| **Total (stopped, storage only)** | **~$27/month** | |

### Cost Rules
- **STOP instance when not downloading** — EC2 = biggest cost
- After downloads done: consider **reducing EBS to 100GB** (move data to S3 first)
- Spot instance option: same m6i.2xlarge at ~$0.12/hr (60-70% off) for future runs

---

## Quick Commands Reference

```bash
# SSH connect
ssh -i "C:\desktop\PhotoGenius AI\ai-pipeline\test.pem" ubuntu@100.51.126.239

# Tmux
tmux new -s downloads          # new session
tmux attach -t downloads       # reconnect
# Ctrl+B, D                    # disconnect

# Monitor
htop                           # RAM/CPU
df -h /data                    # disk usage
du -sh /data/downloads/*/       # per-source size
ls /data/downloads/

# S3
aws s3 sync /data/downloads/ s3://brahm-ai-datasets/downloads/
aws s3 ls s3://brahm-ai-datasets/ --recursive --summarize

 # Count wikisource files using find
find /data/processed/sanskrit/ -name "wikisource_*" | wc -l

du -sh /data/processed/*/

# Instance control (from local machine)
# EC2 Console → Stop/Start instance
```
