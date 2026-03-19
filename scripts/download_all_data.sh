#!/bin/bash
# ============================================
# Brahm AI - Master Data Download Script
# Run: bash download_all_data.sh
# ============================================

echo "============================================"
echo "Brahm AI - Downloading ALL Data Sources"
echo "============================================"

# Activate environment
source ~/ai-env/bin/activate

# ============================================
# 1. CREATE FOLDER STRUCTURE
# ============================================
echo ""
echo "[1/5] Creating folder structure..."

mkdir -p ~/books/data/raw/sanskrit
mkdir -p ~/books/data/raw/hindi
mkdir -p ~/books/data/raw/english
mkdir -p ~/books/data/raw/mixed
mkdir -p ~/books/data/hf_datasets
mkdir -p ~/books/data/swiss_ephe
mkdir -p ~/books/data/github
mkdir -p ~/books/data/chunks
mkdir -p ~/books/data/embeddings
mkdir -p ~/books/indexes

echo "[OK] Folders created"

# ============================================
# 2. DOWNLOAD PDFs (Agama Academy)
# ============================================
echo ""
echo "[2/5] Downloading Agama PDFs..."

# Kamikagama in Devanagari
wget -q --show-progress "https://agamaacademy.com/digital-library-book/Kamikagama%20in%20Devanagari.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_Kamikagama_Devanagari.pdf

# Yogaja Agamam
wget -q --show-progress "https://agamaacademy.com/digital-library-book/Yogaja%20-%20Agamam.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_Yogaja_Agamam.pdf

# Chintyagama
wget -q --show-progress "https://agamaacademy.com/digital-library-book/Chintyagama.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_Chintyagama.pdf

# Purva Karana Agama (5 parts)
wget -q --show-progress "https://agamaacademy.com/digital-library-book/Purva%20KaranaAgama%20pgs001-050.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_PurvaKaranaAgama_Part01.pdf

wget -q --show-progress "https://agamaacademy.com/digital-library-book/Purva%20KaranaAgama%20pgs051-100.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_PurvaKaranaAgama_Part02.pdf

wget -q --show-progress "https://agamaacademy.com/digital-library-book/Purva%20KaranaAgama%20pgs101-150.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_PurvaKaranaAgama_Part03.pdf

wget -q --show-progress "https://agamaacademy.com/digital-library-book/Purva%20KaranaAgama%20pgs151-200.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_PurvaKaranaAgama_Part04.pdf

wget -q --show-progress "https://agamaacademy.com/digital-library-book/Purva%20KaranaAgama%20pgs201-250.pdf" \
  -O ~/books/data/raw/sanskrit/AgamaAcademy_PurvaKaranaAgama_Part05.pdf

# Internet Archive - Agama and Tripitaka Vol 2
wget -q --show-progress "https://ia601500.us.archive.org/19/items/in.ernet.dli.2015.125412/2015.125412.Agama-And-Tripitaka-Vol2_text.pdf" \
  -O ~/books/data/raw/sanskrit/InternetArchive_Agama_Tripitaka_Vol02.pdf

# Kamika Agama from Google Drive (already uploaded)
# Already at: ~/books/data/raw/sanskrit/KamikaAgama_PurvaPada_Part1.pdf

echo "[OK] Agama PDFs downloaded"

# ============================================
# 3. DOWNLOAD HUGGING FACE DATASETS
# ============================================
echo ""
echo "[3/5] Downloading HuggingFace datasets..."

pip install -q datasets gdown

python3 << 'PYEOF'
import os
from datasets import load_dataset

save_dir = os.path.expanduser("~/books/data/hf_datasets")

# --- Already downloaded ---
# Bhagwat-Gita-Infinity at ~/books/data/raw/huggingface/bhagwat-gita-infinity/

# --- Astrology/Kundali datasets ---
print("Downloading VedAstro Planet Data...")
try:
    ds = load_dataset("vedastro-org/Astro_Planet_Data")
    ds.save_to_disk(os.path.join(save_dir, "VedAstro_PlanetData"))
    print("[OK] VedAstro Planet Data saved")
