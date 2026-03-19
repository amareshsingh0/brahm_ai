"""
Brahm AI - Master Ingestion Pipeline
20-Step Production OCR+RAG Pipeline Orchestrator

Pipeline Steps:
  1. PDF Discovery - Find all PDFs in raw/ folders
  2. Type Detection - text/scanned/hybrid
  3. Page Extraction - Extract pages from PDF
  4. Script Detection - Identify script (Devanagari, Tamil, etc.)
  5. OCR Routing - Choose OCR engine based on script
  6. Text Extraction - pypdf for text, PaddleOCR for scanned
  7. Text Normalization - Unicode cleanup, diacritic fixes
  8. Devanagari Correction - Fix common OCR errors
  9. Chunking - Split into ~400 token chunks with overlap
  10. Metadata Tagging - Source, page, language, category
  11. Save Structured JSON - One JSON per book
  12. (Future) Embedding Generation
  13. (Future) FAISS Index Building

Usage:
    from ingestion.pipeline import IngestionPipeline
    pipeline = IngestionPipeline(raw_dir="~/books/data/raw", output_dir="~/books/data/processed")
    pipeline.run()

    Or from CLI:
    python scripts/01_ingest_books.py --raw-dir ~/books/data/raw --output-dir ~/books/data/processed
"""

import os
import json
import time
import hashlib
import logging
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)


# Category detection from folder name (first component of relative path)
FOLDER_CATEGORY_MAP = {
    # Original categories
    "sanskrit": "sanskrit",
    "mixed": "agama_mixed",
    "vedic_astrology": "vedic_astrology",
    "jyotisha_astronomy": "jyotisha_astronomy",
    "hindi": "hindi",
    "tamil": "tamil",
    "telugu": "telugu",
    "english": "english",
    # New categories (Archive.org organized)
    "vedas": "vedas",
    "puranas": "puranas",
    "smriti_dharma": "smriti_dharma",
    "itihasa": "itihasa",
    "yoga": "yoga",
    "ayurveda": "ayurveda",
    "tantra": "tantra",
    "bauddha": "bauddha",
    "jaina": "jaina",
    "darshana": "darshana",
    "grammar_literature": "grammar_literature",
    # Phase 3.5 categories
    "samudrika": "samudrika",
    "nakshatra_jyotish": "nakshatra_jyotish",
    "muhurta_prashna": "muhurta_prashna",
    "vastu": "vastu",
    "panchang": "panchang",
    "festivals_vrat": "festivals_vrat",
    "mantra_stotra": "mantra_stotra",
    "dharma_karma": "dharma_karma",
    "gotra": "gotra",
    "special_jyotish": "special_jyotish",
    "lal_kitab": "lal_kitab",
}


