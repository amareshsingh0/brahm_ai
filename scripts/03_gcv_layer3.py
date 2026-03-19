#!/usr/bin/env python3
"""
Brahm AI - OCR Layer 3: Google Cloud Vision
Re-OCRs worst scanned PDFs using GCV API.

Target: books with avg_score 0.15-0.55 from correction_report.json
These are Tesseract failures that GCV can significantly improve.

GCV uses DOCUMENT_TEXT_DETECTION which handles Devanagari/Sanskrit/Hindi
dramatically better than Tesseract for degraded or printed manuscripts.

Setup on VM:
    pip install google-cloud-vision pdf2image
    # GCP credentials auto-detected from VM service account
    # Enable Vision API: gcloud services enable vision.googleapis.com

Run:
    cd ~/books
    source ~/ai-env/bin/activate

    # Dry run - see which books will be processed:
    python3 scripts/03_gcv_layer3.py --dry-run

    # Process all target books (asks confirmation first):
    python3 scripts/03_gcv_layer3.py

    # Limit to N books for testing (cheapest way to verify):
    python3 scripts/03_gcv_layer3.py --limit 5

    # Skip confirmation prompt:
    python3 scripts/03_gcv_layer3.py --yes

    # Custom score range:
    python3 scripts/03_gcv_layer3.py --min-score 0.10 --max-score 0.45
"""

import os
import sys
import json
import time
import argparse
import logging
from pathlib import Path
from typing import Optional

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

BASE_DIR   = os.path.expanduser("~/books")
JSON_DIR   = os.path.join(BASE_DIR, "data/processed/json")
RAW_DIR    = os.path.join(BASE_DIR, "data/raw")
DICT_DIR   = os.path.join(BASE_DIR, "data/dictionaries")
LOG_DIR    = os.path.join(BASE_DIR, "data/processed/logs")
REPORT_PATH = os.path.join(LOG_DIR, "correction_report.json")

# Score thresholds (book-level avg_score from correction_report.json)
TARGET_MIN_SCORE     = 0.15   # Below this: image quality too bad for GCV to help
TARGET_MAX_SCORE     = 0.55   # Above this: already acceptable, skip

# Chunk-level: only re-OCR chunks whose individual score is below this
CHUNK_SCORE_THRESHOLD = 0.55

# Only save improved text if GCV raised the score by at least this much
MIN_SCORE_IMPROVEMENT = 0.10

# GCV pricing: Document Text Detection = $1.50 / 1000 pages
GCV_COST_PER_PAGE = 0.0015

# pdf2image resolution — 200 DPI is good balance of quality vs speed
PDF_DPI = 200

# ---------------------------------------------------------------------------
# Logging setup
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("gcv_layer3")


# ---------------------------------------------------------------------------
# PDF lookup
# ---------------------------------------------------------------------------

def find_pdf(source_name: str, raw_dir: str) -> Optional[Path]:
    """
    Locate the source PDF on disk.

    source_name may be:
      - a relative path like  "puranas/SrimadBhagavatam.pdf"
      - just a basename       "SrimadBhagavatam.pdf"
      - a full abs path       "/home/.../books/raw/puranas/SrimadBhagavatam.pdf"

    Search order:
      1. raw_dir / source_name        (relative path as stored in JSON)
      2. Recursive glob on basename   (book moved to a different sub-folder)
    """
    raw_path = Path(raw_dir)

    # 1. Direct join
    candidate = raw_path / source_name
    if candidate.exists():
        return candidate

    # 2. source_name might be an absolute path already
    abs_candidate = Path(source_name)
    if abs_candidate.exists():
        return abs_candidate

    # 3. Recursive glob by basename
    basename = Path(source_name).name
    if not basename.endswith(".pdf"):
        basename = basename + ".pdf"

    matches = list(raw_path.rglob(basename))
    if matches:
        # Prefer exact case match
        for m in matches:
            if m.name == basename:
                return m
        return matches[0]

    # 4. Case-insensitive fallback (some filesystems are case-sensitive)
    basename_lower = basename.lower()
    for m in raw_path.rglob("*.pdf"):
        if m.name.lower() == basename_lower:
            return m

    return None


# ---------------------------------------------------------------------------
# GCV OCR
# ---------------------------------------------------------------------------

