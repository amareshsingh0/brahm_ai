# Brahm AI - Download Sources & Coverage Map
# Last Updated: 2026-03-10

## Source Status (Actual Results)

| # | Source | Format | Actual Size | Status | Notes |
|---|--------|--------|-------------|--------|-------|
| 1 | **GRETIL** | HTML/XML | — | **BLOCKED (403)** | wget blocked from VM IP, even with browser User-Agent |
| 2 | **Sacred-Texts.com** | HTML | — | **BLOCKED (Cloudflare)** | wget fails, needs browser session |
| 3 | **SuttaCentral** | JSON | **306 MB** | **CLONED** | Full Pali Canon + English, bilara-data published branch |
| 4 | **SARIT** | TEI-XML | **204 MB** | **CLONED** | 85 XML files, curated Sanskrit scholarly texts |
| 5 | **Archive.org** | PDF | **6.1 GB** | **DOWNLOADED** | 31 items, 62 PDFs via `ia` CLI tool |
| 6 | **Sangraha** (HF) | Parquet | — | Researched only | 251B tokens web corpus, not curated classical texts |
| 7 | **Muktabodha** | Scanned images | — | Manual only | No bulk download, page-by-page browsing |

---

## Coverage Matrix: BOOKS_TO_UPLOAD vs Sources

### Vedas & Upanishads → `vedas/`

| Book | GRETIL | Sacred-Texts | Archive.org | SARIT | Status |
|------|--------|--------------|-------------|-------|--------|
| Rigveda | ✅ San | ✅ Eng (Griffith) | ✅ PDF | — | **3 sources** |
| Samaveda | ✅ San | ✅ Eng (Griffith) | ✅ PDF | — | **3 sources** |
| Yajurveda (Krishna) | ✅ San | ✅ Eng (Keith) | — | — | **2 sources** |
| Yajurveda (Shukla) | ✅ San | ✅ Eng (Griffith) | ✅ PDF | — | **3 sources** |
| Atharvaveda | ✅ San | ✅ Eng (Griffith) | ✅ PDF | — | **3 sources** |
| 108 Upanishads | ✅ (major ones) | ✅ (SBE + 30 minor) | — | — | **2 sources** |
| Brihadaranyaka Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Chandogya Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Katha Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Mundaka Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Mandukya + Karika | ✅ San | ✅ Eng | — | — | **2 sources** |
| Taittiriya Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Shvetashvatara Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Isha Up. | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Shatapatha Brahmana | ✅ San | ✅ Eng (SBE) | — | — | **2 sources** |
| Taittiriya Brahmana | ✅ San | — | — | — | **1 source** |
| Aitareya Brahmana | ✅ San | — | — | — | **1 source** |
| Rigveda Max Muller | — | ✅ Eng (SBE) | ✅ PDF | — | **2 sources** |

### Smritis & Dharmashastra → `smriti_dharma/`

| Book | GRETIL | Sacred-Texts | Archive.org | SARIT |
|------|--------|--------------|-------------|-------|
| Manu Smriti | ✅ San | ✅ Eng (SBE 25) | ✅ PDF | Likely |
| Yajnavalkya Smriti | ✅ San | — | — | Possible |
| Parasara Smriti | ✅ San | — | — | — |
| Narada Smriti | ✅ San | — | — | — |
| Vishnu Smriti | ✅ San | ✅ Eng (SBE 7) | — | — |
| Apastamba Dharmasutra | ✅ San | ✅ Eng (SBE) | — | — |
| Gautama Dharmasutra | ✅ San | ✅ Eng (SBE) | — | — |
| Baudhayana Dharmasutra | ✅ San | — | — | — |
| Arthashastra | ✅ San | ✅ Eng | ✅ PDF | Likely |
| Nitisara (Kamandaka) | ✅ San | — | — | — |

### Puranas → `puranas/`

| Book | GRETIL | Sacred-Texts | Archive.org | Status |
|------|--------|--------------|-------------|--------|
| Bhagavata Purana | ✅ San | ✅ Eng | ✅ PDF | **3 sources** |
| Vishnu Purana | ✅ San | ✅ Eng (Wilson) | ✅ PDF | **3 sources** |
| Shiva Purana | ✅ San | — | Search | **1-2 sources** |
| Garuda Purana | ✅ San | ✅ Eng | — | **2 sources** |
| Agni Purana | ✅ San | — | Search | **1 source** |
| Markandeya Purana | ✅ San | ✅ Eng (Pargiter) | — | **2 sources** |
| Matsya Purana | ✅ San | — | Search | **1 source** |
| Devi Bhagavata | — | ✅ Eng | — | **1 source** |
| Other Puranas | ✅ (partial) | Limited | Search | **1 source** |

