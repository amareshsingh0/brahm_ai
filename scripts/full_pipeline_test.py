"""
Brahm AI - Full Pipeline Test
Bhagwat Gita Infinity Dataset

Pipeline: Load Data -> Build Chunks with Metadata -> Embed -> FAISS Index -> Search -> Show Sources

Run: python full_pipeline_test.py
"""

import os
import json
import glob
import time
import sqlite3
import numpy as np

# ============================================
# STEP 1: LOAD ALL SLOKS WITH METADATA
# ============================================
print("=" * 60)
print("STEP 1: Loading Bhagwat Gita data...")
print("=" * 60)

SLOK_DIR = "data/raw/huggingface/bhagwat-gita-infinity/slok"
CHAPTER_DIR = "data/raw/huggingface/bhagwat-gita-infinity/chapter"
DB_PATH = "indexes/metadata.db"
INDEX_PATH = "indexes/gita.index"
EMBEDDINGS_PATH = "data/embeddings/gita_embeddings.npy"

os.makedirs("indexes", exist_ok=True)
os.makedirs("data/embeddings", exist_ok=True)
os.makedirs("data/chunks", exist_ok=True)

# Load chapter info
chapters = {}
for f in glob.glob(os.path.join(CHAPTER_DIR, "*.json")):
    with open(f, 'r', encoding='utf-8') as fh:
        ch = json.load(fh)
        chapters[ch["chapter_number"]] = {
            "name": ch.get("name", ""),
            "translation": ch.get("translation", ""),
            "verses_count": ch.get("verses_count", 0),
            "summary_en": ch.get("summary", {}).get("en", "") if isinstance(ch.get("summary"), dict) else ch.get("summary", "")
        }
print(f"Loaded {len(chapters)} chapters")

# Load all sloks
sloks = []
for f in sorted(glob.glob(os.path.join(SLOK_DIR, "*.json"))):
    with open(f, 'r', encoding='utf-8') as fh:
        slok = json.load(fh)
        sloks.append(slok)
print(f"Loaded {len(sloks)} sloks")

# Show sample
sample = sloks[0]
print(f"\nSample slok: {sample.get('_id', 'N/A')}")
print(f"Chapter: {sample.get('chapter')}, Verse: {sample.get('verse')}")
print(f"Slok: {str(sample.get('slok', ''))[:100]}...")


# ============================================
# STEP 2: BUILD CHUNKS WITH RICH METADATA
# ============================================
print("\n" + "=" * 60)
print("STEP 2: Building chunks with metadata...")
print("=" * 60)

# Known commentary authors mapping
COMMENTARY_AUTHORS = {
    "tej": "Swami Tejomayananda",
    "siva": "Swami Sivananda",
    "purohit": "Shri Purohit Swami",
    "chinmay": "Swami Chinmayananda",
    "san": "Shankaracharya",
    "adi": "Adi Shankaracharya",
    "gambir": "Swami Gambirananda",
    "madhav": "Madhvacharya",
    "anand": "Swami Adidevananda",
    "rams": "Swami Ramsukhdas",
    "raman": "Ramanujacharya",
    "abhinav": "Abhinavagupta",
    "sankar": "Shankaracharya (Alt)",
    "jaya": "Jayaram",
    "vallabh": "Vallabhacharya",
    "ms": "Dr. S. Sankaranarayan",
    "srid": "Sridhara Swami",
    "dhan": "Dhanpati",
    "venkat": "Vedantadeshika",
    "puru": "Purushottamji",
    "neel": "Neelkanth",
    "prabhu": "Prabhupada (ISKCON)",
}

chunks = []
chunk_id = 0

for slok in sloks:
    slok_id = slok.get("_id", f"BG{slok.get('chapter','?')}.{slok.get('verse','?')}")
    chapter_num = slok.get("chapter", 0)
    verse_num = slok.get("verse", 0)
    sanskrit_text = slok.get("slok", "")
    transliteration = slok.get("transliteration", "")

    chapter_info = chapters.get(chapter_num, {})
    chapter_name = chapter_info.get("translation", f"Chapter {chapter_num}")

    # CHUNK TYPE 1: Sanskrit slok + transliteration (for Sanskrit search)
    if sanskrit_text:
        chunks.append({
            "chunk_id": f"gita_{chunk_id:05d}",
            "text": f"{sanskrit_text}\n\nTransliteration: {transliteration}",
            "book": "Bhagavad Gita",
            "chapter": chapter_num,
            "chapter_name": chapter_name,
            "verse": verse_num,
            "slok_id": slok_id,
            "language": "sanskrit",
            "type": "slok",
            "author": "Ved Vyasa",
        })
        chunk_id += 1

    # CHUNK TYPE 2: Each commentary as separate chunk (for Hindi/English search)
    for key, author_name in COMMENTARY_AUTHORS.items():
        commentary = slok.get(key, "")
        if not commentary:
            continue

        # Some commentaries are dicts with "author" and "ht" (Hindi text)
        if isinstance(commentary, dict):
            text = commentary.get("ht", "") or commentary.get("et", "") or str(commentary)
            author_name = commentary.get("author", author_name)
        else:
            text = str(commentary)

        if len(text.strip()) < 10:
            continue

        # Detect language
        lang = "hindi"
        if text and ord(text[0]) < 128:
            lang = "english"

        chunks.append({
            "chunk_id": f"gita_{chunk_id:05d}",
            "text": f"Bhagavad Gita Chapter {chapter_num} ({chapter_name}), Verse {verse_num}\n"
                    f"Sanskrit: {sanskrit_text[:200]}\n\n"
                    f"Commentary by {author_name}:\n{text}",
            "book": "Bhagavad Gita",
            "chapter": chapter_num,
            "chapter_name": chapter_name,
            "verse": verse_num,
            "slok_id": slok_id,
            "language": lang,
            "type": "commentary",
            "author": author_name,
        })
        chunk_id += 1

