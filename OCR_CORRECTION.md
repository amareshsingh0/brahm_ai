# Post-OCR Correction Pipeline — FINAL
# Brahm AI - Maximum Accuracy Text Quality System
# Status: PLANNING (Start after BATCH B+C ingestion complete)

# ============================================================
# GOAL: BEST POSSIBLE accuracy. 3-Layer system:
#   Layer 1: Tesseract/PaddleOCR (free, fast, 90%+ pages)
#   Layer 2: Dictionary + SymSpell correction (free, offline)
#   Layer 3: Google Cloud Vision re-OCR (free credits, worst pages only)
# Zero Anthropic/Claude API. Google Cloud $300 credits ka smart use.
# ============================================================


## Accuracy Targets

```
                         BEFORE          AFTER Layer 2      AFTER Layer 3
                         (Raw OCR)       (Dict Correct)     (+ GCV Re-OCR)
─────────────────────────────────────────────────────────────────────────
Text PDFs (pypdf):       99%+            99%+               99%+
English scan (Paddle):   95-98%          98-99%             99%+
Devanagari (Tesseract):  90-95%          95-97%             97-99%
Old manuscripts:         60-80%          75-85%             85-95%
Mixed script pages:      75-85%          80-90%             92-97%
─────────────────────────────────────────────────────────────────────────
OVERALL AVERAGE:         ~88%            ~93%               ~97%+
```


## Blueprint Gap

Master Blueprint (Sections 17, 19, 20) has NO post-OCR correction planned.
- Section 19 "What's Missing": OCR = "pip install pytesseract" (basic)
- Section 17 "Clean" phase: only "headers, footers, page nums" removal
- Section 20 Roadmap: no dictionary/NLP correction at any phase
- Our plan fills this gap with 3-layer hybrid system + Google Cloud credits


## Final Architecture — 3-Layer Hybrid

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BRAHM AI OCR CORRECTION PIPELINE                  │
│                    Maximum Accuracy, Minimum Cost                    │
└─────────────────────────────────────────────────────────────────────┘

LAYER 1: PRIMARY OCR (Already Done — Ingestion Pipeline)
═══════════════════════════════════════════════════════════
  Text PDF ──→ pypdf (direct extract)    → 99%+ accuracy ✅
  Scanned:
    English    ──→ PaddleOCR (GPU)       → 95-98% accuracy ✅
    Devanagari ──→ Tesseract (hin+san)   → 90-95% accuracy ⚠️
    Tamil/Tel  ──→ Tesseract (tam/tel)   → 85-92% accuracy ⚠️
    Old prints ──→ Tesseract             → 60-80% accuracy ❌

         ↓ All pages saved as JSON chunks ↓

LAYER 2: OFFLINE DICTIONARY CORRECTION (FREE)
═══════════════════════════════════════════════
  For EVERY chunk:
    Step 1: Score chunk (% words in dictionary)
    Step 2: If score < 0.80 → apply corrections:
      ├── SymSpell (edit distance 1-2, 1M words/sec)
      ├── Devanagari OCR Error Map (म↔भ, ध↔घ, etc.)
      ├── Matra/Halant fixes (ाे→ो, nukta cleanup)
      └── Indic NLP tokenizer (better word boundaries)
    Step 3: Re-score after correction
    Step 4: If STILL score < 0.50 → flag for Layer 3

  Cost: $0 | Speed: ~50,000 chunks/minute | Accuracy gain: +3-7%

         ↓ Flagged BAD pages (est. ~2,000-5,000 pages) ↓

LAYER 3: GOOGLE CLOUD VISION RE-OCR (FREE CREDITS)
═══════════════════════════════════════════════════════
  ONLY for pages that Layer 2 couldn't fix (score < 0.50)
    Step 1: Extract original page image from PDF
    Step 2: Send to Google Cloud Vision document_text_detection
    Step 3: Replace chunk text with GCV output
    Step 4: Re-run Layer 2 normalization on GCV text
    Step 5: Score again → if still bad, mark as "low_confidence"

  Cost: ~$3-8 from free credits | Pages: ~2,000-5,000 only
  Google Vision Devanagari: 97-99% (much better than Tesseract)

         ↓ Clean, verified JSONs ↓

  02_build_embeddings.py → FAISS Index → Search Pipeline