### Itihasa & Epics → `itihasa/`

| Book | GRETIL | Sacred-Texts | Archive.org | Status |
|------|--------|--------------|-------------|--------|
| Mahabharata Ganguli | — | ✅ Eng (18 Parvas, FULL) | ✅ PDF | **2 sources** |
| ~~Ramayana Valmiki~~ | ~~✅ San~~ | ~~✅ Eng (Griffith)~~ | — | **ON VM** |
| Adhyatma Ramayana | ✅ San | — | Search | **1 source** |
| Yoga Vasistha | ✅ San | ✅ Eng | — | **2 sources** |
| ~~Bhagavad Gita~~ | — | — | — | **ON VM** |
| Gita - Tilak | — | — | Search | Search Archive.org |
| Gita - Shankara Bhashya | ✅ San | ✅ Eng (SBE 8) | — | **2 sources** |
| Ashtavakra Gita | ✅ San | — | Search | **1 source** |
| Avadhuta Gita | ✅ San | — | Search | **1 source** |

### Yoga → `yoga/`

| Book | GRETIL | Sacred-Texts | Status |
|------|--------|--------------|--------|
| Yoga Sutras | ✅ San | ✅ Eng (Johnston) | **2 sources** |
| Hatha Yoga Pradipika | ✅ San | ✅ Eng (Pancham Sinh) | **2 sources** |
| Gheranda Samhita | ✅ San | — | **1 source** |
| Shiva Samhita | ✅ San | — | **1 source** |
| Yoga Yajnavalkya | ✅ San | — | **1 source** |
| Goraksha Shataka | ✅ San | — | **1 source** |

### Ayurveda → `ayurveda/`

| Book | GRETIL | Status |
|------|--------|--------|
| Charaka Samhita | ✅ San (partial) | **1 source** |
| Sushruta Samhita | ✅ San (partial) | **1 source** |
| Ashtanga Hridayam | ✅ San (partial) | **1 source** |
| Others | Search GRETIL sastra/ | Check after download |

### Tantra → `tantra/`

| Book | GRETIL | Sacred-Texts | Muktabodha | Status |
|------|--------|--------------|------------|--------|
| Mahanirvana Tantra | ✅ San | — | Possible | **1 source** |
| Kularnava Tantra | ✅ San | — | Possible | **1 source** |
| Tantraloka | ✅ San | — | ✅ (scans) | **2 sources** |
| Vijnana Bhairava | ✅ San | — | ✅ (scans) | **2 sources** |
| Shiva Sutras | ✅ San | — | ✅ (scans) | **2 sources** |
| Pratyabhijnahridayam | ✅ San | — | ✅ (scans) | **2 sources** |
| Spanda Karikas | ✅ San | — | ✅ (scans) | **2 sources** |
| Soundarya Lahari | ✅ San | — | — | **1 source** |
| Devi Mahatmya | ✅ San | ✅ Eng | — | **2 sources** |

### Buddhist → `bauddha/`

| Book | SuttaCentral | GRETIL | Sacred-Texts | Status |
|------|-------------|--------|--------------|--------|
| Dhammapada | ✅ Pali+Eng | ✅ Pali | ✅ Eng (SBE) | **3 sources** |
| Digha Nikaya | ✅ Pali+Eng (FULL) | ✅ Pali | ✅ Eng (partial) | **3 sources** |
| Majjhima Nikaya | ✅ Pali+Eng (FULL) | ✅ Pali | — | **2 sources** |
| Samyutta Nikaya | ✅ Pali+Eng (FULL) | ✅ Pali | — | **2 sources** |
| Anguttara Nikaya | ✅ Pali+Eng (FULL) | ✅ Pali | — | **2 sources** |
| Sutta Nipata | ✅ Pali+Eng | ✅ Pali | ✅ Eng (SBE) | **3 sources** |
| Jataka Tales | — | — | ✅ Eng | **1 source** |
| Buddhacharita | — | ✅ San | ✅ Eng (SBE) | **2 sources** |
| Milindapanha | Partial (sc-data) | ✅ Pali | ✅ Eng (SBE) | **3 sources** |
| Nagarjuna MMK | — | ✅ San | — | **1 source** |
| Vasubandhu AK | — | ✅ San | — | **1 source** |
| Diamond Sutra | — | ✅ San | ✅ Eng | **2 sources** |
| Heart Sutra | — | ✅ San | ✅ Eng | **2 sources** |
| Lankavatara | — | ✅ San | — | **1 source** |

### Jain → `jaina/`