def gcv_ocr_page(image_bytes: bytes, gcv_client) -> str:
    """
    Send a single page image to Google Cloud Vision DOCUMENT_TEXT_DETECTION.

    Uses document_text_detection (not text_detection) because it is optimised
    for dense, multi-column, mixed-script documents — exactly what Sanskrit/
    Hindi manuscripts look like.

    Returns the full extracted text, or "" on failure.
    """
    try:
        from google.cloud import vision
        image = vision.Image(content=image_bytes)
        response = gcv_client.document_text_detection(image=image)

        if response.error.message:
            logger.warning("GCV error for page: %s", response.error.message)
            return ""

        annotation = response.full_text_annotation
        if annotation:
            return annotation.text
        return ""
    except Exception as exc:
        logger.warning("GCV API exception: %s", exc)
        return ""


# ---------------------------------------------------------------------------
# Text scoring  (mirrors 01b_correct_ocr.py logic exactly)
# ---------------------------------------------------------------------------

def score_text(text: str, word_dict: set) -> float:
    """
    Score text quality as ratio of recognised words.

    A word is 'known' if:
      - it appears in word_dict (case-insensitive), OR
      - it is very short (<= 2 chars, e.g. particles, conjunctions), OR
      - it is a pure digit string (page numbers, verse numbers)

    Returns 0.0 – 1.0.
    """
    words = text.split()
    if not words:
        return 0.0
    known = sum(
        1 for w in words
        if w.lower() in word_dict or len(w) <= 2 or w.isdigit()
    )
    return known / len(words)


# ---------------------------------------------------------------------------
# Dictionary loader
# ---------------------------------------------------------------------------

def load_hindi_dict(dict_dir: str) -> set:
    """
    Load hindi_freq.json and return the word set for fast O(1) lookup.

    Falls back gracefully if the file does not exist (score_text will then
    rely only on length/digit heuristics, which still filters pure garbage).
    """
    path = os.path.join(dict_dir, "hindi_freq.json")
    if not os.path.exists(path):
        logger.warning(
            "hindi_freq.json not found at %s — "
            "run build_word_dict.py first for better scoring", path
        )
        return set()
    with open(path, "r", encoding="utf-8") as f:
        freq = json.load(f)
    word_set = set(freq.keys())
    logger.info("Loaded hindi dict: %d words", len(word_set))
    return word_set


def load_english_dict(dict_dir: str) -> set:
    """Load english_freq.json for English chunks."""
    path = os.path.join(dict_dir, "english_freq.json")
    if not os.path.exists(path):
        return set()
    with open(path, "r", encoding="utf-8") as f:
        freq = json.load(f)
    return set(freq.keys())


# ---------------------------------------------------------------------------
# Language detection (same heuristic as rest of the pipeline)
# ---------------------------------------------------------------------------

def detect_lang(text: str, chunk_lang: str = None) -> str:
    """Return 'hindi' (covers Sanskrit+Hindi) or 'english'."""
    if chunk_lang in ("hindi", "sanskrit"):
        return "hindi"
    if chunk_lang == "english":
        return "english"
    devanagari = sum(1 for c in text if "\u0900" <= c <= "\u097F")
    if devanagari / max(len(text), 1) > 0.2:
        return "hindi"
    return "english"


# ---------------------------------------------------------------------------
# Per-book processing
# ---------------------------------------------------------------------------