```


## Pipeline Position

```
01_ingest_books.py  →  01b_correct_ocr.py  →  01c_gcv_reocr.py  →  02_build_embeddings.py
(Layer 1: OCR)         (Layer 2: Dict Fix)    (Layer 3: GCV)       (Vectors)
     ↓                       ↓                      ↓                    ↓
Raw JSON               Corrected JSON         Best JSON             FAISS Index
(may have errors)      (dict-verified)        (GCV-enhanced)        (clean vectors)
```

### Files
```
src/ingestion/ocr_corrector.py        # Layer 2: Dictionary correction logic
src/ingestion/gcv_reocr.py            # Layer 3: Google Cloud Vision re-OCR
scripts/build_word_dict.py            # Build frequency dicts from good JSONs
scripts/01b_correct_ocr.py            # CLI: Run Layer 2 on all JSONs
scripts/01c_gcv_reocr.py              # CLI: Run Layer 3 on flagged pages
data/dictionaries/                    # Word frequency dicts (auto-built)
  hindi_freq.json                     # Hindi word frequencies
  sanskrit_freq.json                  # Sanskrit word frequencies
  english_freq.json                   # English word frequencies
  devanagari_ocr_errors.json          # Tesseract confusion pairs
```


## Layer 2: Dictionary Correction (Detail)

### Method 1: Word Frequency Dictionary (Primary)
```
SOURCE: 274+ good text-PDF JSONs (99%+ accurate, already processed)
BUILD:  Extract all words → count frequency → save as {word: count}
USE:    For each OCR word, check if exists in dict
        If not found → candidate for correction

Example:
  Dict has: "ब्रह्म" (freq: 4521), "धर्म" (freq: 8932)
  OCR gives: "ब्रद्म" → not in dict → correct to "ब्रह्म" (edit distance 1)
```

### Method 2: SymSpell (Already Installed on VM)
```
TOOL:   symspellpy — ALREADY on VM
SPEED:  1M words/sec correction speed
METHOD: Symmetric delete algorithm, edit distance 1-2
USE:    Load frequency dict → symspell.lookup(word, max_edit_distance=2)

Code:
  from symspellpy import SymSpell
  sym = SymSpell(max_dictionary_edit_distance=2)
  sym.load_dictionary("hindi_freq.txt", 0, 1)
  suggestions = sym.lookup(word, Verbosity.CLOSEST, max_edit_distance=2)
```

### Method 3: Devanagari OCR Error Map
```
COMMON TESSERACT CONFUSIONS (Devanagari):
  म ↔ भ    (similar shape)
  ख ↔ रव   (combined looks similar)
  घ ↔ पा   (OCR merges/splits)
  ध ↔ घ    (similar curves)
  थ ↔ या   (common misread)
  श ↔ ज    (similar in low res)
  ष ↔ प    (OCR confusion)
  ण ↔ ल    (similar bottom curve)
  क्ष ↔ झ  (complex conjunct)
  त्र ↔ व  (conjunct misread)

MATRA FIXES (already in normalizer.py):
  ाे → ो   (split matra → combined)
  ाै → ौ   (split matra → combined)

HALANT/NUKTA:
  प़ → प    (nukta removal for consistency)
  क़ → क    (if not Urdu loanword)
```

### Method 4: Indic NLP Library (Better Tokenization)
```
TOOL:   indic-nlp-library (pip install indic-nlp-library)
WHY:    Default split() breaks Devanagari conjuncts wrong
        Indic NLP gives proper word boundaries for Hindi/Sanskrit
USE:    Better tokenization → better dictionary lookup → fewer false flags

Code:
  from indicnlp.tokenize import indic_tokenize
  words = indic_tokenize.trivial_tokenize(text, lang='hi')
