"""
Brahm AI - PDF Text Extractor (Multi-OCR Router)
Handles text-based and scanned PDFs with smart OCR routing.
Supports: Sanskrit, Hindi, Tamil, Telugu, Kannada, Malayalam, English

OCR Engines:
  - tesseract: Best for Devanagari/Sanskrit/Hindi/Tamil/Telugu (default for Indic)
  - paddle: PaddleOCR v2.9.1, good for English
  - auto: Detect script first, route to best engine

Requires: pypdf, pdf2image, pytesseract, tesseract-ocr, poppler-utils (apt)
Optional: paddleocr==2.9.1 (English fallback)
Tesseract langs: apt install tesseract-ocr-hin tesseract-ocr-san tesseract-ocr-tam tesseract-ocr-tel

Usage:
    from ingestion.pdf_extractor import extract_pdf
    pages = extract_pdf("/path/to/book.pdf", ocr_engine="tesseract")
"""

import os
import time
import logging
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)

_paddle_instance = None


def _get_paddle(lang: str = "en"):
    """Get or create PaddleOCR instance (singleton, English fallback)."""
    global _paddle_instance
    if _paddle_instance is None:
        from paddleocr import PaddleOCR
        _paddle_instance = PaddleOCR(use_angle_cls=True, lang=lang, show_log=False)
    return _paddle_instance


def _classify_script(text: str) -> str:
    """Classify dominant script from text sample."""
    if not text:
        return "unknown"
    dev = sum(1 for c in text if 0x0900 <= ord(c) <= 0x097F)
    tam = sum(1 for c in text if 0x0B80 <= ord(c) <= 0x0BFF)
    tel = sum(1 for c in text if 0x0C00 <= ord(c) <= 0x0C7F)
    lat = sum(1 for c in text if 0x0041 <= ord(c) <= 0x024F)
    total = dev + tam + tel + lat
    if total == 0:
        return "unknown"
    if dev / total > 0.3:
        return "devanagari"
    if tam / total > 0.3:
        return "tamil"
    if tel / total > 0.3:
        return "telugu"
    if lat / total > 0.3:
        return "latin"
    return "unknown"


def _detect_script_from_image(img) -> str:
    """Quick script detection using Tesseract on first page."""
    try:
        import pytesseract
        from PIL import Image
        if not isinstance(img, Image.Image):
            img = Image.fromarray(img)
        text = pytesseract.image_to_string(img, lang="hin+san+eng", config="--psm 6")
        script = _classify_script(text)
        return script if script != "unknown" else "devanagari"
    except Exception as e:
        logger.warning(f"  [Script] Detection failed: {e}")
        return "unknown"


def _detect_script_from_text(text: str) -> str:
    """Detect script from already-extracted text (for pre-routing)."""
    return _classify_script(text)


# Script -> OCR engine mapping
SCRIPT_ENGINE_MAP = {
    "devanagari": "tesseract",
    "tamil": "tesseract",
    "telugu": "tesseract",
    "latin": "paddle",
    "unknown": "tesseract",
}

# Script -> Tesseract language codes
SCRIPT_TESS_LANG = {
    "devanagari": "hin+san+eng",
    "tamil": "tam+eng",
    "telugu": "tel+eng",
    "latin": "eng",
    "unknown": "hin+san+eng",
}


def _get_best_engine(script: str, preferred: str = "auto") -> str:
    """Choose best OCR engine for given script."""
    if preferred in ("tesseract", "paddle"):
        return preferred
    return SCRIPT_ENGINE_MAP.get(script, "tesseract")


def _quality_score(text: str) -> float:
    """Score extracted text quality (0.0 to 1.0)."""
    words = text.split()
    if not words:
        return 0.0
    avg_len = sum(len(w) for w in words) / len(words)
    long_words = sum(1 for w in words if len(w) >= 3) / len(words)
    return min(1.0, (avg_len / 5) * 0.4 + long_words * 0.6)


def _ocr_page_tesseract(img, tess_lang: str = "hin+san+eng") -> str:
    """Extract text from single page image using Tesseract."""
    import pytesseract
    from PIL import Image
    if not isinstance(img, Image.Image):
        img = Image.fromarray(img)
    try:
        text = pytesseract.image_to_string(img, lang=tess_lang, config="--psm 6")
        return text.strip()
    except Exception as e:
        logger.warning(f"  [Tesseract] Page OCR failed: {e}")
        return ""


def _ocr_page_paddle(img_array) -> str:
    """Extract text from single page image using PaddleOCR."""
    import numpy as np
    ocr = _get_paddle("en")
    if not isinstance(img_array, np.ndarray):
        img_array = np.array(img_array)
    try:
        result = ocr.ocr(img_array, cls=True)
        if result and result[0]:
            lines = [line[1][0] for line in result[0] if line[1][1] > 0.5]
            return "\n".join(lines)
    except Exception as e:
        logger.warning(f"  [Paddle] Page OCR failed: {e}")
    return ""