def process_book(
    json_path: Path,
    raw_dir: str,
    gcv_client,
    word_dicts: dict,
    dry_run: bool,
    page_cache: Optional[dict] = None,
) -> dict:
    """
    Re-OCR a single book JSON with GCV for all low-scoring chunks.

    page_cache: shared dict  { pdf_path_str -> { page_num -> gcv_text } }
                Used so that multiple chunks sharing a page don't trigger
                duplicate GCV API calls.

    Returns a stats dict.
    """
    if page_cache is None:
        page_cache = {}

    # ---- Load JSON ----------------------------------------------------------
    try:
        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as exc:
        return {"error": str(exc), "path": str(json_path)}

    chunks = data.get("chunks", [])
    if not chunks:
        return {"skipped": True, "reason": "no chunks", "path": str(json_path)}

    source = data.get("source", str(json_path.stem))

    # ---- Find PDF -----------------------------------------------------------
    # source field may be "category/BookName" or a full path
    # Try adding .pdf extension variants
    pdf_path = find_pdf(source, raw_dir)
    if pdf_path is None:
        # Also try stripping leading directory components
        pdf_path = find_pdf(Path(source).name, raw_dir)
    if pdf_path is None:
        logger.warning("  PDF not found for source '%s' — skipping", source)
        return {
            "source": source,
            "skipped": True,
            "reason": "pdf_not_found",
            "path": str(json_path),
        }

    pdf_key = str(pdf_path)
    if pdf_key not in page_cache:
        page_cache[pdf_key] = {}

    # ---- Import pdf2image lazily -------------------------------------------
    try:
        from pdf2image import convert_from_path
    except ImportError:
        logger.error("pdf2image not installed. Run: pip install pdf2image")
        return {"error": "pdf2image not installed"}

    # ---- Process chunks -----------------------------------------------------
    pages_sent   = 0
    chunks_improved = 0
    chunks_checked  = 0
    scores_before   = []
    scores_after    = []

    for idx, chunk in enumerate(chunks):
        text = chunk.get("text", "")
        if not text:
            continue

        chunk_lang = chunk.get("language", "unknown")
        lang = detect_lang(text, chunk_lang)
        word_dict = word_dicts.get(lang, set())

        score_before = score_text(text, word_dict)
        scores_before.append(score_before)

        # Only re-OCR chunks that are still low quality
        if score_before >= CHUNK_SCORE_THRESHOLD:
            scores_after.append(score_before)
            continue

        chunks_checked += 1
        page_num = chunk.get("page_num", None)

        if page_num is None:
            # No page metadata — can't target specific page
            scores_after.append(score_before)
            continue

        # ---- Get/cache GCV text for this page -------------------------------
        if page_num not in page_cache[pdf_key]:
            if dry_run:
                # In dry-run: just record that we would have sent this page
                pages_sent += 1
                page_cache[pdf_key][page_num] = None  # sentinel
                scores_after.append(score_before)
                continue

            # Convert single page to image (page-by-page to avoid OOM)
            try:
                images = convert_from_path(
                    str(pdf_path),
                    dpi=PDF_DPI,
                    first_page=page_num,
                    last_page=page_num,
                )
            except Exception as exc:
                logger.warning(
                    "  pdf2image failed page %d of %s: %s", page_num, pdf_path.name, exc
                )
                page_cache[pdf_key][page_num] = ""
                scores_after.append(score_before)
                continue

            if not images:
                page_cache[pdf_key][page_num] = ""
                scores_after.append(score_before)
                continue

            # Convert PIL image to bytes
            import io
            img_byte_buf = io.BytesIO()
            images[0].save(img_byte_buf, format="PNG")
            image_bytes = img_byte_buf.getvalue()

            gcv_text = gcv_ocr_page(image_bytes, gcv_client)
            page_cache[pdf_key][page_num] = gcv_text
            pages_sent += 1

            # Brief pause to stay within GCV rate limits (600 req/min)
            time.sleep(0.12)
        else:
            gcv_text = page_cache[pdf_key][page_num]

        if gcv_text is None:
            # dry-run sentinel
            scores_after.append(score_before)
            continue

        if not gcv_text:
            scores_after.append(score_before)
            continue

        # ---- Score GCV result -----------------------------------------------
        score_after = score_text(gcv_text, word_dict)

        improvement = score_after - score_before
        if improvement >= MIN_SCORE_IMPROVEMENT:
            chunks_improved += 1
            if not dry_run:
                chunks[idx]["text"] = gcv_text
                chunks[idx]["gcv_layer3"] = True
                chunks[idx]["score_before_gcv"] = round(score_before, 3)
                chunks[idx]["score_after_gcv"]  = round(score_after, 3)
            scores_after.append(score_after)
        else:
            scores_after.append(score_before)

    # ---- Save JSON if improvements were made --------------------------------
    if not dry_run and chunks_improved > 0:
        try:
            with open(json_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=None)
        except Exception as exc:
            logger.error("  Failed to save %s: %s", json_path.name, exc)

    # ---- Compute averages ---------------------------------------------------
    avg_before = sum(scores_before) / len(scores_before) if scores_before else 0.0
    avg_after  = sum(scores_after)  / len(scores_after)  if scores_after  else 0.0

    return {
        "source":           source,
        "json_path":        str(json_path),
        "pdf_path":         str(pdf_path),
        "total_chunks":     len(chunks),
        "chunks_checked":   chunks_checked,
        "chunks_improved":  chunks_improved,
        "pages_sent_gcv":   pages_sent,
        "avg_score_before": round(avg_before, 3),
        "avg_score_after":  round(avg_after, 3),
        "improvement":      round(avg_after - avg_before, 3),
    }


