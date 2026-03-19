"""
Brahm AI - Script & Language Detector
Detects the script (Devanagari, Tamil, Telugu, etc.) and language of text.
Uses Unicode ranges for script detection, langdetect for language.

Usage:
    from ingestion.script_detector import detect_script, detect_language
    script = detect_script("ॐ नमः शिवाय")  # "devanagari"
    lang = detect_language("This is English text")  # "en"
"""

import re
import logging
from collections import Counter
from typing import Optional

logger = logging.getLogger(__name__)

# Unicode ranges for Indian scripts
SCRIPT_RANGES = {
    "devanagari": (0x0900, 0x097F),      # Hindi, Sanskrit, Marathi
    "bengali": (0x0980, 0x09FF),
    "gurmukhi": (0x0A00, 0x0A7F),        # Punjabi
    "gujarati": (0x0A80, 0x0AFF),
    "oriya": (0x0B00, 0x0B7F),
    "tamil": (0x0B80, 0x0BFF),
    "telugu": (0x0C00, 0x0C7F),
    "kannada": (0x0C80, 0x0CFF),
    "malayalam": (0x0D00, 0x0D7F),
    "sinhala": (0x0D80, 0x0DFF),
    "tibetan": (0x0F00, 0x0FFF),
    "latin": (0x0041, 0x024F),            # English, IAST transliteration
    "arabic": (0x0600, 0x06FF),           # Urdu, Arabic
}

# Script to likely language mapping
SCRIPT_LANGUAGE_MAP = {
    "devanagari": ["sa", "hi", "mr"],     # Sanskrit, Hindi, Marathi
    "tamil": ["ta"],
    "telugu": ["te"],
    "kannada": ["kn"],
    "malayalam": ["ml"],
    "bengali": ["bn"],
    "gujarati": ["gu"],
    "gurmukhi": ["pa"],
    "oriya": ["or"],
    "latin": ["en"],
    "arabic": ["ur"],
}

# PaddleOCR language code mapping
SCRIPT_TO_PADDLE_LANG = {
    "devanagari": "hi",
    "tamil": "ta",
    "telugu": "te",
    "kannada": "kn",
    "malayalam": "ml",
    "bengali": "bn",
    "latin": "en",
    "arabic": "ar",
}


def detect_script(text: str) -> dict:
    """Detect the primary script(s) in the given text.

    Returns:
        dict with keys:
            primary: str - dominant script name
            scripts: dict - {script_name: percentage}
            is_multilingual: bool
            ocr_lang: str - recommended PaddleOCR language code
    """
    if not text or not text.strip():
        return {"primary": "unknown", "scripts": {}, "is_multilingual": False, "ocr_lang": "en"}

    script_counts = Counter()
    total_chars = 0

    for char in text:
        cp = ord(char)
        if char.isspace() or char in "।॥,.;:!?'-()[]{}\"0123456789०१२३४५६७८९":
            continue

        total_chars += 1
        matched = False
        for script_name, (start, end) in SCRIPT_RANGES.items():
            if start <= cp <= end:
                script_counts[script_name] += 1
                matched = True
                break

        if not matched:
            script_counts["other"] += 1

    if total_chars == 0:
        return {"primary": "unknown", "scripts": {}, "is_multilingual": False, "ocr_lang": "en"}

    # Calculate percentages
    scripts = {}
    for script, count in script_counts.most_common():
        pct = round(count / total_chars * 100, 1)
        if pct >= 1.0:  # Only include scripts with >1%
            scripts[script] = pct

    primary = script_counts.most_common(1)[0][0] if script_counts else "unknown"

    # Check if multilingual (second script has >15%)
    is_multilingual = False
    if len(script_counts) > 1:
        sorted_counts = script_counts.most_common()
        if len(sorted_counts) > 1:
            second_pct = sorted_counts[1][1] / total_chars * 100
            is_multilingual = second_pct > 15

    ocr_lang = SCRIPT_TO_PADDLE_LANG.get(primary, "en")

    return {
        "primary": primary,
        "scripts": scripts,
        "is_multilingual": is_multilingual,
        "ocr_lang": ocr_lang,
    }


def detect_language(text: str) -> Optional[str]:
    """Detect language using langdetect library.

    Returns ISO 639-1 language code or None.
    """
    try:
        from langdetect import detect
        if len(text.strip()) < 20:
            return None
        return detect(text)
    except Exception:
        return None


def classify_difficulty(text: str) -> str:
    """Classify OCR difficulty of text.

    Returns: 'easy', 'medium', 'hard'
    - easy: Clean printed text, single script
    - medium: Multiple scripts, some special characters
    - hard: Handwritten-like, dense diacritics, complex layouts
    """
    script_info = detect_script(text)

    # Count diacritical marks and special chars
    diacritics = len(re.findall(r'[\u0300-\u036F\u0900-\u0903\u093A-\u094F\u0951-\u0957]', text))
    total = max(len(text), 1)
    diacritic_ratio = diacritics / total

    if script_info["is_multilingual"]:
        return "hard"
    elif diacritic_ratio > 0.15:
        return "hard"
    elif script_info["primary"] in ("tamil", "malayalam", "telugu", "kannada"):
        return "medium"  # South Indian scripts are inherently more complex for OCR
    elif script_info["primary"] == "latin":
        return "easy"
    else:
        return "medium"


def get_ocr_languages(text_sample: str) -> list[str]:
    """Get recommended OCR language list based on text sample.

    Returns list of PaddleOCR language codes.
    """
    script_info = detect_script(text_sample)
    langs = set()

    for script_name in script_info["scripts"]:
        paddle_lang = SCRIPT_TO_PADDLE_LANG.get(script_name)
        if paddle_lang:
            langs.add(paddle_lang)

    if not langs:
        langs.add("en")

    return list(langs)