```


## Layer 3: Google Cloud Vision Re-OCR (Detail)

### Setup
```bash
# On VM (one-time setup):
pip install google-cloud-vision
# Upload service account key JSON (from Google Cloud Console)
export GOOGLE_APPLICATION_CREDENTIALS="~/gcp-key.json"
```

### How It Works
```python
from google.cloud import vision
import io

def gcv_ocr_page(image_path: str) -> str:
    """Re-OCR a single page using Google Cloud Vision."""
    client = vision.ImageAnnotatorClient()

    with open(image_path, 'rb') as f:
        content = f.read()

    image = vision.Image(content=content)

    # document_text_detection = best for printed text (structured)
    response = client.document_text_detection(image=image)

    if response.error.message:
        raise Exception(response.error.message)

    return response.full_text_annotation.text


def gcv_ocr_pdf_pages(pdf_path: str, page_numbers: list[int]) -> dict:
    """Re-OCR specific pages from a PDF using GCV.

    Only called for pages where Layer 2 score < 0.50.
    Returns {page_num: ocr_text} dict.
    """
    from pdf2image import convert_from_path
    import tempfile

    results = {}
    images = convert_from_path(pdf_path, dpi=300, first_page=min(page_numbers),
                                last_page=max(page_numbers))

    for page_num, img in zip(page_numbers, images):
        if page_num not in page_numbers:
            continue
        # Save temp image
        with tempfile.NamedTemporaryFile(suffix='.png', delete=False) as tmp:
            img.save(tmp.name, 'PNG')
            text = gcv_ocr_page(tmp.name)
            results[page_num] = text

    return results
```

### Smart Routing — Only Bad Pages
```python
def decide_gcv_reocr(correction_report: dict) -> list:
    """From Layer 2 report, pick pages that need GCV re-OCR.

    Selection criteria:
      - Chunk score < 0.50 after Layer 2 correction
      - Page is from scanned PDF (not text PDF)
      - Estimated: ~5-10% of scanned pages
    """
    pages_to_reocr = []
    for book in correction_report['books']:
        if book['pdf_type'] == 'text':
            continue  # text PDFs don't need re-OCR
        for chunk in book['chunks']:
            if chunk['score_after_correction'] < 0.50:
                pages_to_reocr.append({
                    'pdf_path': book['filepath'],
                    'page_num': chunk['page'],
                    'current_score': chunk['score_after_correction'],
                    'book': book['source'],
                })
    return pages_to_reocr
```

### GCV Language Hints (Better Accuracy)
```python
# Google Vision supports language hints for better detection
from google.cloud import vision

image_context = vision.ImageContext(
    language_hints=['sa', 'hi', 'en']  # Sanskrit, Hindi, English
)
response = client.document_text_detection(
    image=image,
    image_context=image_context
)
```


## Confidence Scoring System

```python
def score_chunk(chunk_text: str, word_dict: dict) -> dict:
    """Score a chunk's text quality against known dictionary."""
    words = chunk_text.split()
    if not words:
        return {"score": 0, "grade": "EMPTY", "total": 0, "known": 0}

    known = sum(1 for w in words if w in word_dict or len(w) <= 2)
    score = known / len(words)

    if score >= 0.80:
        grade = "GOOD"         # Clean text, no correction needed
    elif score >= 0.50:
        grade = "FIXABLE"      # Layer 2 can fix this
    elif score >= 0.30:
        grade = "GCV_NEEDED"   # Send to Google Cloud Vision
    else:
        grade = "BAD"          # Even GCV may struggle, flag for review

    return {"score": round(score, 3), "grade": grade,
            "total": len(words), "known": known}
```

### Action by Grade
```
GOOD (80%+):       Skip correction. Already clean.             → ~70% of chunks
FIXABLE (50-80%):  Layer 2: SymSpell + OCR error map.          → ~20% of chunks
GCV_NEEDED (30-50%): Layer 3: Google Vision re-OCR.            → ~8% of chunks
BAD (<30%):        Flag for review. Mark "low_confidence".     → ~2% of chunks
                   These are usually cover pages, images,
                   handwritten sections, or damaged scans.
