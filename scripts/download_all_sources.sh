#!/bin/bash
# ============================================================
# Brahm AI - Master Download Script
# Downloads texts from all verified sources
# Run on VM: source ~/ai-env/bin/activate && bash ~/books/scripts/download_all_sources.sh
# ============================================================

set -e
echo "=========================================="
echo "  Brahm AI - Master Source Downloader"
echo "=========================================="

BASE=~/books/data/raw
mkdir -p $BASE/{vedas,smriti_dharma,puranas,itihasa,yoga,ayurveda,tantra,bauddha,jaina,darshana,grammar_literature}
mkdir -p $BASE/gretil/{sanskrit,pali,xml}
mkdir -p $BASE/sacred_texts/{hindu,buddhist,jain}
mkdir -p $BASE/suttacentral
mkdir -p $BASE/sarit
mkdir -p $BASE/archive_org
mkdir -p $BASE/sangraha

# ============================================================
# SOURCE 1: GRETIL (Goettingen) — Plain text / HTML / XML
# ~2000+ Sanskrit texts, wget-able, no auth needed
# PRIORITY: ★★★★★ (HIGHEST — covers most of wish list)
# ============================================================
echo ""
echo "===== [1/7] GRETIL — Sanskrit Texts ====="

# 1A. Vedas, Brahmanas, Upanishads
echo ">> Downloading Vedic texts (Vedas, Brahmanas, Upanishads)..."
wget -r -np -nH --cut-dirs=3 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/sanskrit/veda/ \
  "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/1_veda/" 2>&1 | tail -1

# 1B. Epics (Mahabharata, Ramayana)
echo ">> Downloading Epics (Mahabharata, Ramayana)..."
wget -r -np -nH --cut-dirs=3 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/sanskrit/epic/ \
  "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/2_epic/" 2>&1 | tail -1

# 1C. Puranas
echo ">> Downloading Puranas..."
wget -r -np -nH --cut-dirs=3 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/sanskrit/purana/ \
  "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/3_purana/" 2>&1 | tail -1

# 1D. Religious literature (Buddhist Sanskrit, Jain)
echo ">> Downloading Religious literature (Buddhist/Jain Sanskrit)..."
wget -r -np -nH --cut-dirs=3 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/sanskrit/rellit/ \
  "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/4_rellit/" 2>&1 | tail -1

# 1E. Poetry (Kalidasa, etc.)
echo ">> Downloading Poetry (Kalidasa, Kavya)..."
wget -r -np -nH --cut-dirs=3 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/sanskrit/poetry/ \
  "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/5_poetry/" 2>&1 | tail -1

# 1F. Shastra (Dharmashastra, Arthashastra, Ayurveda, Nyaya, Yoga, Grammar)
echo ">> Downloading Shastra (Dharma, Artha, Ayurveda, Darshana, Grammar)..."
wget -r -np -nH --cut-dirs=3 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/sanskrit/sastra/ \
  "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/6_sastra/" 2>&1 | tail -1

# 1G. Pali texts (Buddhist canon)
echo ">> Downloading Pali Buddhist texts..."
wget -r -np -nH --cut-dirs=2 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/pali/ \
  "https://gretil.sub.uni-goettingen.de/gretil/2_pali/" 2>&1 | tail -1

# 1H. Prakrit texts (Jain)
echo ">> Downloading Prakrit texts (Jain)..."
wget -r -np -nH --cut-dirs=2 -e robots=off --wait=1 --random-wait \
  -R "index.html*" \
  -P $BASE/gretil/prakrit/ \
  "https://gretil.sub.uni-goettingen.de/gretil/3_prakrt/" 2>&1 | tail -1

# 1I. TEI/XML versions (structured, best for parsing)
echo ">> Downloading TEI/XML corpus..."
wget -r -np -nH --cut-dirs=2 -e robots=off --wait=1 --random-wait \
  -A "*.xml" \
  -P $BASE/gretil/xml/ \
  "https://gretil.sub.uni-goettingen.de/gretil/corpustei/" 2>&1 | tail -1

echo ">> GRETIL download complete!"

# ============================================================
# SOURCE 2: SARIT — Sanskrit TEI-XML Corpus (GitHub)
# ~200-400 MB, curated scholarly texts
# PRIORITY: ★★★★☆
# ============================================================
echo ""
echo "===== [2/7] SARIT — Sanskrit XML Corpus ====="

if [ ! -d "$BASE/sarit/SARIT-corpus" ]; then
  echo ">> Cloning SARIT corpus (shallow)..."
  git clone --depth 1 https://github.com/sarit/SARIT-corpus.git $BASE/sarit/SARIT-corpus
  echo ">> SARIT clone complete!"
else
  echo ">> SARIT already cloned, skipping."
fi

# ============================================================
# SOURCE 3: SuttaCentral — Buddhist Pali Canon + English (GitHub)
# JSON format, Pali+English aligned, CC0 license
# Covers: Dhammapada, 5 Nikayas, Sutta Nipata
# PRIORITY: ★★★★★ (for Buddhist texts)
# ============================================================
echo ""
echo "===== [3/7] SuttaCentral — Buddhist Texts ====="