except Exception as e:
    print(f"[SKIP] VedAstro Planet Data: {e}")

print("Downloading 15000 Famous People Birth Data...")
try:
    ds = load_dataset("vedastro-org/15000-Famous-People-Birth-Date-Location")
    ds.save_to_disk(os.path.join(save_dir, "VedAstro_15000_BirthData"))
    print("[OK] 15000 Birth Data saved")
except Exception as e:
    print(f"[SKIP] 15000 Birth Data: {e}")

print("Downloading 15000 Marriage/Divorce Data...")
try:
    ds = load_dataset("vedastro-org/15000-Famous-People-Marriage-Divorce-Info")
    ds.save_to_disk(os.path.join(save_dir, "VedAstro_15000_MarriageData"))
    print("[OK] 15000 Marriage Data saved")
except Exception as e:
    print(f"[SKIP] 15000 Marriage Data: {e}")

print("Downloading Celestial Comprehensive Spiritual AI...")
try:
    ds = load_dataset("dp1812/celestial-comprehensive-spiritual-ai")
    ds.save_to_disk(os.path.join(save_dir, "Celestial_SpiritualAI"))
    print("[OK] Celestial Spiritual AI saved")
except Exception as e:
    print(f"[SKIP] Celestial Spiritual AI: {e}")

print("\nAll HuggingFace datasets done!")
PYEOF

echo "[OK] HuggingFace datasets downloaded"

# ============================================
# 4. DOWNLOAD SWISS EPHEMERIS FILES
# ============================================
echo ""
echo "[4/5] Downloading Swiss Ephemeris files..."

wget -q --show-progress "https://www.astro.com/ftp/swisseph/ephe/seas_18.se1" \
  -O ~/books/data/swiss_ephe/seas_18.se1

wget -q --show-progress "https://www.astro.com/ftp/swisseph/ephe/sepl_18.se1" \
  -O ~/books/data/swiss_ephe/sepl_18.se1

wget -q --show-progress "https://www.astro.com/ftp/swisseph/ephe/semo_18.se1" \
  -O ~/books/data/swiss_ephe/semo_18.se1

echo "[OK] Swiss Ephemeris files downloaded"

# ============================================
# 5. CLONE VEDASTRO GITHUB REPO
# ============================================
echo ""
echo "[5/5] Cloning VedAstro GitHub repo..."

if [ ! -d ~/books/data/github/VedAstro ]; then
    git clone --depth 1 https://github.com/VedAstro/VedAstro.git ~/books/data/github/VedAstro
    echo "[OK] VedAstro repo cloned"
else
    echo "[SKIP] VedAstro repo already exists"
fi

# ============================================
# VERIFY ALL DOWNLOADS
# ============================================
echo ""
echo "============================================"
echo "VERIFICATION"
echo "============================================"

echo ""
echo "--- PDFs (Sanskrit) ---"
ls -lh ~/books/data/raw/sanskrit/*.pdf 2>/dev/null || echo "  No PDFs found"

echo ""
echo "--- HuggingFace Datasets ---"
ls -d ~/books/data/hf_datasets/*/ 2>/dev/null || echo "  No datasets found"
ls -d ~/books/data/raw/huggingface/*/ 2>/dev/null || echo "  No HF raw datasets"

echo ""
echo "--- Swiss Ephemeris ---"
ls -lh ~/books/data/swiss_ephe/*.se1 2>/dev/null || echo "  No ephe files found"

echo ""
echo "--- GitHub Repos ---"
ls -d ~/books/data/github/*/ 2>/dev/null || echo "  No repos found"

echo ""
echo "============================================"
echo "ALL DOWNLOADS COMPLETE!"
echo "============================================"
echo ""
echo "File naming convention used:"
echo "  PDFs:     Source_BookName_PartNN.pdf"
echo "  HF Data:  Source_DatasetName/"
echo "  Ephe:     Original Swiss Ephemeris names"
echo ""
echo "Next steps:"
echo "  1. Verify PDFs: file ~/books/data/raw/sanskrit/*.pdf"
echo "  2. Run ingestion: python ingest_pdf.py"
echo "  3. Run search:    python smart_search.py"