```


## Cost Breakdown — Google Cloud Credits

```
GOOGLE CLOUD VISION PRICING:
  First 1,000 units/month:  FREE (always free tier)
  1,001-5M units/month:     $1.50 per 1,000 pages

OUR USAGE ESTIMATE:
  Total pages across 394 PDFs:       ~78,000 pages
  Text PDFs (no re-OCR needed):      ~50,000 pages  → $0
  Scanned pages (Layer 1+2 OK):      ~24,000 pages  → $0
  Pages needing GCV (score < 0.50):  ~4,000 pages   → $6.00

  TOTAL GCV COST: ~$6 from $300 free credits
  REMAINING CREDITS: ~$294 for future use (embeddings, compute, etc.)

COMPARE ALTERNATIVES:
  Google Cloud Vision (our plan):    $6     for 4,000 worst pages
  Claude Vision API (all pages):     $150+  for 78,000 pages
  AWS Textract:                      $117   for 78,000 pages
  Azure AI Vision:                   $78    for 78,000 pages
  Our hybrid approach:               $6     BEST VALUE ✅
```


## Implementation Plan — FINAL

### Step 0: Setup Google Cloud Vision (One-Time)
```bash
# On VM:
pip install google-cloud-vision

# In Google Cloud Console:
# 1. Enable "Cloud Vision API" (APIs & Services → Enable APIs)
# 2. Create Service Account (IAM → Service Accounts → Create)
# 3. Download JSON key file
# 4. Upload to VM:
gcloud compute scp gcp-key.json amareshsingh2005@brahm-ai-vm:~/

# 5. Set environment variable:
echo 'export GOOGLE_APPLICATION_CREDENTIALS="$HOME/gcp-key.json"' >> ~/.bashrc
source ~/.bashrc

# 6. Test:
python3 -c "from google.cloud import vision; print('GCV OK')"
```

### Step 1: Build Word Frequency Dictionaries
```bash
# Script: scripts/build_word_dict.py
# Input: All processed JSONs from text PDFs (high quality)
# Output: data/dictionaries/{lang}_freq.json

python3 scripts/build_word_dict.py \
  --json-dir ~/books/data/processed/json \
  --output-dir ~/books/data/dictionaries \
  --min-freq 2
```

### Step 2: Build OCR Error Map
```bash
# Compare Tesseract output vs known-good text for same pages
# Store: data/dictionaries/devanagari_ocr_errors.json
# Also auto-detect common patterns from correction attempts
```

### Step 3: Run Layer 2 — Dictionary Correction (Dry Run First)
```bash
python3 scripts/01b_correct_ocr.py \
  --json-dir ~/books/data/processed/json \
  --dict-dir ~/books/data/dictionaries \
  --mode auto \
  --dry-run     # FIRST: just score + report, no changes
# Review correction_report.json
# Then run without --dry-run to apply
```

### Step 4: Run Layer 3 — Google Vision Re-OCR (Flagged Pages Only)
```bash
python3 scripts/01c_gcv_reocr.py \
  --report ~/books/data/processed/logs/correction_report.json \
  --json-dir ~/books/data/processed/json \
  --min-grade GCV_NEEDED \
  --dry-run     # FIRST: estimate cost + page count
# Review estimate, then run without --dry-run
```

### Step 5: Final Quality Report
```bash
python3 scripts/01b_correct_ocr.py \
  --json-dir ~/books/data/processed/json \
  --dict-dir ~/books/data/dictionaries \
  --report-only   # Just score, no corrections