# 3A. bilara-data (main: JSON, Pali+English, published branch)
if [ ! -d "$BASE/suttacentral/bilara-data" ]; then
  echo ">> Cloning bilara-data (Pali+English JSON, published branch)..."
  git clone --depth 1 --branch published --single-branch \
    https://github.com/suttacentral/bilara-data.git \
    $BASE/suttacentral/bilara-data
  echo ">> bilara-data clone complete!"
else
  echo ">> bilara-data already cloned, skipping."
fi

# 3B. sc-data (legacy HTML, has Milindapanha and other texts)
if [ ! -d "$BASE/suttacentral/sc-data" ]; then
  echo ">> Cloning sc-data (legacy HTML texts)..."
  git clone --depth 1 https://github.com/suttacentral/sc-data.git \
    $BASE/suttacentral/sc-data
  echo ">> sc-data clone complete!"
else
  echo ">> sc-data already cloned, skipping."
fi

# ============================================================
# SOURCE 4: Sacred-Texts.com — English Translations (wget HTML)
# Public domain, pre-1923 translations
# Covers: Vedas, Mahabharata, Upanishads, Puranas, Gita, Yoga
# PRIORITY: ★★★★☆ (English translations only, no Sanskrit)
# ============================================================
echo ""
echo "===== [4/7] Sacred-Texts.com — English Translations ====="

# 4A. Rigveda (Griffith, all 10 Mandalas)
echo ">> Downloading Rigveda (Griffith)..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/rigveda/" 2>&1 | tail -1

# 4B. Sama Veda
echo ">> Downloading Sama Veda..."
wget --no-clobber -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/sv.htm" 2>&1 | tail -1

# 4C. White Yajurveda (Griffith)
echo ">> Downloading Yajurveda..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/wyv/" 2>&1 | tail -1

# 4D. Atharvaveda
echo ">> Downloading Atharvaveda..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/av/" 2>&1 | tail -1

# 4E. Upanishads (SBE vols 1 & 15 — Max Muller)
echo ">> Downloading Upanishads (SBE)..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/sbe01/" 2>&1 | tail -1
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/sbe15/" 2>&1 | tail -1

# 4F. Mahabharata Ganguli (18 Parvas — the crown jewel)
echo ">> Downloading Mahabharata (Ganguli, 18 Parvas)..."
for i in $(seq -w 1 18); do
  wget -r --no-clobber --no-parent --wait=1 --random-wait \
    -P $BASE/sacred_texts/hindu/ \
    "https://www.sacred-texts.com/hin/m0${i}/" 2>&1 | tail -1
done

# 4G. Bhagavad Gita (Edwin Arnold + SBE)
echo ">> Downloading Bhagavad Gita..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/gita/" 2>&1 | tail -1
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/sbe08/" 2>&1 | tail -1

# 4H. Vishnu Purana (Wilson)
echo ">> Downloading Vishnu Purana..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/vp/" 2>&1 | tail -1

# 4I. Markandeya Purana
echo ">> Downloading Markandeya Purana..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/mp/" 2>&1 | tail -1

# 4J. Garuda Purana
echo ">> Downloading Garuda Purana..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/gpu/" 2>&1 | tail -1

# 4K. Devi Bhagavata Purana
echo ">> Downloading Devi Bhagavata Purana..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/db/" 2>&1 | tail -1

# 4L. Ramayana (Griffith)
echo ">> Downloading Ramayana (Griffith)..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/rama/" 2>&1 | tail -1

# 4M. Arthashastra (Shamasastry)
echo ">> Downloading Arthashastra..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/artu/" 2>&1 | tail -1

# 4N. Manusmriti (SBE 25)
echo ">> Downloading Manusmriti..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/manu.htm" 2>&1 | tail -1

# 4O. Yoga Sutras + Hatha Yoga Pradipika
echo ">> Downloading Yoga texts..."
wget --no-clobber -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/yogasutr.htm" 2>&1 | tail -1
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/hyp/" 2>&1 | tail -1

# 4P. Vedanta Sutras (Sankara + Ramanuja bhashya, SBE 34 & 38)
echo ">> Downloading Vedanta Sutras (Brahma Sutras)..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/sbe34/" 2>&1 | tail -1
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/sbe38/" 2>&1 | tail -1

# 4Q. Thirty Minor Upanishads
echo ">> Downloading 30 Minor Upanishads..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/hindu/ \
  "https://www.sacred-texts.com/hin/tmu/" 2>&1 | tail -1

# 4R. Buddhist texts
echo ">> Downloading Buddhist texts (Dhammapada, Jataka, Suttas)..."
wget -r --no-clobber --no-parent --wait=2 --random-wait \
  -P $BASE/sacred_texts/buddhist/ \
  "https://www.sacred-texts.com/bud/" 2>&1 | tail -1