# ---------------------------------------------------------------------------
# Cost estimation helper
# ---------------------------------------------------------------------------

def estimate_pages(book_entry: dict) -> int:
    """
    Estimate how many GCV page requests a book will generate.

    We use the number of GCV_NEEDED/BAD chunks as a proxy, but since multiple
    chunks can share a page, we try to count unique page_nums from gcv_chunks.
    """
    gcv_chunks = book_entry.get("gcv_chunks", [])
    if not gcv_chunks:
        # Fallback: rough estimate from gcv_needed count
        return book_entry.get("gcv_needed", 0)

    unique_pages = set()
    for c in gcv_chunks:
        pn = c.get("page_num", -1)
        if pn >= 0:
            unique_pages.add(pn)

    # If all page_nums are -1 (metadata missing), fall back to chunk count
    return len(unique_pages) if unique_pages else len(gcv_chunks)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Brahm AI - OCR Layer 3: Google Cloud Vision re-OCR",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Show what would be processed + cost estimate. No API calls.",
    )
    parser.add_argument(
        "--yes", "-y", action="store_true",
        help="Skip confirmation prompt and process immediately.",
    )
    parser.add_argument(
        "--limit", type=int, default=0,
        help="Process at most N books (0 = all).",
    )
    parser.add_argument(
        "--min-score", type=float, default=TARGET_MIN_SCORE,
        help=f"Skip books below this avg score (default: {TARGET_MIN_SCORE}).",
    )
    parser.add_argument(
        "--max-score", type=float, default=TARGET_MAX_SCORE,
        help=f"Skip books above this avg score (default: {TARGET_MAX_SCORE}).",
    )
    parser.add_argument(
        "--json-dir",  default=JSON_DIR,   help="Processed JSON directory."
    )
    parser.add_argument(
        "--raw-dir",   default=RAW_DIR,    help="Raw PDF directory."
    )
    parser.add_argument(
        "--dict-dir",  default=DICT_DIR,   help="Word dictionary directory."
    )
    parser.add_argument(
        "--report",    default=REPORT_PATH, help="Path to correction_report.json."
    )
    parser.add_argument(
        "--log-dir",   default=LOG_DIR,    help="Directory to write updated report."
    )
    args = parser.parse_args()

    raw_dir  = os.path.expanduser(args.raw_dir)
    json_dir = os.path.expanduser(args.json_dir)
    dict_dir = os.path.expanduser(args.dict_dir)
    log_dir  = os.path.expanduser(args.log_dir)
    report_path = os.path.expanduser(args.report)

    os.makedirs(log_dir, exist_ok=True)

    print("=" * 68)
    print("  BRAHM AI — OCR Layer 3: Google Cloud Vision")
    print(f"  Mode: {'DRY RUN (no API calls)' if args.dry_run else 'LIVE'}")
    print(f"  Score target: {args.min_score} – {args.max_score}")
    print("=" * 68)

    # ---- Load correction_report.json ----------------------------------------
    if not os.path.exists(report_path):
        logger.error(
            "correction_report.json not found at: %s\n"
            "Run 01b_correct_ocr.py first to generate it.", report_path
        )
        sys.exit(1)

    with open(report_path, "r", encoding="utf-8") as f:
        report = json.load(f)

    books = report.get("books", []) or report.get("book_scores", [])
    if not books:
        logger.error("No books found in correction_report.json.")
        sys.exit(1)

    logger.info("Loaded correction_report.json: %d book entries", len(books))

    # ---- Filter by score range ----------------------------------------------
    # Use avg_score_after (post Layer-2 correction) as the ranking signal.
    # Fall back to avg_score_before if _after is absent.
    def book_score(b):
        return b.get("avg_score_after", b.get("avg_score_before", b.get("avg_score", 0.0)))

    target_books = [
        b for b in books
        if args.min_score <= book_score(b) <= args.max_score
    ]

    # Sort worst-first so highest-impact books are processed first
    target_books.sort(key=book_score)

    if args.limit:
        target_books = target_books[: args.limit]

    if not target_books:
        print(
            f"\nNo books found in score range "
            f"[{args.min_score}, {args.max_score}]. Nothing to do."
        )
        sys.exit(0)

    # ---- Estimate cost ------------------------------------------------------
    total_est_pages = sum(estimate_pages(b) for b in target_books)
    est_cost = total_est_pages * GCV_COST_PER_PAGE

    print(f"\nTarget books: {len(target_books)}")
    print(f"Estimated GCV pages: {total_est_pages:,}")
    print(f"Estimated cost: ${est_cost:.2f}  (at ${GCV_COST_PER_PAGE}/page)")
    print()

    # Print the list
    print(f"{'#':<4}  {'Score':>6}  {'GCV chunks':>10}  {'Est pages':>9}  Source")
    print("-" * 68)
    for i, b in enumerate(target_books, 1):
        sc = book_score(b)
        gcv_n = b.get("gcv_needed", 0)
        est_p = estimate_pages(b)
        src = b.get("source", "unknown")[:45]
        print(f"{i:<4}  {sc:>6.3f}  {gcv_n:>10,}  {est_p:>9,}  {src}")
    print("-" * 68)

    if args.dry_run:
        print(
            "\nDry run complete — no API calls made.\n"
            "Remove --dry-run to process these books."
        )
        return

    # ---- Confirmation -------------------------------------------------------
    if not args.yes:
        print(
            f"\nThis will send up to {total_est_pages:,} pages to GCV "
            f"(~${est_cost:.2f}).\n"
            "Continue? [y/N] ",
            end="",
            flush=True,
        )
        answer = sys.stdin.readline().strip().lower()
        if answer not in ("y", "yes"):
            print("Aborted.")
            return

    # ---- Load dictionaries --------------------------------------------------
    logger.info("Loading word dictionaries...")
    word_dicts = {
        "hindi":   load_hindi_dict(dict_dir),
        "english": load_english_dict(dict_dir),
    }

    # ---- Initialise GCV client ----------------------------------------------
    logger.info("Initialising Google Cloud Vision client...")
    try:
        from google.cloud import vision
        gcv_client = vision.ImageAnnotatorClient()
        logger.info("GCV client ready.")
    except ImportError:
        logger.error(
            "google-cloud-vision not installed.\n"
            "Run: pip install google-cloud-vision"
        )
        sys.exit(1)
    except Exception as exc:
        logger.error(
            "Failed to create GCV client: %s\n"
            "Check that Vision API is enabled and credentials are configured.", exc
        )
        sys.exit(1)

    # ---- Process books ------------------------------------------------------
    print(f"\nProcessing {len(target_books)} books...\n")
    t_start = time.time()

    # Shared page cache — avoids duplicate GCV calls across the whole session.
    # Structure: { pdf_path_str -> { page_num -> gcv_text } }
    page_cache = {}

    results      = []
    total_improved_chunks = 0
    total_pages_sent      = 0

    for i, book_entry in enumerate(target_books, 1):
        source = book_entry.get("source", "unknown")
        sc     = book_score(book_entry)

        # Resolve JSON path from source name
        # source may end with .pdf — strip it for JSON lookup
        source_stem = Path(source).stem  # removes .pdf extension
        possible_json = Path(json_dir) / (source_stem + ".json")
        if not possible_json.exists():
            # Fuzzy match: spaces→underscores and vice versa
            alt_stem = source_stem.replace(" ", "_")
            alt_json = Path(json_dir) / (alt_stem + ".json")
            if alt_json.exists():
                possible_json = alt_json
            else:
                # Recursive glob on stem name
                candidates = list(Path(json_dir).rglob(source_stem + ".json"))
                if not candidates:
                    candidates = list(Path(json_dir).rglob(alt_stem + ".json"))
                if candidates:
                    possible_json = candidates[0]
                else:
                    logger.warning("[%d/%d] JSON not found for '%s' — skip",
                                   i, len(target_books), source)
                    continue

        elapsed = time.time() - t_start
        logger.info(
            "[%d/%d] %.1fs | %s (score=%.3f)",
            i, len(target_books), elapsed, source, sc,
        )

        result = process_book(
            json_path    = possible_json,
            raw_dir      = raw_dir,
            gcv_client   = gcv_client,
            word_dicts   = word_dicts,
            dry_run      = False,
            page_cache   = page_cache,
        )

        results.append(result)

        if "error" in result or result.get("skipped"):
            reason = result.get("reason") or result.get("error", "?")
            logger.warning("  Skipped: %s", reason)
            continue

        imp = result["chunks_improved"]
        pgs = result["pages_sent_gcv"]
        sc_after = result["avg_score_after"]
        total_improved_chunks += imp
        total_pages_sent      += pgs

        logger.info(
            "  chunks_improved=%d  pages_sent=%d  "
            "score %.3f → %.3f  (+%.3f)",
            imp, pgs,
            result["avg_score_before"], sc_after, result["improvement"],
        )

    total_elapsed = time.time() - t_start
    total_cost    = total_pages_sent * GCV_COST_PER_PAGE

    # ---- Summary ------------------------------------------------------------
    print("\n" + "=" * 68)
    print("  GCV LAYER 3 COMPLETE")
    print("=" * 68)
    print(f"  Books processed:       {len(results)}")
    print(f"  Pages sent to GCV:     {total_pages_sent:,}")
    print(f"  Chunks improved:       {total_improved_chunks:,}")
    print(f"  Actual cost:           ${total_cost:.2f}")
    print(f"  Elapsed time:          {total_elapsed:.1f}s "
          f"({total_elapsed/60:.1f} min)")

    # Top improvements
    improved = [r for r in results if not r.get("skipped") and "error" not in r
                and r.get("improvement", 0) > 0]
    improved.sort(key=lambda x: x["improvement"], reverse=True)
    if improved:
        print(f"\n  Top improved books:")
        for r in improved[:10]:
            print(
                f"    {r['source'][:45]:<45}  "
                f"{r['avg_score_before']:.3f} → {r['avg_score_after']:.3f}  "
                f"(+{r['improvement']:.3f})"
            )

    # ---- Save updated report ------------------------------------------------
    updated_report_path = os.path.join(log_dir, "gcv_layer3_report.json")
    updated_report = {
        "run_timestamp":        time.strftime("%Y-%m-%dT%H:%M:%S"),
        "books_processed":      len(results),
        "pages_sent_gcv":       total_pages_sent,
        "chunks_improved":      total_improved_chunks,
        "actual_cost_usd":      round(total_cost, 4),
        "elapsed_seconds":      round(total_elapsed, 1),
        "score_min_threshold":  args.min_score,
        "score_max_threshold":  args.max_score,
        "books": results,
    }
    with open(updated_report_path, "w", encoding="utf-8") as f:
        json.dump(updated_report, f, ensure_ascii=False, indent=2)
    logger.info("Report saved: %s", updated_report_path)

    # Merge updated scores back into correction_report so 02_build_index.py
    # (via OCR quality multiplier) sees the improved scores.
    _merge_scores_into_report(report, results, report_path)


