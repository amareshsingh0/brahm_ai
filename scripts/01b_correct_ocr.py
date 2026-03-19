#!/usr/bin/env python3
"""
Brahm AI - OCR Correction Layer 2
Dictionary + SymSpell correction on all processed JSONs.

Step 2 of OCR Correction Pipeline.
Run AFTER build_word_dict.py.

Run on VM:
    cd ~/books
    source ~/ai-env/bin/activate

    # Dry run first (just score, no changes):
    python3 scripts/01b_correct_ocr.py --dry-run

    # Apply corrections:
    python3 scripts/01b_correct_ocr.py

    # Only show report:
    python3 scripts/01b_correct_ocr.py --report-only
"""

import os
import json
import re
import argparse
import time
from pathlib import Path
from collections import defaultdict

BASE_DIR = os.path.expanduser("~/books")
JSON_DIR = os.path.join(BASE_DIR, "data/processed/json")
DICT_DIR = os.path.join(BASE_DIR, "data/dictionaries")
LOG_DIR = os.path.join(BASE_DIR, "data/processed/logs")

# Chunk score thresholds
GRADE_GOOD = 0.80        # No correction needed
GRADE_FIXABLE = 0.50     # Layer 2 can fix
GRADE_GCV_NEEDED = 0.30  # Send to Google Cloud Vision (Layer 3)
# Below 0.30 = BAD (flag for review)

# Common Tesseract Devanagari confusion pairs
DEVANAGARI_OCR_ERRORS = {
    "ब्रद्म": "ब्रह्म",
    "ब्रम्ह": "ब्रह्म",
    "धर्म्म": "धर्म",
    "कर्म्म": "कर्म",
    "अाा": "आ",
    "र्म": "र्म",
    # Common matra splits (already handled by normalizer, but double-check)
    "ाे": "ो",
    "ाै": "ौ",
    # Numeric OCR confusion
    "०": "0",
    "।।": "॥",
}

# Garbage patterns to remove
GARBAGE_PATTERNS = [
    r'[A-Za-z]\s*=\s*[A-Za-z]',   # "x=y" style OCR garbage
    r'\w+!=-\w+',                   # "word!=-word" style garbage (seen in results)
    r'[^\u0000-\u007F\u0900-\u097F\s]{3,}',  # Long runs of non-Latin/non-Devanagari
]


def load_dictionaries(dict_dir: str) -> dict:
    """Load word frequency dictionaries."""
    dicts = {}
    for lang in ["hindi", "english"]:
        path = os.path.join(dict_dir, f"{lang}_freq.json")
        if os.path.exists(path):
            with open(path, 'r', encoding='utf-8') as f:
                dicts[lang] = json.load(f)
            print(f"  Loaded {lang} dict: {len(dicts[lang]):,} words")
        else:
            print(f"  WARNING: {path} not found — run build_word_dict.py first")
            dicts[lang] = {}
    return dicts


def load_symspell(dict_dir: str) -> dict:
    """Load SymSpell instances per language."""
    try:
        from symspellpy import SymSpell, Verbosity
        sym = {}
        for lang in ["hindi", "english"]:
            path = os.path.join(dict_dir, f"{lang}_symspell.txt")
            if os.path.exists(path):
                s = SymSpell(max_dictionary_edit_distance=2)
                s.load_dictionary(path, term_index=0, count_index=1, encoding="utf-8")
                sym[lang] = s
                print(f"  SymSpell {lang}: loaded")
            else:
                sym[lang] = None
        return sym, Verbosity
    except ImportError:
        print("  WARNING: symspellpy not installed. Run: pip install symspellpy")
        return {}, None


def score_chunk(text: str, word_dict: dict) -> dict:
    """Score a chunk's text quality against known dictionary."""
    if not text or len(text.strip()) < 10:
        return {"score": 0.0, "grade": "EMPTY", "total": 0, "known": 0}

    words = text.split()
    if not words:
        return {"score": 0.0, "grade": "EMPTY", "total": 0, "known": 0}

    # Count known words (in dict OR very short common words)
    known = sum(1 for w in words if
                w.lower() in word_dict or
                len(w) <= 2 or
                w.isdigit())
    score = known / len(words)

    if score >= GRADE_GOOD:
        grade = "GOOD"
    elif score >= GRADE_FIXABLE:
        grade = "FIXABLE"
    elif score >= GRADE_GCV_NEEDED:
        grade = "GCV_NEEDED"
    else:
        grade = "BAD"

    return {"score": round(score, 3), "grade": grade,
            "total": len(words), "known": known}