print(f"Total chunks created: {len(chunks)}")
print(f"Sanskrit slok chunks: {sum(1 for c in chunks if c['type'] == 'slok')}")
print(f"Commentary chunks: {sum(1 for c in chunks if c['type'] == 'commentary')}")

# Count by language
lang_counts = {}
for c in chunks:
    lang_counts[c["language"]] = lang_counts.get(c["language"], 0) + 1
print(f"By language: {lang_counts}")

# Save chunks to JSONL
chunks_path = "data/chunks/gita_chunks.jsonl"
with open(chunks_path, 'w', encoding='utf-8') as f:
    for chunk in chunks:
        f.write(json.dumps(chunk, ensure_ascii=False) + "\n")
print(f"Saved chunks to {chunks_path}")


# ============================================
# STEP 3: SQLITE METADATA STORE
# ============================================
print("\n" + "=" * 60)
print("STEP 3: Building SQLite metadata store...")
print("=" * 60)

conn = sqlite3.connect(DB_PATH)
cursor = conn.cursor()

cursor.execute("DROP TABLE IF EXISTS chunks")
cursor.execute("DROP TABLE IF EXISTS books")

cursor.execute("""
CREATE TABLE books (
    book_id TEXT PRIMARY KEY,
    title TEXT,
    author TEXT,
    language TEXT,
    category TEXT,
    total_chunks INTEGER,
    source TEXT
)
""")

cursor.execute("""
CREATE TABLE chunks (
    chunk_id TEXT PRIMARY KEY,
    book_id TEXT,
    chapter INTEGER,
    chapter_name TEXT,
    verse INTEGER,
    slok_id TEXT,
    language TEXT,
    type TEXT,
    author TEXT,
    text_preview TEXT,
    faiss_id INTEGER
)
""")

# Insert book
cursor.execute("""
INSERT INTO books VALUES (?, ?, ?, ?, ?, ?, ?)
""", ("bhagavad_gita", "Bhagavad Gita", "Ved Vyasa", "sanskrit,hindi,english",
      "scripture", len(chunks), "Modotte/Bhagwat-Gita-Infinity"))

