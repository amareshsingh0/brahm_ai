#!/usr/bin/env python3
"""
Brahm AI - Build Word Frequency Dictionaries
Step 1 of OCR Correction Pipeline (Layer 2)

Uses high-quality text-PDF JSONs as source of truth.
Builds per-language frequency dicts for SymSpell correction.

Run on VM:
    cd ~/books
    source ~/ai-env/bin/activate
    python3 scripts/build_word_dict.py
    python3 scripts/build_word_dict.py --min-freq 3 --verbose
"""

import os
import json
import re
import argparse
from collections import defaultdict
from pathlib import Path

BASE_DIR = os.path.expanduser("~/books")
JSON_DIR = os.path.join(BASE_DIR, "data/processed/json")
DICT_DIR = os.path.join(BASE_DIR, "data/dictionaries")

# Sources known to be TEXT PDFs (99%+ quality) — use as dictionary source
# These are digital text, not scanned — high accuracy
GOOD_SOURCES_KEYWORDS = [
    "wikipedia", "openphilology", "hf/", "suttacentral",
    "aws_datasets", "gretil", "wikisource"
]

# Minimum word length to include in dictionary
MIN_WORD_LEN = 2
MAX_WORD_LEN = 50


def is_valid_word(word: str) -> bool:
    """Filter out garbage tokens."""
    if len(word) < MIN_WORD_LEN or len(word) > MAX_WORD_LEN:
        return False
    # Skip pure numbers
    if word.isdigit():
        return False
    # Skip if more than 30% special chars (OCR garbage)
    special = sum(1 for c in word if not c.isalpha() and c not in "्ा-")
    if special / len(word) > 0.3:
        return False
    # Skip common OCR garbage patterns
    if re.search(r'[!@#$%^&*()+=\[\]{};\'\"\\|<>?/~`]', word):
        return False
    return True


def detect_language(text: str) -> str:
    """Simple language detection based on Unicode ranges."""
    devanagari = sum(1 for c in text if '\u0900' <= c <= '\u097F')
    latin = sum(1 for c in text if 'a' <= c.lower() <= 'z')
    total = len(text.replace(' ', '')) or 1

    if devanagari / total > 0.4:
        return "hindi"  # Hindi + Sanskrit both Devanagari
    elif latin / total > 0.4:
        return "english"
    return "unknown"


def tokenize(text: str) -> list:
    """Split text into words, handling Devanagari properly."""
    # Remove punctuation but keep Devanagari chars, Latin, numbers
    cleaned = re.sub(r'[^\w\s\u0900-\u097F\u0A00-\u0A7F]', ' ', text)
    words = cleaned.split()
    return [w.strip() for w in words if w.strip()]


def build_dictionaries(json_dir: str, dict_dir: str, min_freq: int = 2, verbose: bool = False):
    """
    Scan all JSONs, extract words from high-quality sources,
    build per-language frequency dictionaries.
    """
    os.makedirs(dict_dir, exist_ok=True)

    freq = {
        "hindi": defaultdict(int),
        "english": defaultdict(int),
        "sanskrit": defaultdict(int),
    }

    json_files = list(Path(json_dir).glob("**/*.json"))
    total = len(json_files)
    print(f"Scanning {total} JSON files...")

    processed = 0
    skipped = 0
    total_words = 0

    for i, jf in enumerate(json_files):
        if i % 500 == 0:
            print(f"  [{i}/{total}] processed={processed}, words={total_words:,}")

        try:
            with open(jf, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except Exception:
            skipped += 1
            continue

        chunks = data.get("chunks", [])
        source = data.get("source", str(jf))
        pdf_type = data.get("pdf_type", "unknown")

        # Only use high-quality sources for dictionary building
        # Text PDFs are 99%+ accurate — use them as ground truth
        is_good_source = (
            pdf_type == "text" or
            any(kw in source.lower() for kw in GOOD_SOURCES_KEYWORDS)
        )

        if not is_good_source:
            skipped += 1
            continue

        processed += 1

        for chunk in chunks:
            text = chunk.get("text", "")
            if not text or len(text) < 20:
                continue

            # Detect language
            lang = chunk.get("language", detect_language(text))
            if lang not in freq:
                lang = detect_language(text)
            if lang == "unknown":
                continue

            # For Devanagari, treat hindi/sanskrit as same dict
            if lang == "sanskrit":
                lang = "hindi"

            words = tokenize(text)
            for word in words:
                if is_valid_word(word):
                    freq[lang][word.lower()] += 1
                    total_words += 1

    print(f"\nDictionary build complete:")
    print(f"  Processed: {processed} files (good sources)")
    print(f"  Skipped:   {skipped} files (scanned/unknown)")
    print(f"  Total words extracted: {total_words:,}")

    # Save dictionaries
    for lang, word_freq in freq.items():
        # Filter by minimum frequency
        filtered = {w: c for w, c in word_freq.items() if c >= min_freq}
        filtered_sorted = dict(sorted(filtered.items(), key=lambda x: x[1], reverse=True))

        out_path = os.path.join(dict_dir, f"{lang}_freq.json")
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(filtered_sorted, f, ensure_ascii=False, indent=None)

        # Also save SymSpell format (word frequency.txt: "word count\n")
        symspell_path = os.path.join(dict_dir, f"{lang}_symspell.txt")
        with open(symspell_path, 'w', encoding='utf-8') as f:
            for word, count in filtered_sorted.items():
                f.write(f"{word} {count}\n")

        print(f"  {lang}: {len(filtered_sorted):,} words (min_freq={min_freq}) → {out_path}")

        if verbose and filtered_sorted:
            top10 = list(filtered_sorted.items())[:10]
            print(f"    Top 10: {top10}")

    print(f"\nDone! Dictionaries saved to: {dict_dir}")
    return freq


def main():
    parser = argparse.ArgumentParser(description="Build word frequency dictionaries for OCR correction")
    parser.add_argument("--json-dir", default=JSON_DIR, help="Directory with processed JSON files")
    parser.add_argument("--output-dir", default=DICT_DIR, help="Output directory for dictionaries")
    parser.add_argument("--min-freq", type=int, default=2, help="Minimum word frequency to include (default: 2)")
    parser.add_argument("--verbose", action="store_true", help="Show top words per language")
    args = parser.parse_args()

    print("=" * 60)
    print("  BRAHM AI - Build Word Frequency Dictionaries")
    print(f"  Source: {args.json_dir}")
    print(f"  Output: {args.output_dir}")
    print(f"  Min frequency: {args.min_freq}")
    print("=" * 60)

    build_dictionaries(args.json_dir, args.output_dir, args.min_freq, args.verbose)


if __name__ == "__main__":
    main()