class IngestionPipeline:
    """Master ingestion pipeline for processing PDFs into structured chunks."""

    def __init__(
        self,
        raw_dir: str,
        output_dir: str,
        chunk_size: int = 400,
        chunk_overlap: int = 50,
        skip_existing: bool = True,
        force_ocr: bool = False,
        max_pages: Optional[int] = None,
        ocr_engine: str = "auto",
    ):
        self.raw_dir = Path(raw_dir).expanduser()
        self.output_dir = Path(output_dir).expanduser()
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.skip_existing = skip_existing
        self.force_ocr = force_ocr
        self.max_pages = max_pages
        self.ocr_engine = ocr_engine

        # Create output directories
        self.output_dir.mkdir(parents=True, exist_ok=True)
        (self.output_dir / "json").mkdir(exist_ok=True)
        (self.output_dir / "logs").mkdir(exist_ok=True)

        # Stats tracking
        self.stats = {
            "total_pdfs": 0,
            "processed": 0,
            "skipped": 0,
            "failed": 0,
            "total_pages": 0,
            "total_chunks": 0,
            "by_category": {},
            "by_script": {},
            "by_type": {},
            "errors": [],
        }

    def discover_pdfs(self) -> list[dict]:
        """Step 1: Find all PDFs in raw/ subdirectories."""
        pdfs = []

        for root, dirs, files in os.walk(self.raw_dir):
            for f in sorted(files):
                if not f.lower().endswith(".pdf"):
                    continue

                filepath = Path(root) / f
                filesize = filepath.stat().st_size

                # Skip 0-byte files
                if filesize == 0:
                    logger.warning(f"[SKIP] 0-byte: {filepath}")
                    continue

                # Detect category from first folder component
                rel_path = filepath.relative_to(self.raw_dir)
                parts = rel_path.parts
                folder = parts[0] if len(parts) > 1 else "."
                category = FOLDER_CATEGORY_MAP.get(folder, "uncategorized")

                # File hash for dedup
                with open(filepath, "rb") as fh:
                    file_hash = hashlib.md5(fh.read(10240)).hexdigest()

                pdfs.append({
                    "path": str(filepath),
                    "filename": f,
                    "size_mb": round(filesize / 1024 / 1024, 1),
                    "category": category,
                    "folder": folder,
                    "hash": file_hash,
                })

        # Deduplicate by hash
        seen_hashes = {}
        unique_pdfs = []
        for pdf in pdfs:
            if pdf["hash"] not in seen_hashes:
                seen_hashes[pdf["hash"]] = pdf["filename"]
                unique_pdfs.append(pdf)
            else:
                logger.warning(f"[DEDUP] {pdf['filename']} matches {seen_hashes[pdf['hash']]}")

        self.stats["total_pdfs"] = len(unique_pdfs)
        logger.info(f"Discovered {len(unique_pdfs)} unique PDFs ({len(pdfs) - len(unique_pdfs)} duplicates)")
        return unique_pdfs

    def get_output_path(self, pdf_info: dict) -> Path:
        """Get output JSON path for a PDF."""
        stem = Path(pdf_info["filename"]).stem
        # Clean filename for output
        clean_name = stem.replace("%20", "_").replace(" ", "_")
        return self.output_dir / "json" / f"{clean_name}.json"

    def process_single_pdf(self, pdf_info: dict) -> Optional[dict]:
        """Process a single PDF through the full pipeline.

        Returns dict with processing results or None on failure.
        """
        from ingestion.pdf_extractor import extract_pdf, detect_pdf_type
        from ingestion.script_detector import detect_script
        from ingestion.normalizer import normalize_text
        from ingestion.chunker import chunk_pages

        filename = pdf_info["filename"]
        filepath = pdf_info["path"]
        category = pdf_info["category"]

        output_path = self.get_output_path(pdf_info)

        # Check if already processed
        if self.skip_existing and output_path.exists():
            logger.info(f"[SKIP] Already processed: {filename}")
            self.stats["skipped"] += 1
            return None

        logger.info(f"\n{'='*60}")
        logger.info(f"Processing: {filename} ({pdf_info['size_mb']} MB)")
        logger.info(f"Category: {category}")
        start_time = time.time()

        try:
            # Step 2: Detect PDF type
            pdf_type = detect_pdf_type(filepath)
            logger.info(f"  Step 2 - PDF type: {pdf_type}")

            # Step 3-6: Extract text (handles text/scanned/hybrid internally)
            pages = extract_pdf(filepath, force_ocr=self.force_ocr, ocr_engine=self.ocr_engine)

            if self.max_pages and len(pages) > self.max_pages:
                pages = pages[:self.max_pages]
                logger.info(f"  Limited to first {self.max_pages} pages")

            if not pages:
                logger.warning(f"  No text extracted from {filename}")
                self.stats["failed"] += 1
                self.stats["errors"].append({"file": filename, "error": "No text extracted"})
                return None

            logger.info(f"  Step 3-6 - Extracted {len(pages)} pages")

            # Step 4: Script detection (sample from first few pages)
            sample_text = " ".join(p["text"][:500] for p in pages[:3])
            script_info = detect_script(sample_text)
            primary_script = script_info["primary"]
            logger.info(f"  Step 4 - Script: {primary_script} ({script_info['scripts']})")

            # Step 7-8: Normalize text
            for page in pages:
                page["text"] = normalize_text(page["text"], script=primary_script)

            logger.info(f"  Step 7-8 - Text normalized")

            # Step 9-10: Chunk with metadata
            chunks = chunk_pages(
                pages,
                source=filename,
                chunk_size=self.chunk_size,
                overlap=self.chunk_overlap,
                language=script_info.get("ocr_lang", "auto"),
                category=category,
            )

            logger.info(f"  Step 9-10 - Created {len(chunks)} chunks")

            # Step 11: Save structured JSON
            result = {
                "source": filename,
                "filepath": filepath,
                "category": category,
                "pdf_type": pdf_type,
                "primary_script": primary_script,
                "scripts": script_info["scripts"],
                "is_multilingual": script_info["is_multilingual"],
                "total_pages": len(pages),
                "total_chunks": len(chunks),
                "chunk_size": self.chunk_size,
                "chunk_overlap": self.chunk_overlap,
                "processing_time_sec": round(time.time() - start_time, 2),
                "chunks": chunks,
            }

            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(result, f, ensure_ascii=False, indent=2)

            logger.info(f"  Step 11 - Saved: {output_path.name}")

            # Update stats
            self.stats["processed"] += 1
            self.stats["total_pages"] += len(pages)
            self.stats["total_chunks"] += len(chunks)
            self.stats["by_category"][category] = self.stats["by_category"].get(category, 0) + 1
            self.stats["by_script"][primary_script] = self.stats["by_script"].get(primary_script, 0) + 1
            self.stats["by_type"][pdf_type] = self.stats["by_type"].get(pdf_type, 0) + 1

            elapsed = time.time() - start_time
            logger.info(f"  DONE in {elapsed:.1f}s: {len(pages)} pages -> {len(chunks)} chunks")

            return result

        except Exception as e:
            logger.error(f"  FAILED: {filename} - {e}")
            self.stats["failed"] += 1
            self.stats["errors"].append({"file": filename, "error": str(e)})
            return None

    def _save_stats(self, pipeline_start: float):
        """Save stats to JSON (called after each book for resume-friendliness)."""
        elapsed = time.time() - pipeline_start
        self.stats["total_time_sec"] = round(elapsed, 2)
        self.stats["total_time_min"] = round(elapsed / 60, 1)
        stats_path = self.output_dir / "logs" / "ingestion_stats.json"
        with open(stats_path, "w") as f:
            json.dump(self.stats, f, indent=2)

    def _print_summary(self):
        """Print final summary to logger."""
        logger.info("\n" + "=" * 60)
        logger.info("INGESTION COMPLETE")
        logger.info("=" * 60)
        logger.info(f"Total PDFs: {self.stats['total_pdfs']}")
        logger.info(f"Processed:  {self.stats['processed']}")
        logger.info(f"Skipped:    {self.stats['skipped']}")
        logger.info(f"Failed:     {self.stats['failed']}")
        logger.info(f"Pages:      {self.stats['total_pages']}")
        logger.info(f"Chunks:     {self.stats['total_chunks']}")
        logger.info(f"Time:       {self.stats.get('total_time_min', 0)} minutes")
        logger.info(f"By category: {self.stats['by_category']}")
        logger.info(f"By script:   {self.stats['by_script']}")
        logger.info(f"By type:     {self.stats['by_type']}")
        if self.stats["errors"]:
            logger.info(f"Errors:     {len(self.stats['errors'])}")
            for err in self.stats["errors"]:
                logger.info(f"  - {err['file']}: {err['error']}")

    def run(self, categories: Optional[list[str]] = None) -> dict:
        """Run the full ingestion pipeline.

        Args:
            categories: Only process these categories (None = all)

        Returns:
            Stats dict with processing summary
        """
        logger.info("=" * 60)
        logger.info("BRAHM AI - Ingestion Pipeline Starting")
        logger.info("=" * 60)
        logger.info(f"Raw dir: {self.raw_dir}")
        logger.info(f"Output dir: {self.output_dir}")
        logger.info(f"Chunk size: {self.chunk_size}, Overlap: {self.chunk_overlap}")

        pipeline_start = time.time()

        # Step 1: Discover PDFs
        pdfs = self.discover_pdfs()

        # Filter by category if specified
        if categories:
            pdfs = [p for p in pdfs if p["category"] in categories]
            logger.info(f"Filtered to {len(pdfs)} PDFs in categories: {categories}")

        # Sort: smaller files first (faster feedback)
        pdfs.sort(key=lambda x: x["size_mb"])

        # Process each PDF (save stats after each for resume)
        for i, pdf_info in enumerate(pdfs):
            logger.info(f"\n[{i+1}/{len(pdfs)}] {pdf_info['filename']}")
            self.process_single_pdf(pdf_info)
            self._save_stats(pipeline_start)

        self._save_stats(pipeline_start)
        self._print_summary()
        return self.stats