def detect_lang(text: str, chunk_lang: str = None) -> str:
    """Determine which dictionary to use for scoring."""
    if chunk_lang in ("hindi", "sanskrit"):
        return "hindi"
    if chunk_lang == "english":
        return "english"
    # Auto-detect
    devanagari = sum(1 for c in text if '\u0900' <= c <= '\u097F')
    if devanagari / max(len(text), 1) > 0.2:
        return "hindi"
    return "english"


def apply_ocr_error_map(text: str) -> str:
    """Fix known Tesseract confusion pairs."""
    for wrong, correct in DEVANAGARI_OCR_ERRORS.items():
        text = text.replace(wrong, correct)
    return text


def remove_garbage(text: str) -> str:
    """Remove obvious OCR garbage patterns."""
    for pattern in GARBAGE_PATTERNS:
        text = re.sub(pattern, ' ', text)
    # Clean multiple spaces
    text = re.sub(r' {2,}', ' ', text).strip()
    return text


def symspell_correct(text: str, sym, verbosity) -> str:
    """Apply SymSpell correction to text."""
    if sym is None:
        return text
    words = text.split()
    corrected = []
    for word in words:
        if len(word) < 3:
            corrected.append(word)
            continue
        suggestions = sym.lookup(word, verbosity.CLOSEST, max_edit_distance=2)
        if suggestions and suggestions[0].distance <= 1:
            corrected.append(suggestions[0].term)
        else:
            corrected.append(word)
    return ' '.join(corrected)


def correct_chunk(text: str, lang: str, sym_dict: dict, verbosity) -> str:
    """Apply all Layer 2 corrections to a chunk."""
    # Step 1: OCR error map (Devanagari confusions)
    if lang == "hindi":
        text = apply_ocr_error_map(text)

    # Step 2: Remove garbage patterns
    text = remove_garbage(text)

    # Step 3: SymSpell correction
    sym = sym_dict.get(lang)
    if sym and verbosity:
        text = symspell_correct(text, sym, verbosity)

    return text


def process_json_file(json_path: Path, word_dicts: dict, sym_dict: dict,
                      verbosity, dry_run: bool = False) -> dict:
    """Process a single JSON file — score all chunks, optionally correct."""
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        return {"error": str(e), "path": str(json_path)}

    chunks = data.get("chunks", [])
    if not chunks:
        return {"skipped": True, "reason": "no chunks"}

    source = data.get("source", str(json_path.name))
    pdf_type = data.get("pdf_type", "unknown")

    scores_before = []
    scores_after = []
    gcv_needed_chunks = []
    corrections_made = 0

    for i, chunk in enumerate(chunks):
        text = chunk.get("text", "")
        if not text:
            continue

        chunk_lang = chunk.get("language", "unknown")
        lang = detect_lang(text, chunk_lang)
        word_dict = word_dicts.get(lang, {})

        # Score before
        score_before = score_chunk(text, word_dict)
        scores_before.append(score_before["score"])

        if score_before["grade"] == "GOOD":
            scores_after.append(score_before["score"])
            continue

        # Apply correction
        if not dry_run:
            corrected_text = correct_chunk(text, lang, sym_dict, verbosity)
        else:
            corrected_text = text  # dry run: no changes

        # Score after
        score_after = score_chunk(corrected_text, word_dict)
        scores_after.append(score_after["score"])

        if corrected_text != text:
            corrections_made += 1
            if not dry_run:
                chunks[i]["text"] = corrected_text
                chunks[i]["ocr_corrected"] = True
                chunks[i]["score_before"] = score_before["score"]
                chunks[i]["score_after"] = score_after["score"]

        # Flag for GCV if still bad
        if score_after["grade"] in ("GCV_NEEDED", "BAD"):
            gcv_needed_chunks.append({
                "chunk_idx": i,
                "page_num": chunk.get("page_num", -1),
                "score_before": score_before["score"],
                "score_after": score_after["score"],
                "grade": score_after["grade"],
            })

    # Save corrected file
    if not dry_run and corrections_made > 0:
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=None)

    avg_before = sum(scores_before) / len(scores_before) if scores_before else 0
    avg_after = sum(scores_after) / len(scores_after) if scores_after else 0

    return {
        "source": source,
        "pdf_type": pdf_type,
        "chunks": len(chunks),
        "corrections_made": corrections_made,
        "avg_score_before": round(avg_before, 3),
        "avg_score_after": round(avg_after, 3),
        "gcv_needed": len(gcv_needed_chunks),
        "gcv_chunks": gcv_needed_chunks,
    }