| Book | GRETIL | Sacred-Texts | Status |
|------|--------|--------------|--------|
| Acharanga Sutra | ✅ Prakrit | ✅ Eng (SBE 22) | **2 sources** |
| Tattvartha Sutra | ✅ San | — | **1 source** |
| Kalpa Sutra | ✅ Prakrit | ✅ Eng (SBE 22) | **2 sources** |
| Uttaradhyayana | ✅ Prakrit | ✅ Eng (SBE 45) | **2 sources** |
| Kundakunda texts | ✅ Prakrit | — | **1 source** |

### Darshana → `darshana/`

| Book | GRETIL | Sacred-Texts | SARIT | Status |
|------|--------|--------------|-------|--------|
| Brahma Sutras + Shankara | ✅ San | ✅ Eng (SBE 34) | Possible | **2 sources** |
| Brahma Sutras - Ramanuja | ✅ San | ✅ Eng (SBE 48) | — | **2 sources** |
| Vivekachudamani | ✅ San | — | Possible | **1 source** |
| Panchadashi | ✅ San | — | — | **1 source** |
| Nyaya Sutras | ✅ San | — | — | **1 source** |
| Samkhya Karika | ✅ San | — | — | **1 source** |
| Tarka Sangraha | ✅ San | — | — | **1 source** |

### Grammar & Literature → `grammar_literature/`

| Book | GRETIL | Sacred-Texts | Status |
|------|--------|--------------|--------|
| Ashtadhyayi | ✅ San | — | **1 source** |
| Mahabhashya | ✅ San | — | **1 source** |
| Meghaduta | ✅ San | — | **1 source** |
| Shakuntalam | ✅ San | — | **1 source** |
| Raghuvamsha | ✅ San | — | **1 source** |
| Natyashastra | ✅ San | — | **1 source** |
| Panchatantra | ✅ San | — | **1 source** |

---

## Download Estimates

| Source | Est. Size | Download Time (100Mbps) |
|--------|----------|------------------------|
| GRETIL (full Sanskrit+Pali) | ~500MB | ~1 min |
| Sacred-Texts (Hindu+Buddhist+Jain) | ~2-3GB | ~5 min |
| SuttaCentral (bilara-data + sc-data) | ~500MB | ~1 min |
| SARIT corpus | ~300MB | ~30 sec |
| Archive.org (select items) | ~3-5GB | ~10 min |
| Sangraha (sample 10K docs) | ~50MB | ~2 min |
| **TOTAL** | **~7-10 GB** | **~20 min** |

---

## VM Run Commands

### Quick Start (run in tmux)
```bash
# SSH into VM
gcloud compute ssh amareshsingh2005@brahm --zone=us-central1-a

# Activate venv
source ~/ai-env/bin/activate

# Upload script (via Google Drive + gdown) or copy-paste into:
nano ~/books/scripts/download_all_sources.sh

# Make executable and run
chmod +x ~/books/scripts/download_all_sources.sh
bash ~/books/scripts/download_all_sources.sh 2>&1 | tee ~/download_all.log

# Or run in background
nohup bash ~/books/scripts/download_all_sources.sh > ~/download_all.log 2>&1 &
tail -f ~/download_all.log
```

### Run Individual Sources (if you want to test one at a time)
```bash
# Just GRETIL
wget -r -np -nH --cut-dirs=2 -e robots=off --wait=1 --random-wait -R "index.html*" -P ~/books/data/raw/gretil/sanskrit/ "https://gretil.sub.uni-goettingen.de/gretil/1_sanskr/"

# Just SARIT
git clone --depth 1 https://github.com/sarit/SARIT-corpus.git ~/books/data/raw/sarit/SARIT-corpus

# Just SuttaCentral
git clone --depth 1 --branch published --single-branch https://github.com/suttacentral/bilara-data.git ~/books/data/raw/suttacentral/bilara-data

# Just Sacred-Texts Hindu
wget -r --no-clobber --no-parent --wait=1 --random-wait -P ~/books/data/raw/sacred_texts/hindu/ "https://www.sacred-texts.com/hin/"
```

---

## Post-Download: Need New Extractors

Current pipeline only handles PDFs. New data needs:

1. **`src/ingestion/html_extractor.py`** — For GRETIL (.htm) and Sacred-Texts (.html)
2. **`src/ingestion/xml_extractor.py`** — For SARIT (TEI-XML) and GRETIL XML
3. **`src/ingestion/json_extractor.py`** — For SuttaCentral (bilara-data JSON) and HF datasets
4. **`src/ingestion/text_extractor.py`** — For plain .txt files

These feed into existing chunker.py → pipeline.py → embedding.