# Output: final_quality_report.json
# Shows: per-book scores, overall accuracy, remaining BAD chunks
```

### Step 6: Proceed to Embeddings
```bash
python3 scripts/02_build_embeddings.py   # On corrected, verified JSONs
```


## Execution Timeline — FINAL

```
PHASE                              WHEN                    TIME        COST
──────────────────────────────────────────────────────────────────────────────
1. Complete ingestion (BATCH B+C)  Now (running)           ~6-8 hrs    $0
2. Setup GCV on VM                 After batches done      10 min      $0
3. Build word frequency dicts      After ingestion done    15 min      $0
4. Layer 2 dry run + review        After dict build        30 min      $0
5. Layer 2 apply corrections       After review            20 min      $0
6. Layer 3 dry run (cost estimate) After Layer 2           10 min      $0
7. Layer 3 GCV re-OCR              After Layer 3 review    1-2 hrs     ~$6
8. Final quality report            After all layers        15 min      $0
9. Build embeddings (02_embed)     After quality verified  2-3 hrs     $0
──────────────────────────────────────────────────────────────────────────────
TOTAL:                                                     ~12-16 hrs  ~$6
```


## Dependencies — FINAL

```
ALREADY INSTALLED ON VM:
  - symspellpy          (SymSpell spell correction)
  - regex               (Unicode-aware regex)
  - pytesseract         (Tesseract OCR)
  - paddleocr           (PaddleOCR)
  - pdf2image           (PDF to images for GCV)

NEED TO INSTALL:
  - google-cloud-vision (pip install google-cloud-vision)
  - indic-nlp-library   (pip install indic-nlp-library)

NEED TO SETUP:
  - GCP Service Account key (for Vision API)
  - Enable Cloud Vision API in Google Cloud Console

NOT NEEDED:
  - Anthropic/Claude API     (no LLM for OCR)
  - OpenAI API               (not needed)
  - AWS Textract             (Google credits available)
```


## Why This Is The BEST Approach

```
1. COST EFFICIENCY:
   - 95% pages: FREE (Tesseract/Paddle + Dictionary)
   - 5% pages:  ~$6 (Google Vision, from $300 free credits)
   - vs $150+ if we sent everything to Cloud Vision

2. ACCURACY:
   - Layer 1 alone:  ~88% average
   - + Layer 2:      ~93% average (+5%)
   - + Layer 3:      ~97% average (+4%)
   - Best possible without manual human review

3. SPEED:
   - Layer 2: ~50,000 chunks/minute (offline, CPU)
   - Layer 3: ~10 pages/sec (API, parallel)
   - Total correction: ~2-3 hours

4. NO VENDOR LOCK-IN:
   - Core pipeline is 100% offline
   - Google Vision only for worst cases
   - Can replace with any OCR API later

5. SMART CREDIT USE:
   - $6 out of $300 for OCR
   - Remaining $294: VM compute, future APIs, storage
```


## Remaining $294 Credits — Future Use Ideas

```
PRIORITY  USE CASE                              EST. COST
─────────────────────────────────────────────────────────
HIGH      VM compute (BATCH B+C + embedding)    $50-80
HIGH      Google Vision OCR (Layer 3)           $6
MED       Cloud Storage backup (GCS bucket)     $5/month
MED       Future book OCR (new downloads)       $10-20
LOW       Text-to-Speech API (future feature)   $20-30
LOW       Translation API (cross-lang verify)   $10-15
─────────────────────────────────────────────────────────
ESTIMATED TOTAL:                                $100-150
REMAINING AFTER ALL:                            ~$150-200
```


## Update Log

| Date | Update |
|------|--------|
| 2026-03-12 | Initial plan created. Blueprint gap identified (no post-OCR correction). |
| 2026-03-12 | Decided: run AFTER ingestion, BEFORE embedding. |
| 2026-03-12 | UPGRADED to 3-Layer hybrid: Tesseract + Dictionary + Google Cloud Vision. |
| 2026-03-12 | Added GCV setup, smart routing (only bad pages), cost estimate ($6 of $300). |
| 2026-03-12 | Final pipeline: 01_ingest → 01b_correct → 01c_gcv_reocr → 02_embed. |
| 2026-03-13 | Page-by-page OCR fix deployed (pdf2image convert 1 page at a time — prevents OOM hang on large PDFs). |
| 2026-03-13 | Ingestion progress: 355+ JSONs. BATCH A/B/C done, final 6 processes running (BATCH C+). |
| | |