# 4S. Jain texts
echo ">> Downloading Jain texts (Acaranga, Kalpa, Uttaradhyayana)..."
wget -r --no-clobber --no-parent --wait=1 --random-wait \
  -P $BASE/sacred_texts/jain/ \
  "https://www.sacred-texts.com/jai/" 2>&1 | tail -1

echo ">> Sacred-Texts download complete!"

# ============================================================
# SOURCE 5: Archive.org — PDFs (using ia CLI)
# Covers: SBE series, specific Puranas, Vedas
# PRIORITY: ★★★★☆ (PDF format, works with existing pipeline)
# ============================================================
echo ""
echo "===== [5/7] Archive.org — PDF Downloads ====="

# Install ia tool if not present
pip install internetarchive 2>/dev/null

# Key items to download
ARCHIVE_ITEMS=(
  "rigvedacomplete"
  "SamaVedaSamhita"
  "TheTextsOfTheWhiteYajurveda"
  "HymnsOfTheAtharvaVeda"
  "TheMahabharataOfKrishna-dwaipayanaVyasa"
  "SrimadBhagavatam"
  "Arthashastra_Of_Chanakya"
  "TheLawsOfManu-SBE25"
  "vishnu-puran"
)

for item in "${ARCHIVE_ITEMS[@]}"; do
  echo ">> Downloading: $item"
  ia download "$item" --glob="*.pdf" --destdir=$BASE/archive_org/ 2>&1 | tail -1 || echo "  (item may not exist, skipping)"
done

# Search for more Puranas
echo ">> Searching for more Puranas on Archive.org..."
ia search "subject:puranas AND mediatype:texts" -f identifier -f title 2>/dev/null | head -20

echo ">> Archive.org downloads complete!"

# ============================================================
# SOURCE 6: HuggingFace ai4bharat/sangraha — Sanskrit NLP corpus
# 251B tokens, mainly web-scraped, useful for fine-tuning
# PRIORITY: ★★☆☆☆ (general corpus, not curated classical texts)
# Only download verified Sanskrit subset (~1.3B tokens)
# ============================================================
echo ""
echo "===== [6/7] HuggingFace Sangraha — Sanskrit Corpus ====="

echo ">> Downloading verified Sanskrit subset..."
python3 -c "
from datasets import load_dataset
import json, os

out_dir = os.path.expanduser('~/books/data/raw/sangraha')
os.makedirs(out_dir, exist_ok=True)

print('Loading verified Sanskrit split (streaming)...')
ds = load_dataset('ai4bharat/sangraha', 'verified', split='san', streaming=True)

# Save first 10000 docs as sample (full dataset is massive)
count = 0
with open(os.path.join(out_dir, 'sangraha_verified_san_sample.jsonl'), 'w') as f:
    for row in ds:
        f.write(json.dumps(row, ensure_ascii=False) + '\n')
        count += 1
        if count >= 10000:
            break
        if count % 1000 == 0:
            print(f'  Saved {count} docs...')

print(f'Done! Saved {count} Sanskrit docs to {out_dir}/')
" 2>&1

echo ">> Sangraha download complete!"

# ============================================================
# SOURCE 7: Muktabodha — Tantra/Agama manuscripts
# Manual browse required, no bulk download
# PRIORITY: ★★★☆☆ (rare texts but scanned images only)
# ============================================================
echo ""
echo "===== [7/7] Muktabodha — Manual Browse Required ====="
echo ">> Muktabodha (muktalib7.com) requires manual browsing."
echo ">> Visit: https://muktalib7.com/DL_CATALOG_ROOT/digital_library.htm"
echo ">> Focus on: Tantraloka, Vijnana Bhairava, Netra Tantra, Svacchanda Tantra"
echo ">> Format: Scanned manuscript images (need OCR)"
echo ">> No automated download available."

# ============================================================
# POST-DOWNLOAD: Summary & Stats
# ============================================================
echo ""
echo "=========================================="
echo "  DOWNLOAD COMPLETE — Summary"
echo "=========================================="
echo ""
echo "Checking downloaded data sizes..."
echo ""
du -sh $BASE/gretil/ 2>/dev/null || echo "GRETIL: not downloaded"
du -sh $BASE/sarit/ 2>/dev/null || echo "SARIT: not downloaded"
du -sh $BASE/suttacentral/ 2>/dev/null || echo "SuttaCentral: not downloaded"
du -sh $BASE/sacred_texts/ 2>/dev/null || echo "Sacred-Texts: not downloaded"
du -sh $BASE/archive_org/ 2>/dev/null || echo "Archive.org: not downloaded"
du -sh $BASE/sangraha/ 2>/dev/null || echo "Sangraha: not downloaded"
echo ""
echo "Total raw data:"
du -sh $BASE/ 2>/dev/null
echo ""
echo "=========================================="
echo "  NEXT STEPS:"
echo "  1. Build text_extractor.py for HTML/XML/JSON"
echo "  2. Run ingestion pipeline on new data"
echo "  3. Build embeddings & FAISS indexes"
echo "=========================================="
