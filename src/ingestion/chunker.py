"""
Brahm AI - Smart Text Chunker
Chunks text with metadata preservation, overlap, and semantic awareness.

Supports verse-aware chunking for Sanskrit shlokas and structured texts.

Usage:
    from ingestion.chunker import chunk_text, chunk_pages
    chunks = chunk_pages(pages, source="Kamikagama.pdf", chunk_size=400, overlap=50)
"""

import re
import logging
from typing import Optional

logger = logging.getLogger(__name__)


def count_tokens_approx(text: str) -> int:
    """Approximate token count (words + punctuation).
    Rough estimate: 1 token ≈ 4 chars for English, ~3 chars for Devanagari.
    """
    # Simple word-based approximation
    return len(text.split())


def find_verse_boundaries(text: str) -> list[int]:
    """Find verse/shloka boundaries in Sanskrit/Hindi text.

    Looks for: ॥ (double danda), । (single danda), verse numbers
    """
    boundaries = []
    for match in re.finditer(r'॥\s*\d*\s*॥|॥|।।', text):
        boundaries.append(match.end())
    return boundaries


def split_into_sentences(text: str) -> list[str]:
    """Split text into sentences, respecting Sanskrit dandas and English periods."""
    # Split on: period, danda, double danda, question mark, exclamation
    sentences = re.split(r'(?<=[।॥.!?])\s+', text)
    return [s.strip() for s in sentences if s.strip()]


def chunk_text(
    text: str,
    chunk_size: int = 400,
    overlap: int = 50,
    respect_verses: bool = True,
) -> list[str]:
    """Chunk text into pieces of approximately chunk_size tokens.

    Simplified fast version - pure word-split chunking.
    Handles 1000+ page books without slowdown.

    Args:
        text: Input text
        chunk_size: Target tokens per chunk
        overlap: Overlap tokens between chunks
        respect_verses: Reserved for future use

    Returns:
        List of text chunks
    """
    if not text or not text.strip():
        return []

    words = text.split()
    total = len(words)
    if total <= chunk_size:
        return [text.strip()]

    chunks = []
    start = 0

    while start < total:
        end = min(start + chunk_size, total)
        chunk = ' '.join(words[start:end])
        chunks.append(chunk)

        start = end - overlap
        if start >= total or (end == total):
            break

    return chunks


def chunk_pages(
    pages: list[dict],
    source: str,
    chunk_size: int = 400,
    overlap: int = 50,
    language: str = "auto",
    category: str = "unknown",
) -> list[dict]:
    """Chunk extracted pages into indexed chunks with full metadata.

    Args:
        pages: List of page dicts from pdf_extractor (page_num, text, method)
        source: Source filename
        chunk_size: Target tokens per chunk
        overlap: Overlap tokens between chunks
        language: Language code or 'auto'
        category: Book category (sanskrit, jyotisha, agama, etc.)

    Returns:
        List of chunk dicts with metadata:
            chunk_id, text, source, page_num, chunk_idx,
            language, category, method, char_count, word_count
    """
    all_chunks = []
    chunk_counter = 0

    for page in pages:
        page_num = page["page_num"]
        text = page["text"]
        method = page.get("method", "unknown")

        page_chunks = chunk_text(text, chunk_size=chunk_size, overlap=overlap)

        for i, chunk in enumerate(page_chunks):
            chunk_counter += 1
            all_chunks.append({
                "chunk_id": f"{source}::p{page_num}::c{i}",
                "text": chunk,
                "source": source,
                "page_num": page_num,
                "chunk_idx": i,
                "total_chunk_num": chunk_counter,
                "language": language,
                "category": category,
                "extraction_method": method,
                "char_count": len(chunk),
                "word_count": len(chunk.split()),
            })

    logger.info(f"Chunked {source}: {len(pages)} pages -> {len(all_chunks)} chunks")
    return all_chunks


def chunk_json_dataset(
    records: list[dict],
    text_field: str,
    source: str,
    metadata_fields: Optional[list[str]] = None,
    chunk_size: int = 400,
    overlap: int = 50,
) -> list[dict]:
    """Chunk structured JSON/CSV dataset records.

    Args:
        records: List of data records
        text_field: Key containing the main text
        source: Dataset name
        metadata_fields: Additional fields to preserve as metadata
        chunk_size: Target tokens per chunk
        overlap: Overlap between chunks

    Returns:
        List of chunk dicts with metadata
    """
    if metadata_fields is None:
        metadata_fields = []

    all_chunks = []
    chunk_counter = 0

    for rec_idx, record in enumerate(records):
        text = record.get(text_field, "")
        if not text or not str(text).strip():
            continue

        text = str(text).strip()
        record_chunks = chunk_text(text, chunk_size=chunk_size, overlap=overlap)

        for i, chunk in enumerate(record_chunks):
            chunk_counter += 1
            chunk_data = {
                "chunk_id": f"{source}::r{rec_idx}::c{i}",
                "text": chunk,
                "source": source,
                "record_idx": rec_idx,
                "chunk_idx": i,
                "total_chunk_num": chunk_counter,
                "char_count": len(chunk),
                "word_count": len(chunk.split()),
            }

            # Preserve requested metadata fields
            for field in metadata_fields:
                if field in record:
                    chunk_data[field] = record[field]

            all_chunks.append(chunk_data)

    logger.info(f"Chunked {source}: {len(records)} records -> {len(all_chunks)} chunks")
    return all_chunks