def _ocr_page(img, script: str, engine: str, tess_lang: str = "hin+san+eng") -> tuple[str, str]:
    """Route OCR to best engine with quality fallback.

    Returns: (text, engine_used)
    """
    if engine == "tesseract":
        text = _ocr_page_tesseract(img, tess_lang)
    elif engine == "paddle":
        import numpy as np
        img_arr = np.array(img) if not isinstance(img, np.ndarray) else img
        text = _ocr_page_paddle(img_arr)
    else:
        text = _ocr_page_tesseract(img, tess_lang)

    # Quality fallback
    score = _quality_score(text)
    if score < 0.3 and len(text.strip()) > 0:
        fallback = "paddle" if engine == "tesseract" else "tesseract"
        try:
            if fallback == "tesseract":
                fb_text = _ocr_page_tesseract(img, tess_lang)
            else:
                import numpy as np
                img_arr = np.array(img) if not isinstance(img, np.ndarray) else img
                fb_text = _ocr_page_paddle(img_arr)
            if _quality_score(fb_text) > score:
                return fb_text, fallback
        except Exception:
            pass
    return text, engine


def detect_pdf_type(pdf_path: str) -> str:
    """Detect if PDF is text-based, scanned (image-based), or hybrid."""
    from pypdf import PdfReader
    reader = PdfReader(pdf_path)
    if len(reader.pages) == 0:
        return "empty"
    sample_pages = min(5, len(reader.pages))
    total_chars = 0
    for i in range(sample_pages):
        text = reader.pages[i].extract_text() or ""
        total_chars += len(text.strip())
    avg_chars = total_chars / sample_pages
    if avg_chars > 100:
        return "text"
    elif avg_chars > 20:
        return "hybrid"
    else:
        return "scanned"


def extract_text_pdf(pdf_path: str) -> list[dict]:
    """Extract text from text-based PDF using pypdf."""
    from pypdf import PdfReader
    reader = PdfReader(pdf_path)
    pages = []
    total = len(reader.pages)
    for i, page in enumerate(reader.pages):
        text = (page.extract_text() or "").strip()
        if text:
            pages.append({"page_num": i + 1, "text": text, "method": "pypdf", "char_count": len(text)})
        if (i + 1) % 100 == 0:
            logger.info(f"  [pypdf] {i+1}/{total} pages extracted")
    logger.info(f"  [pypdf] Done: {len(pages)}/{total} pages had text")
    return pages


def extract_scanned_pdf(
    pdf_path: str,
    languages: Optional[list[str]] = None,
    max_pages: Optional[int] = None,
    ocr_engine: str = "auto",
) -> list[dict]:
    """Extract text from scanned PDF using smart OCR routing.
    Converts pages one-at-a-time to avoid RAM overload on large PDFs."""
    from pdf2image import convert_from_path
    from pypdf import PdfReader

    # Get total page count without loading images
    try:
        reader = PdfReader(pdf_path)
        total = len(reader.pages)
    except Exception:
        total = 0

    if max_pages and total > 0:
        total = min(total, max_pages)

    if total == 0:
        logger.error(f"  [OCR] Could not read PDF or 0 pages")
        return []

    logger.info(f"  [OCR] {total} pages to process (page-by-page mode)")

    # Detect script from first page only
    engine = "tesseract"
    script = "unknown"
    tess_lang = "hin+san+eng"

    if ocr_engine == "auto":
        try:
            first_imgs = convert_from_path(pdf_path, dpi=300, first_page=1, last_page=1)
            if first_imgs:
                script = _detect_script_from_image(first_imgs[0])
                engine = SCRIPT_ENGINE_MAP.get(script, "tesseract")
                tess_lang = SCRIPT_TESS_LANG.get(script, "hin+san+eng")
                logger.info(f"  [OCR] Detected script: {script} -> engine: {engine}, lang: {tess_lang}")
                del first_imgs
        except Exception as e:
            logger.warning(f"  [OCR] Script detection failed: {e}, using tesseract default")
    elif ocr_engine == "tesseract":
        engine = "tesseract"
        tess_lang = "hin+san+eng"
    elif ocr_engine == "paddle":
        engine = "paddle"
        script = "latin"
        tess_lang = "eng"

    pages = []
    failed = 0
    start_time = time.time()
    engines_used = {}

    # Process pages one-by-one (low RAM usage)
    for i in range(total):
        page_num = i + 1
        try:
            imgs = convert_from_path(pdf_path, dpi=300, first_page=page_num, last_page=page_num)
            if not imgs:
                failed += 1
                continue
            img = imgs[0]
        except Exception as e:
            failed += 1
            if failed <= 3:
                logger.warning(f"  [OCR] Page {page_num} convert failed: {e}")
            continue

        try:
            page_text, used = _ocr_page(img, script, engine, tess_lang)
            engines_used[used] = engines_used.get(used, 0) + 1
        except Exception as e:
            failed += 1
            if failed <= 3:
                logger.warning(f"  [OCR] Page {page_num} OCR failed: {e}")
            del img, imgs
            continue

        if page_text.strip():
            pages.append({"page_num": page_num, "text": page_text.strip(), "method": used, "char_count": len(page_text.strip())})

        # Free memory immediately
        del img, imgs

        if page_num % 10 == 0 or page_num == total:
            elapsed = time.time() - start_time
            speed = page_num / elapsed if elapsed > 0 else 0
            eta = (total - page_num) / speed if speed > 0 else 0
            logger.info(f"  [OCR] {page_num}/{total} pages | {len(pages)} extracted | {failed} failed | {elapsed:.0f}s elapsed | ETA {eta:.0f}s")

    elapsed = time.time() - start_time
    logger.info(f"  [OCR] COMPLETE: {len(pages)}/{total} pages, {failed} failed, {elapsed:.0f}s | Engines: {engines_used}")
    return pages