def main():
    parser = argparse.ArgumentParser(description="OCR Correction Layer 2 — SymSpell + Dictionary")
    parser.add_argument("--json-dir", default=JSON_DIR)
    parser.add_argument("--dict-dir", default=DICT_DIR)
    parser.add_argument("--dry-run", action="store_true", help="Score only, no changes")
    parser.add_argument("--report-only", action="store_true", help="Score report only")
    parser.add_argument("--limit", type=int, default=0, help="Process only N files (for testing)")
    args = parser.parse_args()

    if args.report_only:
        args.dry_run = True

    print("=" * 60)
    print(f"  BRAHM AI - OCR Correction Layer 2")
    print(f"  Mode: {'DRY RUN' if args.dry_run else 'APPLY CORRECTIONS'}")
    print("=" * 60)

    os.makedirs(LOG_DIR, exist_ok=True)

    # Load dictionaries
    print("\n[1/3] Loading dictionaries...")
    word_dicts = load_dictionaries(args.dict_dir)

    print("\n[2/3] Loading SymSpell...")
    sym_dict, verbosity = load_symspell(args.dict_dir)

    # Process all JSONs
    print("\n[3/3] Processing JSON files...")
    json_files = sorted(Path(args.json_dir).glob("**/*.json"))
    if args.limit:
        json_files = json_files[:args.limit]

    total = len(json_files)
    print(f"  Found {total} JSON files")

    results = []
    t0 = time.time()

    grade_counts = defaultdict(int)
    total_corrections = 0
    total_gcv_needed = 0

    for i, jf in enumerate(json_files):
        if i % 50 == 0:
            elapsed = time.time() - t0
            rate = i / elapsed if elapsed > 0 else 0
            eta = (total - i) / rate if rate > 0 else 0
            print(f"  [{i}/{total}] corrections={total_corrections} gcv_needed={total_gcv_needed} ETA={eta:.0f}s")

        result = process_json_file(jf, word_dicts, sym_dict, verbosity, args.dry_run)
        if "error" not in result and not result.get("skipped"):
            results.append(result)
            total_corrections += result["corrections_made"]
            total_gcv_needed += result["gcv_needed"]

    elapsed = time.time() - t0

    # Summary
    print(f"\n{'='*60}")
    print(f"  CORRECTION SUMMARY")
    print(f"{'='*60}")
    print(f"  Files processed: {len(results)}")
    print(f"  Chunks corrected: {total_corrections}")
    print(f"  Chunks needing GCV (Layer 3): {total_gcv_needed}")
    print(f"  Time: {elapsed:.1f}s")

    if results:
        avg_before = sum(r["avg_score_before"] for r in results) / len(results)
        avg_after = sum(r["avg_score_after"] for r in results) / len(results)
        print(f"\n  Average score before: {avg_before:.3f}")
        print(f"  Average score after:  {avg_after:.3f} (+{avg_after-avg_before:.3f})")

        # Show worst books
        worst = sorted(results, key=lambda x: x["avg_score_after"])[:10]
        print(f"\n  Top 10 worst quality books (after correction):")
        for r in worst:
            print(f"    {r['source'][:50]} → score={r['avg_score_after']:.3f}, gcv_needed={r['gcv_needed']}")

    # Save report
    report_path = os.path.join(LOG_DIR, "correction_report.json")
    report = {
        "mode": "dry_run" if args.dry_run else "applied",
        "total_files": len(results),
        "total_corrections": total_corrections,
        "total_gcv_needed": total_gcv_needed,
        "avg_score_before": avg_before if results else 0,
        "avg_score_after": avg_after if results else 0,
        "books": results,
    }
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"\n  Report saved: {report_path}")

    if args.dry_run:
        print(f"\n  Dry run complete. Run without --dry-run to apply corrections.")
        print(f"  Estimated GCV pages needed: {total_gcv_needed} → ~${total_gcv_needed * 0.0015:.2f} cost")


if __name__ == "__main__":
    main()
