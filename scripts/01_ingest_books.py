#!/usr/bin/env python3
"""
Brahm AI - Book Ingestion CLI
Run on VM: python scripts/01_ingest_books.py

Options:
    --raw-dir       Raw PDF directory (default: ~/books/data/raw)
    --output-dir    Output directory (default: ~/books/data/processed)
    --category      Only process specific category (sanskrit, vedic_astrology, etc.)
    --chunk-size    Tokens per chunk (default: 400)
    --overlap       Overlap tokens (default: 50)
    --force-ocr     Force OCR even for text PDFs
    --max-pages     Limit pages per PDF (for testing)
    --single        Process single PDF file
    --list-only     Only list discovered PDFs, don't process
"""

import sys
import os
import argparse
import logging

# Add project root to path
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)

from src.ingestion.pipeline import IngestionPipeline


def setup_logging(output_dir: str):
    """Configure logging to both console and file."""
    os.makedirs(os.path.join(output_dir, "logs"), exist_ok=True)

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%H:%M:%S",
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler(
                os.path.join(output_dir, "logs", "ingestion.log"),
                encoding="utf-8",
            ),
        ],
    )


def main():
    parser = argparse.ArgumentParser(description="Brahm AI - Book Ingestion Pipeline")
    parser.add_argument("--raw-dir", default="~/books/data/raw", help="Raw PDF directory")
    parser.add_argument("--output-dir", default="~/books/data/processed", help="Output directory")
    parser.add_argument("--category", help="Only process this category")
    parser.add_argument("--chunk-size", type=int, default=400, help="Tokens per chunk")
    parser.add_argument("--overlap", type=int, default=50, help="Overlap tokens")
    parser.add_argument("--force-ocr", action="store_true", help="Force OCR for all PDFs")
    parser.add_argument("--max-pages", type=int, help="Max pages per PDF (for testing)")
    parser.add_argument("--single", help="Process single PDF file path")
    parser.add_argument("--list-only", action="store_true", help="Only list PDFs, don't process")
    parser.add_argument("--ocr-engine", default="auto", choices=["auto", "tesseract", "paddle"],
                        help="OCR engine: auto (detect+route), tesseract (best Indic), paddle (English)")
    args = parser.parse_args()

    raw_dir = os.path.expanduser(args.raw_dir)
    output_dir = os.path.expanduser(args.output_dir)

    setup_logging(output_dir)
    logger = logging.getLogger(__name__)

    pipeline = IngestionPipeline(
        raw_dir=raw_dir,
        output_dir=output_dir,
        chunk_size=args.chunk_size,
        chunk_overlap=args.overlap,
        force_ocr=args.force_ocr,
        max_pages=args.max_pages,
        ocr_engine=args.ocr_engine,
    )

    if args.list_only:
        pdfs = pipeline.discover_pdfs()
        print(f"\nFound {len(pdfs)} PDFs:\n")
        print(f"{'#':<4} {'Size':>8} {'Category':<20} {'Filename'}")
        print("-" * 80)
        for i, pdf in enumerate(sorted(pdfs, key=lambda x: x["category"])):
            print(f"{i+1:<4} {pdf['size_mb']:>6.1f}M  {pdf['category']:<20} {pdf['filename']}")
        return

    if args.single:
        # Process single file
        filepath = os.path.expanduser(args.single)
        if not os.path.exists(filepath):
            print(f"File not found: {filepath}")
            sys.exit(1)

        pdf_info = {
            "path": filepath,
            "filename": os.path.basename(filepath),
            "size_mb": os.path.getsize(filepath) / 1024 / 1024,
            "category": args.category or "uncategorized",
            "folder": os.path.dirname(filepath),
            "hash": "single",
        }
        result = pipeline.process_single_pdf(pdf_info)
        if result:
            print(f"\nDone! {result['total_pages']} pages -> {result['total_chunks']} chunks")
            print(f"Output: {pipeline.get_output_path(pdf_info)}")
    else:
        # Process all
        categories = [args.category] if args.category else None
        stats = pipeline.run(categories=categories)
        print(f"\nPipeline complete! {stats['processed']} books, {stats['total_chunks']} chunks")


if __name__ == "__main__":
    main()