def extract_hybrid_pdf(
    pdf_path: str,
    languages: Optional[list[str]] = None,
    max_pages: Optional[int] = None,
    ocr_engine: str = "auto",
) -> list[dict]:
    """Extract from hybrid PDF - try text first, OCR for pages with little text."""
    from pypdf import PdfReader
    reader = PdfReader(pdf_path)
    pages = []
    ocr_needed = []
    page_limit = max_pages or len(reader.pages)
    for i in range(min(page_limit, len(reader.pages))):
        text = (reader.pages[i].extract_text() or "").strip()
        if len(text) > 50:
            pages.append({"page_num": i + 1, "text": text, "method": "pypdf", "char_count": len(text)})
        else:
            ocr_needed.append(i)
    logger.info(f"  [Hybrid] {len(pages)} text pages, {len(ocr_needed)} need OCR")

    if ocr_needed:
        from pdf2image import convert_from_path
        if pages:
            sample = " ".join(p["text"][:200] for p in pages[:3])
            script = _classify_script(sample)
        else:
            script = "unknown"
        engine = SCRIPT_ENGINE_MAP.get(script, "tesseract") if ocr_engine == "auto" else ocr_engine
        tess_lang = SCRIPT_TESS_LANG.get(script, "hin+san+eng")
        done = 0
        for i in ocr_needed:
            page_num = i + 1
            try:
                imgs = convert_from_path(pdf_path, dpi=300, first_page=page_num, last_page=page_num)
                if not imgs:
                    continue
                img = imgs[0]
            except Exception as e:
                logger.warning(f"  [Hybrid OCR] Page {page_num} convert failed: {e}")
                continue
            try:
                page_text, used = _ocr_page(img, script, engine, tess_lang)
            except Exception:
                del img, imgs
                continue
            if page_text.strip():
                pages.append({"page_num": page_num, "text": page_text.strip(), "method": used, "char_count": len(page_text.strip())})
            del img, imgs
            done += 1
            if done % 10 == 0:
                logger.info(f"  [Hybrid OCR] {done}/{len(ocr_needed)} pages done")
    pages.sort(key=lambda x: x["page_num"])
    logger.info(f"  [Hybrid] Total: {len(pages)} pages extracted")
    return pages


def extract_pdf(
    pdf_path: str,
    languages: Optional[list[str]] = None,
    force_ocr: bool = False,
    max_pages: Optional[int] = None,
    ocr_engine: str = "auto",
) -> list[dict]:
    """Main entry point: extract text from any PDF.

    Args:
        pdf_path: Path to PDF file
        languages: Language hints for OCR ['en', 'hi', 'sa', 'ta']
        force_ocr: Force OCR even for text PDFs
        max_pages: Max pages to process (None = all)
        ocr_engine: 'tesseract' (best for Indic), 'paddle' (English), 'auto' (detect+route)

    Returns:
        List of dicts with keys: page_num, text, method, char_count
    """
    pdf_path = str(pdf_path)
    if not os.path.exists(pdf_path):
        raise FileNotFoundError(f"PDF not found: {pdf_path}")
    file_size = os.path.getsize(pdf_path)
    if file_size == 0:
        return []
    fname = Path(pdf_path).name
    size_mb = file_size / 1024 / 1024
    logger.info(f"  Processing: {fname} ({size_mb:.1f} MB)")
    if force_ocr:
        return extract_scanned_pdf(pdf_path, languages, max_pages, ocr_engine)
    pdf_type = detect_pdf_type(pdf_path)
    logger.info(f"  Type: {pdf_type}")
    if pdf_type == "empty":
        return []
    elif pdf_type == "text":
        return extract_text_pdf(pdf_path)
    elif pdf_type == "hybrid":
        return extract_hybrid_pdf(pdf_path, languages, max_pages, ocr_engine)
    else:
        return extract_scanned_pdf(pdf_path, languages, max_pages, ocr_engine)
