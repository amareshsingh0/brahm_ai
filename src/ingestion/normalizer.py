"""
Brahm AI - Text Normalizer
Handles Sanskrit/Devanagari/IAST normalization, Unicode cleanup,
and transliteration between scripts.

Usage:
    from ingestion.normalizer import normalize_text, transliterate
    clean = normalize_text("  ॐ   नमः   शिवाय  ")
    roman = transliterate("ॐ नमः शिवाय", "devanagari", "iast")
"""

import re
import unicodedata
import logging

logger = logging.getLogger(__name__)


def normalize_unicode(text: str) -> str:
    """Normalize Unicode to NFC form (composed characters)."""
    return unicodedata.normalize("NFC", text)


def clean_whitespace(text: str) -> str:
    """Clean excessive whitespace while preserving paragraph breaks."""
    # Replace multiple spaces with single space
    text = re.sub(r'[^\S\n]+', ' ', text)
    # Replace 3+ newlines with double newline (paragraph break)
    text = re.sub(r'\n{3,}', '\n\n', text)
    # Strip leading/trailing whitespace from each line
    lines = [line.strip() for line in text.split('\n')]
    return '\n'.join(lines).strip()


def remove_page_artifacts(text: str) -> str:
    """Remove common PDF extraction artifacts."""
    # Remove page numbers (standalone numbers on a line)
    text = re.sub(r'^\s*\d{1,4}\s*$', '', text, flags=re.MULTILINE)
    # Remove common headers/footers
    text = re.sub(r'^\s*(page|पृष्ठ|पेज)\s*\d+\s*$', '', text, flags=re.MULTILINE | re.IGNORECASE)
    # Remove excessive dots (table of contents artifacts)
    text = re.sub(r'\.{5,}', '', text)
    # Remove form feed characters
    text = text.replace('\f', '\n')
    return text


def normalize_devanagari(text: str) -> str:
    """Normalize Devanagari-specific issues.

    - Fix common OCR errors in Devanagari
    - Normalize visarga, anusvara, chandrabindu
    - Fix broken conjuncts
    """
    # Normalize various forms of OM
    text = text.replace('ॐ', 'ॐ')  # Ensure consistent OM

    # Fix common OCR misreads
    replacements = {
        'ाे': 'ो',    # aa + e matra -> o matra
        'ाॆ': 'ो',
        'ेा': 'ो',
        'ाै': 'ौ',    # aa + ai matra -> au matra
    }
    for wrong, right in replacements.items():
        text = text.replace(wrong, right)

    return text


def normalize_sanskrit_punctuation(text: str) -> str:
    """Normalize Sanskrit punctuation marks."""
    # Ensure proper danda usage
    text = re.sub(r'\s*।\s*', ' । ', text)
    text = re.sub(r'\s*॥\s*', ' ॥ ', text)
    # Clean up double spaces created
    text = re.sub(r'  +', ' ', text)
    return text


def fix_ocr_errors_devanagari(text: str) -> str:
    """Fix common PaddleOCR/Tesseract errors for Devanagari."""
    fixes = {
        # Common character confusions
        'प़': 'प',
        'क़': 'क',
        'ख़': 'ख',
        'ग़': 'ग',
        # Halant issues
        '्‍': '्',  # Remove ZWJ after halant if not needed
    }
    for wrong, right in fixes.items():
        text = text.replace(wrong, right)
    return text


def normalize_text(text: str, script: str = "auto") -> str:
    """Main normalization pipeline.

    Args:
        text: Raw extracted text
        script: Script type ('devanagari', 'tamil', 'latin', 'auto')

    Returns:
        Cleaned, normalized text
    """
    if not text:
        return ""

    # Step 1: Unicode normalization
    text = normalize_unicode(text)

    # Step 2: Remove PDF artifacts
    text = remove_page_artifacts(text)

    # Step 3: Script-specific normalization
    if script == "auto":
        # Quick check for Devanagari
        devanagari_chars = len(re.findall(r'[\u0900-\u097F]', text))
        total_alpha = len(re.findall(r'\w', text)) or 1
        if devanagari_chars / total_alpha > 0.3:
            script = "devanagari"

    if script == "devanagari":
        text = normalize_devanagari(text)
        text = normalize_sanskrit_punctuation(text)
        text = fix_ocr_errors_devanagari(text)

    # Step 4: Clean whitespace (always last)
    text = clean_whitespace(text)

    return text


def transliterate(text: str, source_script: str, target_script: str) -> str:
    """Transliterate between scripts using aksharamukha.

    Supported scripts: devanagari, iast, slp1, hk, tamil, telugu, kannada, malayalam, bengali

    Args:
        text: Input text
        source_script: Source script name
        target_script: Target script name

    Returns:
        Transliterated text
    """
    try:
        from aksharamukha import transliterate as aksha

        # Map our names to aksharamukha names
        script_map = {
            "devanagari": "Devanagari",
            "iast": "IAST",
            "slp1": "SLP1",
            "hk": "HK",  # Harvard-Kyoto
            "tamil": "Tamil",
            "telugu": "Telugu",
            "kannada": "Kannada",
            "malayalam": "Malayalam",
            "bengali": "Bengali",
            "gurmukhi": "Gurmukhi",
            "gujarati": "Gujarati",
            "oriya": "Oriya",
        }

        src = script_map.get(source_script, source_script)
        tgt = script_map.get(target_script, target_script)

        return aksha.process(src, tgt, text)
    except ImportError:
        logger.warning("aksharamukha not installed. Returning original text.")
        return text
    except Exception as e:
        logger.error(f"Transliteration error: {e}")
        return text