# ---------------------------------------------------------------------------
# Score merge helper
# ---------------------------------------------------------------------------

def _merge_scores_into_report(original_report: dict, gcv_results: list, report_path: str):
    """
    Update avg_score_after in correction_report.json for all books that GCV
    improved, so subsequent pipeline stages (FAISS rebuild, quality multiplier)
    see the real current quality.
    """
    # Build lookup: source → gcv result
    gcv_map = {
        r["source"]: r
        for r in gcv_results
        if not r.get("skipped") and "error" not in r
    }

    if not gcv_map:
        return

    updated = 0
    for book_entry in original_report.get("books", []):
        src = book_entry.get("source", "")
        if src in gcv_map and gcv_map[src]["improvement"] > 0:
            book_entry["avg_score_after"] = gcv_map[src]["avg_score_after"]
            book_entry["gcv_layer3_applied"] = True
            updated += 1

    if updated:
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(original_report, f, ensure_ascii=False, indent=2)
        logger.info(
            "Merged %d updated scores back into correction_report.json", updated
        )


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    # Syntax-check guard — catches import-time errors before any real work
    import ast
    try:
        with open(__file__, "r", encoding="utf-8") as _fh:
            ast.parse(_fh.read())
    except SyntaxError as _e:
        print(f"SYNTAX ERROR in {__file__}: {_e}")
        sys.exit(1)

    main()