# Insert chunks
for i, chunk in enumerate(chunks):
    cursor.execute("""
    INSERT INTO chunks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        chunk["chunk_id"], "bhagavad_gita",
        chunk["chapter"], chunk["chapter_name"],
        chunk["verse"], chunk["slok_id"],
        chunk["language"], chunk["type"],
        chunk["author"], chunk["text"][:200],
        i  # faiss_id = position in embeddings array
    ))

conn.commit()
conn.close()
print(f"Metadata saved to {DB_PATH}")
print(f"Total records: {len(chunks)}")


# ============================================
# STEP 4: GENERATE EMBEDDINGS
# ============================================
print("\n" + "=" * 60)
print("STEP 4: Generating embeddings...")
print("=" * 60)

from sentence_transformers import SentenceTransformer

model_name = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
print(f"Loading embedding model: {model_name}")
embed_model = SentenceTransformer(model_name)

# Extract texts for embedding
texts = [chunk["text"] for chunk in chunks]

print(f"Encoding {len(texts)} chunks...")
start_time = time.time()
embeddings = embed_model.encode(texts, batch_size=64, show_progress_bar=True, normalize_embeddings=True)
elapsed = time.time() - start_time
print(f"Embeddings generated in {elapsed:.1f}s")
print(f"Shape: {embeddings.shape}")  # Should be (N, 384)

# Save embeddings
np.save(EMBEDDINGS_PATH, embeddings)
print(f"Saved to {EMBEDDINGS_PATH}")


# ============================================
# STEP 5: BUILD FAISS INDEX
# ============================================
print("\n" + "=" * 60)
print("STEP 5: Building FAISS index...")
print("=" * 60)

import faiss

dim = embeddings.shape[1]  # 384
print(f"Dimension: {dim}, Vectors: {embeddings.shape[0]}")

# HNSW index
index = faiss.IndexHNSWFlat(dim, 32)  # M=32
index.hnsw.efConstruction = 200
index.hnsw.efSearch = 64

print("Adding vectors to FAISS...")
index.add(embeddings.astype(np.float32))
print(f"Total vectors in index: {index.ntotal}")

# Save index
faiss.write_index(index, INDEX_PATH)
print(f"Saved to {INDEX_PATH}")


# ============================================
# STEP 6: BUILD BM25 INDEX
# ============================================
print("\n" + "=" * 60)
print("STEP 6: Building BM25 keyword index...")
print("=" * 60)

from rank_bm25 import BM25Okapi

tokenized_docs = [text.lower().split() for text in texts]
bm25 = BM25Okapi(tokenized_docs)
print(f"BM25 index built with {len(tokenized_docs)} documents")


# ============================================
# STEP 7: SEARCH TEST WITH SOURCE ATTRIBUTION
# ============================================
print("\n" + "=" * 60)
print("STEP 7: Search Test - Hybrid Search + Source Attribution")
print("=" * 60)

def hybrid_search(query, top_k=5):
    """FAISS + BM25 + RRF fusion + source attribution"""

    # FAISS semantic search
    query_vec = embed_model.encode([query], normalize_embeddings=True).astype(np.float32)
    faiss_D, faiss_I = index.search(query_vec, 20)

    # BM25 keyword search
    bm25_scores = bm25.get_scores(query.lower().split())
    bm25_top = sorted(range(len(bm25_scores)), key=lambda i: bm25_scores[i], reverse=True)[:20]

    # Reciprocal Rank Fusion
    rrf_scores = {}
    for rank, idx in enumerate(faiss_I[0]):
        if idx >= 0:
            rrf_scores[int(idx)] = rrf_scores.get(int(idx), 0) + 1 / (60 + rank)
    for rank, idx in enumerate(bm25_top):
        rrf_scores[idx] = rrf_scores.get(idx, 0) + 1 / (60 + rank)

    # Sort by score, get top_k
    sorted_results = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)[:top_k]

    # Get metadata from SQLite
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    results = []
    for idx, score in sorted_results:
        cursor.execute("""
            SELECT chunk_id, chapter, chapter_name, verse, slok_id,
                   language, type, author, text_preview
            FROM chunks WHERE faiss_id = ?
        """, (idx,))
        row = cursor.fetchone()

        if row:
            results.append({
                "score": round(score, 4),
                "chunk_id": row[0],
                "chapter": row[1],
                "chapter_name": row[2],
                "verse": row[3],
                "slok_id": row[4],
                "language": row[5],
                "type": row[6],
                "author": row[7],
                "text_preview": row[8],
                "full_text": chunks[idx]["text"],
            })

    conn.close()
    return results


def print_results(query, results):
    """Pretty print with source attribution"""
    print(f"\n{'─' * 60}")
    print(f"  QUERY: {query}")
    print(f"{'─' * 60}")

    for i, r in enumerate(results, 1):
        print(f"\n  [{i}] Score: {r['score']}")
        print(f"      Source: Bhagavad Gita | Chapter {r['chapter']} ({r['chapter_name']}) | Verse {r['verse']}")
        print(f"      Slok ID: {r['slok_id']}")
        print(f"      Type: {r['type']} | Language: {r['language']} | Author: {r['author']}")
        print(f"      Text: {r['full_text'][:300]}...")
        print(f"      {'─' * 50}")

    print()


# TEST QUERIES
test_queries = [
    "What does Krishna say about Karma?",
    "dharma ki paribhasha kya hai?",
    "yoga kya hai bhagavad gita me",
    "soul is eternal and cannot be destroyed",
    "arjuna ka vishad aur Krishna ka updesh",
]

print("\nRunning test queries...\n")
for query in test_queries:
    results = hybrid_search(query, top_k=3)
    print_results(query, results)


# ============================================
# STEP 8: SUMMARY
# ============================================
print("=" * 60)
print("PIPELINE TEST COMPLETE - SUMMARY")
print("=" * 60)
print(f"""
Data:
  - Source: Modotte/Bhagwat-Gita-Infinity (HuggingFace)
  - Chapters: {len(chapters)}
  - Sloks: {len(sloks)}
  - Total chunks: {len(chunks)}

Files created:
  - Chunks:     {chunks_path}
  - Embeddings: {EMBEDDINGS_PATH}
  - FAISS index: {INDEX_PATH}
  - Metadata DB: {DB_PATH}

Models used:
  - Embedding: paraphrase-multilingual-MiniLM-L12-v2 (384 dim)
  - Search: FAISS HNSW (M=32) + BM25 + RRF fusion

Source Attribution Format:
  Bhagavad Gita | Chapter X (Name) | Verse Y | Author Z
""")
print("Next: Add Qwen LLM for RAG answer generation")
print("Run: python full_pipeline_test.py")
