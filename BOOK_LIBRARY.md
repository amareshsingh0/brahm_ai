# Brahm AI - Book Library
# Last Updated: 2026-03-13
# Total: 394 PDF files (~36 GB) across 26 categories
# Wish List: 212/212 books downloaded (100%) — Phase 3 (162) + Phase 3.5 (50) + SARIT 85 XML + SuttaCentral 42K JSON
# Ingestion: 355+/394 PDFs processed → JSONs (BATCH C+ final 6 processes running)


## Total Collection Summary
| Data Type | Count | Size | Format |
|---|---|---|---|
| PDF Files (26 categories) | **~1,311** | **~36 GB** | Scanned + Text PDFs |
| SARIT Sanskrit Corpus | 85 | 204 MB | TEI-XML |
| SuttaCentral Pali Canon | 42,000+ | 306 MB | JSON (Pali + English) |
| **GRAND TOTAL** | **~43,396+ files** | **~36.5 GB** | **Multi-format** |

### PDFs by Phase
| Phase | Wish List Items | Actual PDFs | Size | Categories |
|---|---|---|---|---|
| Pre-existing (Phase 1-2) | — | ~122 | ~8.6 GB | agama_mixed, jyotisha, sanskrit, vedic_astrology |
| Phase 3 (Batch 1-5) | 162 | ~1,129 | ~20.4 GB | vedas, puranas, smriti, itihasa, yoga, ayurveda, tantra, bauddha, jaina, darshana, grammar |
| Phase 3.5 (Batch 6) | 50 | 60 | ~7 GB | samudrika, nakshatra, muhurta, vastu, panchang, festivals, mantra, dharma, gotra, special_jyotish, lal_kitab |
| **Total** | **212** | **~1,311** | **~36 GB** | **26 categories** |

### Languages Covered
| Language | Categories |
|---|---|
| Sanskrit | vedas, puranas, tantra, darshana, grammar, jaina, ayurveda, mantra_stotra, gotra |
| Hindi | puranas, ayurveda, vedic_astrology, lal_kitab, festivals_vrat, vastu, panchang, dharma_karma |
| English | darshana, bauddha, yoga, muhurta_prashna, special_jyotish, smriti_dharma |
| Tamil | agama_mixed (80+ manuscripts) |
| Pali | SuttaCentral (full Tipitaka) |
| Prakrit | jaina (Acharanga, Uttaradhyayana) |

## VM Folder Structure
```
~/books/data/raw/
├── vedas/                 # 18 items (72 PDFs, ~1.8 GB)
├── puranas/               # 26+ items (197 PDFs, ~4.5 GB)
├── smriti_dharma/         # 12 items (112 PDFs, ~1.2 GB)
├── itihasa/               # 9 items (30 PDFs, ~0.5 GB)
├── yoga/                  # 11 items (73 PDFs, ~0.4 GB)
├── ayurveda/              # 15 items (161 PDFs, ~2.1 GB)
├── tantra/                # 17 items (114 PDFs, ~1.5 GB)
├── bauddha/               # 17 items (37 PDFs, ~0.3 GB)
├── jaina/                 # 12 items (110 PDFs, ~0.9 GB)
├── darshana/              # 12 items (70 PDFs, ~0.9 GB)
├── grammar_literature/    # 10 items (141 PDFs, ~1.1 GB)
├── jyotisha_astronomy/    # 11 items (11 PDFs, 2.2 GB)
├── vedic_astrology/       # 16 items (16 PDFs, 384 MB)
├── sanskrit/              # 7 items (7 PDFs, 267 MB)
├── mixed/                 # 88 items (88 PDFs, 2.8 GB)
├── samudrika/             # 5 items (5 PDFs)
├── nakshatra_jyotish/     # 5 items (5 PDFs)
├── muhurta_prashna/       # 7 items (7 PDFs)
├── vastu/                 # 5 items (6 PDFs)
├── panchang/              # 4 items (10 PDFs)
├── festivals_vrat/        # 5 items (5 PDFs)
├── mantra_stotra/         # 5 items (4 PDFs)
├── dharma_karma/          # 4 items (3 PDFs)
├── gotra/                 # 3 items (3 PDFs)
├── special_jyotish/       # 4 items (9 PDFs)
├── lal_kitab/             # 3 items (3 PDFs)
├── sarit/SARIT-corpus/    # 85 TEI-XML (204 MB)
└── suttacentral/bilara-data/  # 42K+ JSON (306 MB)
```

## Download Status (All Complete)
| Category | Downloaded | Total | Status |
|----------|-----------|-------|--------|
| Vedas & Upanishads | 18 | 18 | COMPLETE |
| Smritis & Dharma | 12 | 12 | COMPLETE |
| Puranas | 20 | 20 | COMPLETE |
| Itihasa & Epics | 9 | 9 | COMPLETE |
| Yoga | 11 | 11 | COMPLETE |
| Ayurveda | 15 | 15 | COMPLETE |
| Tantra | 17 | 17 | COMPLETE |
| Buddhist | 17 | 17 | COMPLETE |
| Jain | 12 | 12 | COMPLETE |
| Darshana | 12 | 12 | COMPLETE |
| Jyotisha | 9 | 9 | COMPLETE |
| Grammar | 10 | 10 | COMPLETE |
| **Phase 3 Subtotal** | **162** | **162** | **ALL COMPLETE** |
| Samudrika | 5 | 5 | COMPLETE |
| Nakshatra/Rashi | 5 | 5 | COMPLETE |
| Muhurta/Prashna | 7 | 7 | COMPLETE |
| Vastu | 5 | 5 | COMPLETE |
| Panchang | 4 | 4 | COMPLETE |
| Festivals/Vrat | 5 | 5 | COMPLETE |
| Mantra/Stotra | 5 | 5 | COMPLETE |
| Dharma/Karma | 4 | 4 | COMPLETE |
| Gotra/Pravara | 3 | 3 | COMPLETE |
| Special Jyotish | 4 | 4 | COMPLETE |
| Lal Kitab | 3 | 3 | COMPLETE |
| **Phase 3.5 Subtotal** | **50** | **50** | **ALL COMPLETE** |
| **GRAND TOTAL** | **212** | **212** | **ALL COMPLETE** |

> **+ Bonus**: SARIT (85 XML), SuttaCentral (42K+ JSON), HF datasets (Gita, VedAstro, Ramayana)
> **Total on disk: ~1,311 PDFs + 42K+ JSON + 85 XML = ~43,400 files, ~36.5 GB**

---

## Category Summary
| Category | Items | PDFs | Size | Languages | Content |
|---|---|---|---|---|---|
| agama_mixed | 88 | 88 | 2.8 GB | Sanskrit/Tamil/Mixed | Agama texts, Puja Vidhi, Stotras, Dictionaries |
| jyotisha_astronomy | 11 | 11 | 2.2 GB | Sanskrit/English | Classical Jyotish, Siddhanta, Hora Shastra |
| sanskrit | 7 | 7 | 267 MB | Sanskrit/Devanagari | Agama Academy texts, Kamikagama, Tripitaka |
| vedic_astrology | 16 | 16 | 384 MB | Sanskrit/Hindi/English | BPHS, Jaimini, Lal Kitab, Phaladeepika |
| vedas | 18 | 72 | ~1.8 GB | Sanskrit/English | Rigveda, Atharvaveda, 4-Veda set, Upanishads, Brahmanas |
| puranas | 20+ | 197 | ~4.5 GB | Sanskrit/Hindi/English | All 18 Mahapuranas + Harivamsa, Devi Bhagavata, Kalika |
| smriti_dharma | 12 | 112 | ~1.2 GB | Sanskrit/English | Manu, Yajnavalkya, Arthashastra, Dharmasutras, Nitisara |
| itihasa | 9 | 30 | ~0.5 GB | Sanskrit/English | Mahabharata, Yoga Vasistha, Gitas, Adhyatma Ramayana |
| yoga | 11 | 73 | ~0.4 GB | Sanskrit/English | Yoga Sutras, HYP, Gheranda, Khecarividya, Yoga Upanishads |
| ayurveda | 15 | 161 | ~2.1 GB | Sanskrit/Hindi/English | Charaka, Sushruta, Ashtanga, Kashyapa, Rasaratnakara |
| tantra | 17 | 114 | ~1.5 GB | Sanskrit/Hindi/English | Tantraloka, VBT, Kularnava, Kashmir Shaivism, Rudrayamala |
| bauddha | 10+ | 37 | ~0.3 GB | Pali/Sanskrit/English | Dhammapada, Jataka, Buddhacharita, Prajnaparamita, Nagarjuna |
| jaina | 12 | 110 | ~0.9 GB | Prakrit/Sanskrit/English | Acharanga, Tattvartha, Kundakunda texts, Gommathasara |
| darshana | 12 | 70 | ~0.9 GB | Sanskrit/English | Brahma Sutra (3 Bhashyas), Nyaya, Samkhya, Mimamsa |
| grammar_literature | 10 | 141 | ~1.1 GB | Sanskrit/English | Ashtadhyayi, Mahabhashya, Kalidasa, Panchatantra |
| **Phase 3 Subtotal** | **~268** | **~1251** | **~29 GB** | | **162 wish list items + pre-existing** |
| | | | | | |
| samudrika | 5 | 5 | - | Hindi/Sanskrit | Palmistry, Hast Samudrika Shastra |
| nakshatra_jyotish | 5 | 5 | - | Hindi/Sanskrit | Nakshatra, Rashi, Jyotish Phala |
| muhurta_prashna | 7 | 7 | - | English/Sanskrit | Prashna Marga, Muhurta, BV Raman |
| vastu | 5 | 6 | - | Hindi/Sanskrit/English | Vastu Shastra, Vishwakarma Prakash |
| panchang | 4 | 10 | - | Hindi/English | Panchangam, 100-year calendars |
| festivals_vrat | 5 | 5 | - | Hindi | Vrat Katha, Tyohar, Gita Press |
| mantra_stotra | 5 | 4 | - | Sanskrit/Hindi | Sahasranama, Mantra Shastra |
| dharma_karma | 4 | 3 | - | Hindi/English/Sanskrit | Dharma-Karma-Rahasya, Purusharthas |
| gotra | 3 | 3 | - | Sanskrit/English | Gotra Pravara, Lineage systems |
| special_jyotish | 4 | 9 | - | Hindi/English | Brihat Samhita, Shakuna, Swapna |
| lal_kitab | 3 | 3 | - | Hindi | Lal Kitab editions + remedies |
| **Phase 3.5 Subtotal** | **50** | **60** | **~7 GB** | | **50 new wish list items** |
| **GRAND TOTAL** | **~318** | **~1311** | **~36 GB** | | **212 wish list items + pre-existing** |

> **Note**: Archive.org items often contain multiple PDFs per item (e.g., Bhavishya Purana = 20 chapter PDFs).
> That's why 162 wish list items = 1251 actual PDF files on disk.

### Non-PDF Data Sources (not counted above)
| Source | Files | Size | Format |
|---|---|---|---|
| SARIT Sanskrit Corpus | 85 | 204 MB | TEI-XML |
| SuttaCentral Pali Canon | 42,000+ | 306 MB | JSON |


## Ingestion Progress (Phase 4 — PDF → JSON)
**Pipeline**: Tesseract OCR (Indic scanned) + PaddleOCR (English) + pypdf (text) → 400-token chunks → JSON
**OCR Router**: auto-detect script → Tesseract (hin+san+eng) for Devanagari, PaddleOCR for English, quality fallback
**VM**: g2-standard-32, L4 24GB GPU | Max 6 parallel OCR processes (11 = OOM crash)
**Status as of 2026-03-13**: 355+ JSONs complete. BATCH A done, BATCH B done, BATCH C mostly done. Final 6 processes running.

| Batch | Categories | Status | Notes |
|---|---|---|---|
| Pre-existing | mixed, sanskrit, vedic_astrology, jyotisha_astronomy, itihasa | **DONE** | 227 JSONs (processed 2026-03-09) |
| BATCH A | vedas, puranas, jaina, tantra, ayurveda, darshana | **DONE** | 48 JSONs from 6 categories |
| BATCH B | grammar_literature, smriti_dharma, bauddha, panchang, special_jyotish | **DONE** | special_jyotish 9 books, grammar 2, smriti 7, bauddha 1, panchang 1 |
| BATCH B+ | yoga, samudrika | **DONE** | yoga 5 books, samudrika 4 books |
| BATCH C | muhurta_prashna, vastu, festivals_vrat, mantra_stotra, dharma_karma | **DONE** | muhurta 3, vastu 5, festivals 5, mantra 4, dharma 3 |
| BATCH C+ | gotra, lal_kitab, mixed, nakshatra_jyotish, smriti_dharma, bauddha | **RUNNING** | 6 processes with page-by-page OCR (2026-03-13) |

**Post-Ingestion Plan (OCR_CORRECTION.md):**
- 3-Layer hybrid correction: Dictionary + SymSpell (free) → Google Cloud Vision re-OCR ($6 from $300 credits)
- Run AFTER all ingestion, BEFORE embedding (02_build_embeddings.py)
- Target: 88% → 97%+ overall accuracy

**OCR Upgrade (2026-03-12):**
- Replaced Surya OCR (broken API, torch conflicts) with Tesseract OCR
- `apt install tesseract-ocr tesseract-ocr-hin tesseract-ocr-san tesseract-ocr-tam tesseract-ocr-tel`
- CLI: `--ocr-engine auto|tesseract|paddle`
- Tesseract Devanagari quality: 90-95% for printed text, far better than PaddleOCR for Hindi/Sanskrit
- Auto script detection: Tesseract on first page → classify Devanagari/Tamil/Telugu/Latin → route

**Known Issues:**
- darshana book 1 (960 pages): text PDF misclassified as scanned → 0 extracted. Needs `--force-ocr` or text-mode fix
- mixed category: 0/88 processed initially (category mapping issue) — re-running in BATCH C+
- 5+ parallel processes on large scanned PDFs → OOM/hang. Max safe = 3 for big PDFs, 5 for small
- pdf2image hang fix: page-by-page conversion (`first_page=N, last_page=N`) instead of loading all pages at once
- Old manuscripts/handwritten: all OCR engines struggle (60-80%)
- VM scripts path: `~/books/scripts/` (must `cd ~/books` before running)

## Remaining Duplicates Already Fixed (6 found by pipeline)
These exist in both `mixed/` and `sanskrit/` with different filenames:
| In mixed/ | In sanskrit/ | Action |
|---|---|---|
| Purva KaranaAgama pgs001-050.pdf | AgamaAcademy_PurvaKaranaAgama_Part01.pdf | Delete from sanskrit/ (mixed has original) |
| Purva KaranaAgama pgs051-100.pdf | AgamaAcademy_PurvaKaranaAgama_Part02.pdf | Delete from sanskrit/ |
| Purva KaranaAgama pgs101-150.pdf | AgamaAcademy_PurvaKaranaAgama_Part03.pdf | Delete from sanskrit/ |
| Purva KaranaAgama pgs151-200.pdf | AgamaAcademy_PurvaKaranaAgama_Part04.pdf | Delete from sanskrit/ |
| Purva KaranaAgama pgs201-250.pdf | AgamaAcademy_PurvaKaranaAgama_Part05.pdf | Delete from sanskrit/ |
| Agama_Tripitaka_Vol2.pdf | InternetArchive_Agama_Tripitaka_Vol02.pdf | Delete InternetArchive copy |

## Suspicious Files
| File | Size | Issue |
|---|---|---|
| Hora_Sara_Prithuyasas_R_Santhanam.pdf | 33K | Too small for a book - likely corrupted |
| Muhurta_Chintamani_Full.pdf | 1.36 GB | Extremely large - likely scanned at very high DPI |
| Brihat_Parashara_Hora_Shastra_Vol1.pdf | 643 MB | Very large scanned PDF |


## Large Files (>100 MB) - Will take longer to process
| # | File | Size | Category |
|---|---|---|---|
| 1 | Muhurta_Chintamani_Full.pdf | 1358.5M | jyotisha_astronomy |
| 2 | Brihat_Parashara_Hora_Shastra_Vol1.pdf | 642.9M | jyotisha_astronomy |
| 3 | Periya Puranam-Thirutthondar Puranam.pdf | 231.3M | agama_mixed |
| 4 | Jataka_Parijata_Vol1_Gopesh_Kumar_Ojha.pdf | 224.1M | vedic_astrology |
| 5 | Siddhanta Saravali.pdf | 191.4M | agama_mixed |
| 6 | Sivaagra Paribhasha.pdf | 177.0M | agama_mixed |
| 7 | Vadamozhi Ilakiya Varalaaru.pdf | 167.1M | agama_mixed |
| 8 | Kriya Kaanda Kramavali - Somasambhu paddhati.pdf | 154.8M | agama_mixed |
| 9 | Suprabedhagama.pdf | 100.5M | agama_mixed |
| 10 | Ratna Trayam.pdf | 99.2M | agama_mixed |
| 11 | Shiva Bhakta Mahatmiya.pdf | 98.9M | agama_mixed |
| 12 | nigam-agama-chandrika-part-2.pdf | 95.1M | sanskrit |
| 13 | Sivaagra Yogins Siva Neri prakasam.pdf | 94.9M | agama_mixed |


## Full Book List

### agama_mixed/ (88 books, 2.8 GB)
| # | Filename | Size | Script (Expected) |
|---|---|---|---|
| 1 | Agama Kalai Chithira Padangal | 23.9M | Tamil |
| 2 | Agni Kaaryam | 28.2M | Tamil/Sanskrit |
| 3 | Amara Kosha - The Sanskrit Thesaurus | 44.0M | Sanskrit/English |
| 4 | Bodaayana Paarvana Srarddham | 0.3M | Sanskrit |
| 5 | Dhyanasloka T0189 | 3.2M | Sanskrit |
| 6 | Ganesha Gaayatri | 0.1M | Sanskrit |
| 7 | Guide-to-275-Siva-Sthalams | 3.3M | English |
| 8 | Kamika-Agama-English-Intro | 3.9M | English |
| 9 | Kamika-Agamam-Part-I | 3.9M | Sanskrit/Tamil |
| 10 | Kamika-Agamam-Part-II | 6.5M | Sanskrit/Tamil |
| 11 | Kamika-Agamam-Part-III | 2.7M | Sanskrit/Tamil |
| 12 | Kamika-Agamam-Part-IV | 3.1M | Sanskrit/Tamil |
| 13 | Kamika-Agamam-Part-V | 6.1M | Sanskrit/Tamil |
| 14 | KiranAgama Mahatantra | 47.7M | Sanskrit |
| 15 | KiranAgama Part-I Translation (Dr Sabharathnam) | 6.1M | English |
| 16 | KiranAgama Part-II Translation (Dr Sabharathnam) | 6.0M | English |
| 17 | KiranAgama Part-III Translation (Dr Sabharathnam) | 4.9M | English |
| 18 | KiranAgama Part-IV Translation (Dr Sabharathnam) | 6.9M | English |
| 19 | Kiranagamam - Vidyapaadham (Devanagari) | 0.2M | Devanagari |
| 20 | Kriya Kaanda Kramavali - Somasambhu paddhati | 154.8M | Sanskrit |
| 21 | Kumbabisheka kriyakrama vilakkam | 65.2M | Tamil |
| 22 | Lalitaa Sahasra Namavali | 0.1M | Sanskrit |
| 23 | Linga Ashtotharam | 0.5M | Sanskrit |
| 24 | Maasa Parva Puja-Prasaatha Chandrika-Sivagnana Bodha | 88.6M | Tamil/Sanskrit |
| 25 | Mahotsava Vilakkam | 3.0M | Tamil |
| 26 | Makutaagama-Kriya-Charya Paada | 0.1M | Sanskrit |
| 27 | Makutaagama | 41.5M | Sanskrit |
| 28 | Mantra Sangraham | 21.3M | Sanskrit/Tamil |
| 29 | Nataraaja Ashtotharam | 0.3M | Sanskrit |
| 30 | Parartha Nitya Puja Vidhi | 41.3M | Sanskrit/Tamil |
| 31 | Pavitrotsava Vidhi | 79.2M | Sanskrit/Tamil |
| 32 | Periya Puranam-Thirutthondar Puranam | 231.3M | Tamil |
| 33 | Poovum Neerum | 1.9M | Tamil |
| 34 | Pratishtalakshana Saarasamuchayaha | 1.4M | Sanskrit |
| 35 | Prayoga Chandrika | 34.6M | Sanskrit/Tamil |
| 36 | PurvaKaranaAgama pgs001-050 | 9.7M | Sanskrit |
| 37 | PurvaKaranaAgama pgs051-100 | 6.8M | Sanskrit |
| 38 | PurvaKaranaAgama pgs101-150 | 6.1M | Sanskrit |
| 39 | PurvaKaranaAgama pgs151-200 | 6.8M | Sanskrit |
| 40 | PurvaKaranaAgama pgs201-250 | 8.3M | Sanskrit |
| 41 | PurvaKaranaAgama pgs251-300 | 6.8M | Sanskrit |
| 42 | PurvaKaranaAgama pgs301-350 | 7.8M | Sanskrit |
| 43 | PurvaKaranaAgama pgs351-362 | 2.0M | Sanskrit |
| 44 | Ratna Trayam | 99.2M | Sanskrit/Tamil |
| 45 | Rudraaksha Vilakkam | 27.9M | Tamil |
| 46 | Saanti Vilaasa | 8.7M | Sanskrit |
| 47 | Sahasra Agama - Kriya Pada - Ashtabandana Vidhi | 1.5M | Sanskrit |
| 48 | Saiva Anushtana Vidhi | 13.0M | Sanskrit/Tamil |
| 49 | Saiva Ashaucha Dipika | 59.7M | Sanskrit/Tamil |
| 50 | Saiva Samaya Paddhaati - Vathoolagamam | 51.0M | Sanskrit/Tamil |
| 51 | Saiva Siddhanta Paribasha | 9.8M | Sanskrit/Tamil |
| 52 | Saivabhushana Chandrika | 77.3M | Sanskrit/Tamil |
| 53 | Samskruta Siksha | 4.3M | Sanskrit |
| 54 | Sandyavandanam - year 1898 | 3.1M | Sanskrit |
| 55 | Sanskrit-English Dictionary | 0.9M | Sanskrit/English |
| 56 | Sanskrit-Tamil Dictionary | 71.1M | Sanskrit/Tamil |
| 57 | Sarva Gnanottaram Vidyapadam | 67.1M | Sanskrit/Tamil |
| 58 | Seemantham | 0.1M | Sanskrit/Tamil |
| 59 | Shanmukha Pratimukha Ashrottara Sata Namavali | 0.1M | Sanskrit |
| 60 | Shiva Bhakta Mahatmiya | 98.9M | Tamil |
| 61 | Shiva Sahasra Namavali from Mahabaratha | 0.1M | Sanskrit |
| 62 | Shiva Sahasra Namavali from itihasa | 0.2M | Sanskrit |
| 63 | Shiva puja Vidhi | 31.0M | Sanskrit/Tamil |
| 64 | Shivaarchana Chandrika | 28.8M | Sanskrit/Tamil |
| 65 | Shivalinga Vazhipaadu | 60.9M | Tamil |
| 66 | Shivapujastava | 24.4M | Sanskrit |
| 67 | Siddantha Prakashika (Sarvaathmasambhu) | 9.8M | Sanskrit |
| 68 | Siddhanta Saravali | 191.4M | Sanskrit/Tamil |
| 69 | Siva Mahothsava Sangraha | 9.3M | Sanskrit/Tamil |
| 70 | Sivaagra Paribhasha | 177.0M | Sanskrit/Tamil |
| 71 | Sivaagra Yogins Siva Neri prakasam | 94.9M | Tamil |
| 72 | Sivaalaya Utsavam Pavitrotsavam | 15.6M | Tamil |
| 73 | SkandaPuranam-SankaraSamhitaPart1 | 86.6M | Sanskrit |
| 74 | SkandaPuranam-SankaraSamhitaPart2 | 80.9M | Sanskrit |
| 75 | Suprabedhagama | 100.5M | Sanskrit |
| 76 | Thiruvempaavai and Thiruppalliyezhuchi | 0.6M | Tamil |
| 77 | Upanayanam | 0.1M | Sanskrit/Tamil |
| 78 | Uttara KaranaAgama pgs001-050 | 6.5M | Sanskrit |
| 79 | Uttara KaranaAgama pgs051-100 | 5.6M | Sanskrit |
| 80 | Uttara KaranaAgama pgs101-150 | 5.7M | Sanskrit |
| 81 | Uttara KaranaAgama pgs151-200 | 6.6M | Sanskrit |
| 82 | Uttara KaranaAgama pgs201-223 | 4.1M | Sanskrit |
| 83 | Vadamozhi Ilakiya Varalaaru | 167.1M | Tamil |
| 84 | Veeraagamam | 7.1M | Sanskrit |
| 85 | Vinaayaka108 | 0.1M | Sanskrit |
| 86 | Vivaha Virutham | 0.1M | Tamil |
| 87 | Yajur Veda Upakarma - Sanskrit | 11.8M | Sanskrit |
| 88 | kriyadipika | 56.8M | Sanskrit/Tamil |

### jyotisha_astronomy/ (11 books, 2.2 GB)
| # | Filename | Size | Script |
|---|---|---|---|
| 1 | Aryabhatiya_English | 11.2M | English |
| 2 | Brahmasphutasiddhanta_Vol1 | 23.4M | Sanskrit/English |
| 3 | Brihat_Jataka_English | 27.6M | English |
| 4 | Brihat_Parashara_Hora_Shastra_Vol1 | 642.9M | Sanskrit (scanned) |
| 5 | Kalaprakasika_English | 27.6M | English |
| 6 | Muhurta_Chintamani_Full | 1358.5M | Sanskrit (scanned, huge) |
| 7 | Phaladeepika_Full | 23.7M | Sanskrit/English |
| 8 | Saravali_Full | 16.5M | Sanskrit/English |
| 9 | Siddhanta_Darpana | 14.7M | Sanskrit |
| 10 | Surya_Siddhanta_English | 16.9M | English |
| 11 | Vedanga_Jyotisha_English | 7.3M | English |

### sanskrit/ (7 books, 267 MB)
| # | Filename | Size | Script |
|---|---|---|---|
| 1 | AgamaAcademy_Chintyagama | 0.9M | Sanskrit |
| 2 | AgamaAcademy_Kamikagama_Devanagari | 3.1M | Devanagari |
| 3 | AgamaAcademy_Yogaja_Agamam | 0.8M | Sanskrit |
| 4 | Agama_Tripitaka_Vol2 | 58.2M | Sanskrit |
| 5 | KamikaAgama_PurvaPada_Part1 | 4.7M | Sanskrit |
| 6 | nigam-agama-chandrika-part-2 | 95.1M | Sanskrit |
| 7 | sivagnana-bodha | 8.3M | Sanskrit |

### vedic_astrology/ (16 books, 384 MB)
| # | Filename | Size | Script |
|---|---|---|---|
| 1 | BPHS_Girish_Chand_Sharma_Vol1 | 19.2M | Hindi |
| 2 | BPHS_Girish_Chand_Sharma_Vol2 | 20.7M | Hindi |
| 3 | Bhrigu_Samhita_T_M_Rao | 5.5M | English |
| 4 | Brihat_Jataka_Varahamihira (N Chidambaram Iyer) | 7.7M | English |
| 5 | Gochar_Phaladeepika (U S Pulippani) | 7.5M | Hindi |
| 6 | Hora_Sara_Prithuyasas_R_Santhanam | 0.03M | CORRUPTED? |
| 7 | Jaimini_Sutras (B Suryanarain Rao) | 13.3M | English |
| 8 | Jataka_Parijata_Vol1 (Gopesh Kumar Ojha) | 224.1M | Hindi (scanned) |
| 9 | Lal_Kitab_1952_Grammar_Portion | 27.2M | Hindi/Urdu |
| 10 | Narada_Purana_Part1_Motilal | 19.9M | English |
| 11 | Narada_Purana_Part2_Motilal | 28.9M | English |
| 12 | Panchang_and_Horary_Astrology (R Santhanam) | 3.5M | English |
| 13 | Phaladeepika (GS Kapoor) | 1.0M | English |
| 14 | Uttara_Kalamrita (V Subrahmanya Sastri) | 1.8M | English |
| 15 | Vedic_Remedies_Sanjay_Rath_Integrated_Approach | 3.0M | English |
| 16 | Yogas_in_Astrology (K S Charak) | 0.6M | English |


### vedas/ (18 items, ~1.8 GB, 72 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Rigveda (full Sakala Shakha) | Sanskrit | Archive.org `rigvedacomplete` |
| 2 | Samaveda | Sanskrit | 4-Veda set |
| 3 | Yajurveda - Krishna (Taittiriya Samhita) | Sanskrit | 4-Veda set |
| 4 | Yajurveda - Shukla (Vajasaneyi Samhita) | Sanskrit | 4-Veda set |
| 5 | Atharvaveda (2 volumes) | Sanskrit | Archive.org |
| 6 | 108 Upanishads | Sanskrit | Archive.org |
| 7 | Brihadaranyaka Upanishad + Shankara Bhashya | Sanskrit/English | 11-Upanishads set |
| 8 | Chandogya Upanishad | Sanskrit/English | 11-Upanishads set |
| 9 | Katha Upanishad | Sanskrit/English | Ishadi Nau Upanishad |
| 10 | Mundaka Upanishad | Sanskrit/English | Ishadi Nau |
| 11 | Mandukya Upanishad + Gaudapada Karika | Sanskrit/English | Ishadi Nau |
| 12 | Taittiriya Upanishad | Sanskrit/English | Ishadi Nau |
| 13 | Shvetashvatara Upanishad | Sanskrit/English | Ishadi Nau |
| 14 | Isha Upanishad | Sanskrit/English | Ishadi Nau |
| 15 | Shatapatha Brahmana | Sanskrit/English | Archive.org batch 2 |
| 16 | Taittiriya Brahmana | Sanskrit | Archive.org batch 3 |
| 17 | Aitareya Brahmana | Sanskrit/English | Archive.org batch 2 |
| 18 | Rigveda - Max Muller English | English | Archive.org batch 3 |

### puranas/ (20+ items, ~4.5 GB, 197 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Bhagavata Purana (12 Skandhas) | Sanskrit/English | Archive.org |
| 2 | Vishnu Purana | Hindi | Gita Press, 507MB |
| 3 | Shiva Purana | Sanskrit | 59MB |
| 4 | Garuda Purana | Sanskrit/Hindi | 2 editions |
| 5 | Agni Purana | Sanskrit/Hindi | 2 editions |
| 6 | Markandeya Purana | Hindi | Gita Press |
| 7 | Matsya Purana | Sanskrit/Hindi | 2 versions |
| 8 | Kurma Purana | Sanskrit | 566MB |
| 9 | Linga Purana | Sanskrit/English | batch 2 |
| 10 | Brahmanda Purana | Sanskrit | batch 2 |
| 11 | Brahma Purana | Sanskrit/English | batch 2 |
| 12 | Brahma Vaivarta Purana | Sanskrit | batch 2 |
| 13 | Padma Purana (Part 2+3) | Sanskrit | batch 2 |
| 14 | Varaha Purana | Sanskrit | batch 2 |
| 15 | Vamana Purana | Sanskrit/English | batch 2 |
| 16 | Narada Purana (Part 1+2, 2.3GB) | Sanskrit | Archive.org |
| 17 | Bhavishya Purana | Sanskrit | batch 2 |
| 18 | 18 Maha Puranas Summary | English | 3.3MB |
| 19 | Harivamsa Purana | Sanskrit/English | batch 4, 564MB |
| 20 | Devi Bhagavata Purana | Sanskrit/English | batch 2 |
| 21 | Kalika Purana | Sanskrit | batch 4, 126MB |

### smriti_dharma/ (12 items, ~1.2 GB, 112 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Manu Smriti (3 vols, Medhatithi Bhashya) | Sanskrit/Hindi | pre-existing |
| 2 | Yajnavalkya Smriti (Mitakshara) | Sanskrit/Hindi | pre-existing |
| 3 | Parasara Smriti | Sanskrit/English | batch 2 |
| 4 | Narada Smriti | Sanskrit/English | batch 2 |
| 5 | Vishnu Smriti | Sanskrit/English | batch 3 |
| 6 | Apastamba/Gautama/Baudhayana Dharmasutra (4-in-1) | Sanskrit/English | batch 2 |
| 7 | Arthashastra (Kautilya) | Sanskrit/English | Shamasastry English |
| 8 | Nitisara (Kamandaka) | Sanskrit/English | batch 3 |
| 9 | Dharmasindhu | Sanskrit | batch 3 |
| 10 | Nirnayasindhu | Sanskrit | batch 4, 147MB |

### itihasa/ (9 items, ~0.5 GB, 30 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Mahabharata - Ganguli (full) | English | 23MB |
| 2 | Ramayana - Valmiki | Sanskrit/English | HF itihasa dataset |
| 3 | Adhyatma Ramayana | Sanskrit/English | batch 3 |
| 4 | Yoga Vasistha (Maha, 2 vols) | Sanskrit/English | batch 3 |
| 5 | Bhagavad Gita (base text) | Sanskrit/English | HF dataset |
| 6 | Bhagavad Gita - Tilak (Gita Rahasya) | English | batch 3 |
| 7 | Bhagavad Gita - Shankaracharya Bhashya | Sanskrit | batch 4, 30MB |
| 8 | Ashtavakra Gita | Sanskrit/English | batch 2 |
| 9 | Avadhuta Gita (Dattatreya) | Sanskrit/English | batch 3 |

### yoga/ (11 items, ~0.4 GB, 73 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Yoga Sutras of Patanjali (Bhoja commentary) | Sanskrit/English | 21MB |
| 2 | Hatha Yoga Pradipika | Sanskrit/English | 1.6MB |
| 3 | Gheranda Samhita | Sanskrit/English | 2.2MB |
| 4 | Shiva Samhita | Hindi | pre-existing |
| 5 | Yoga Yajnavalkya | Sanskrit/English | batch 2 |
| 6 | Goraksha Shataka / GorakshaSamhita | Sanskrit | batch 4, 145MB |
| 7 | Vasistha Samhita - Yoga Kanda | Sanskrit/Hindi | batch 5, 86MB |
| 8 | 20 Yoga Upanishads | Sanskrit/English | batch 5 |
| 9 | Hatharatnavali | Sanskrit | batch 4, 78MB |
| 10 | Shiva Svarodaya | Sanskrit/English | batch 3, 52MB |
| 11 | Khecarividya (Mallinson critical ed.) | Sanskrit/English | batch 5 |

### ayurveda/ (15 items, ~2.1 GB, 161 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Charaka Samhita (2 vols) | Hindi | 221MB |
| 2 | Sushruta Samhita (3 vols) | English | 96MB |
| 3 | Ashtanga Hridayam | Sanskrit/English | batch 2 |
| 4 | Ashtanga Sangraha | Sanskrit | batch 3 |
| 5 | Madhava Nidanam | Sanskrit/English | batch 2 |
| 6 | Bhavaprakasha | Sanskrit/English | batch 2 |
| 7 | Sharangadhara Samhita | Sanskrit/English | batch 2 |
| 8 | Dhanvantari Nighantu | Sanskrit | batch 3 |
| 9 | Rasaratna Samuccaya | Sanskrit/English | batch 4, 146MB |
| 10 | Rasaratnakara | Sanskrit | batch 5 |
| 11 | Yoga Ratnakara | Sanskrit | batch 3 |
| 12 | Chakradatta | Sanskrit/English | batch 3 |
| 13 | Kashyapa Samhita | Sanskrit/English | batch 3 |
| 14 | Harita Samhita | Sanskrit | batch 4, 391MB |
| 15 | Raj Nighantu | Sanskrit/English | batch 4, 394MB |

### tantra/ (17 items, ~1.5 GB, 114 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Mahanirvana Tantra (Woodroffe + another) | Sanskrit/English | pre-existing |
| 2 | Kularnava Tantra (2 versions) | Sanskrit/English | pre-existing |
| 3 | Tantraloka - Abhinavagupta (Vol I+II) | Sanskrit/Hindi | 340MB |
| 4 | Vijnana Bhairava Tantra | Sanskrit | 640KB |
| 5 | Netra Tantra (KSTS) | Sanskrit | batch 4, 170MB |
| 6 | Svacchanda Tantra (KSTS Vol 1+2) | Sanskrit | batch 4, 398MB |
| 7 | Shiva Sutras (Vasugupta) | Sanskrit/English | batch 2 |
| 8 | Pratyabhijnahridayam (Kshemaraja) | Sanskrit/English | batch 2 |
| 9 | Spanda Karikas | Sanskrit/English | batch 2 |
| 10 | Yogini Hridaya | Sanskrit/English | batch 3 |
| 11 | Tripura Rahasya (2 parts) | Sanskrit/English | batch 2 |
| 12 | Rudrayamala Tantra (Main + Uttara, 2 ed.) | Sanskrit | batch 5 |
| 13 | Mantra Mahodadhi | Sanskrit/English | batch 3 |
| 14 | Nityashodashikarnava | Sanskrit | batch 4, 203MB |
| 15 | Tantra Sara - Abhinavagupta | Sanskrit | batch 4, 33MB |
| 16 | Devi Mahatmya / Chandi Path | Sanskrit/English | batch 3 |
| 17 | Soundarya Lahari | Sanskrit/English | batch 2 |

### bauddha/ (17 items, ~0.3 GB, 37 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Dhammapada | Pali/English | Archive.org + SuttaCentral |
| 2 | Digha Nikaya | Pali/English | SuttaCentral JSON |
| 3 | Majjhima Nikaya | Pali/English | SuttaCentral JSON |
| 4 | Samyutta Nikaya | Pali/English | SuttaCentral JSON |
| 5 | Anguttara Nikaya | Pali/English | SuttaCentral JSON |
| 6 | Sutta Nipata | Pali/English | SuttaCentral JSON |
| 7 | Jataka Tales (6 vols) | English | batch 3 |
| 8 | Buddhacharita (Ashvaghosa) | Sanskrit/English | batch 2, 37MB |
| 9 | Ashtasahasrika Prajnaparamita | Sanskrit/English | batch 4, 18MB |
| 10 | Milindapanha | Pali/English | batch 3 |
| 11 | Mulamadhyamakakarika (Nagarjuna) | Sanskrit/English | batch 4 |
| 12 | Abhidharmakosha (Vasubandhu) | English | batch 5 |
| 13 | Lalitavistara Sutra | Sanskrit/English | batch 3 |
| 14 | Lankavatara Sutra | Sanskrit/English | batch 3 |
| 15 | Vajracchedika (Diamond Sutra) | Sanskrit/English | batch 3 |
| 16 | Heart Sutra | Sanskrit/English | batch 3 |
| 17 | Vimalakirti Nirdesa Sutra | English | batch 3 |

### jaina/ (12 items, ~0.9 GB, 110 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Acharanga Sutra (3 versions) | Prakrit/English | pre-existing |
| 2 | Tattvartha Sutra (Umasvati, with commentary) | Sanskrit/English | pre-existing |
| 3 | Kalpa Sutra | Prakrit/English | batch 2 |
| 4 | Uttaradhyayana Sutra | Prakrit/English | batch 2 |
| 5 | Yoga Shastra (Hemachandra) | Sanskrit/Hindi | batch 5 |
| 6 | Niyamasara (Kundakunda) | Prakrit/English | batch 3 |
| 7 | Samayasara (Kundakunda) | Prakrit/English | batch 2 |
| 8 | Pravachanasara (Kundakunda) | Prakrit/English | batch 3 |
| 9 | Panchastikayasara | Prakrit/English | batch 3 |
| 10 | Gommathasara (Nemichandra) | Sanskrit/English | batch 4, 58MB |
| 11 | Dasavaikalika Sutra | Prakrit/English | batch 3 |
| 12 | Adipurana (Jinasena) | Sanskrit | batch 4, 102MB |

### darshana/ (12 items, ~0.9 GB, 70 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Brahma Sutras + Shankara Bhashya | Sanskrit/English | 148MB |
| 2 | Brahma Sutras - Ramanuja (Sri Bhashya) | Sanskrit/English | batch 4, 430MB |
| 3 | Brahma Sutras - Madhva Bhashya | Sanskrit | batch 5, 130MB |
| 4 | Vivekachudamani (Chinmayananda) | English | 58MB |
| 5 | Panchadashi (Vidyaranya) | Sanskrit/English | batch 3 |
| 6 | Nyaya Sutras (Gautama) | Sanskrit/English | batch 4, 11MB |
| 7 | Vaisheshika Sutras (Kanada) | Sanskrit/English | batch 5 |
| 8 | Samkhya Karika | Sanskrit/English | batch 2 |
| 9 | Mimamsa Sutras (Jaimini) | Sanskrit/English | batch 3 |
| 10 | Tarka Sangraha | Sanskrit/English | batch 2 |
| 11 | Upadesa Sahasri (Shankara) | Sanskrit/English | batch 4, 88MB |
| 12 | Drig Drishya Viveka | Sanskrit/English | batch 3 |

### grammar_literature/ (10 items, ~1.1 GB, 141 PDFs)
| # | Title | Language | Source |
|---|---|---|---|
| 1 | Ashtadhyayi (Panini) | Sanskrit | 29MB |
| 2 | Mahabhashya (Patanjali) | Sanskrit | batch 3 |
| 3 | Vakyapadiya (Bhartrhari) | Sanskrit/English | batch 4, 172MB |
| 4 | Siddhanta Kaumudi | Sanskrit | batch 3 |
| 5 | Meghaduta (Kalidasa) | Sanskrit/English | batch 2 |
| 6 | Abhijnana Shakuntalam (Kalidasa) | Sanskrit/English | batch 3 |
| 7 | Raghuvamsha (Kalidasa) | Sanskrit/English | batch 3 |
| 8 | Natyashastra (Bharata Muni) | Sanskrit/English | batch 2 |
| 9 | Panchatantra | Sanskrit/English | batch 3 |
| 10 | Kathasaritsagara (Somadeva) | Sanskrit/English | batch 3 |

### samudrika/ (5 items, 5 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Hast Samudrika Shastra (K.C. Sen) | Hindi |
| 2 | Samudrika Shastra (Jyotish Grantha) | Hindi |
| 3 | Samudrik Shastr Ya Bhagya Nirnay (1927) | Hindi |
| 4 | Samudrika Shastra (Ghan Shyam Das) | Hindi |
| 5 | Samudrika Shastra Manu (Sanskrit MS) | Sanskrit |

### nakshatra_jyotish/ (5 items, 5 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Nakshatra Jyotish (Ragunandan Gowd) | Hindi |
| 2 | Sampurna Rashi Nakshatra Aur Muhurt Vigyan | Hindi |
| 3 | Nakshatra Maheshwari (Nigrahacharya) | Sanskrit |
| 4 | Jyotisha Prasna Phala Ganana | Hindi |
| 5 | Bhartiya Jyotish (Nemichandra, 706pg) | Hindi |

### muhurta_prashna/ (7 items, 7 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Prashna Marga - BV Raman (2 vols) | English |
| 2 | Prashnamarga Sanskrit Commentary | Sanskrit |
| 3 | Prasna Tantra - BV Raman | English |
| 4 | Muhurtha Electional Astrology - BV Raman | English |
| 5 | Muhurta Jyotis Vedic Astrology | English |
| 6 | Chappanna / Prasna Sastra (1946) | English |
| 7 | Indian Horary Astrology - V.A.K. Ayer | English |

### vastu/ (5 items, 6 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Bhavan Bhaskar (Vastu Shastra) | Hindi |
| 2 | Vastu Sastra Vol.I Hindu Architecture (Shukla) | Sanskrit/English |
| 3 | Vishwakarma Prakash | Sanskrit |
| 4 | Sthapatya Ved Vishvakarma Vastu | Sanskrit |
| 5 | Saral Vastu Shastra | Hindi |

### panchang/ (4 items, 10 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Panchangam Calculations (Karanam) | English |
| 2 | Rashtriya Panchang Archive (multi-year) | Hindi/Sanskrit |
| 3 | 100 Year Panchang 2001-2100 Vikram Era | Hindi |
| 4 | 100 Years Indian Calendar (1926) | English |

### festivals_vrat/ (5 items, 5 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Hinduon Ke Pavitra Vrat Aur Tyohar | Hindi |
| 2 | Hinduo Ke Vrat Aur Tyohar (Gita Press) | Hindi |
| 3 | Vratotsav Chandrika (1903) | Hindi |
| 4 | All Hindu Vrat Katha | Hindi |
| 5 | Vrat Parva Aur Tyohar (Vibha Gupta) | Hindi |

### mantra_stotra/ (5 items, 4 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Sahasranama Stotra Sangrah (Gita Press) | Sanskrit/Hindi |
| 2 | Vishnu Sahasranama Shankara Bhashya + Hindi | Sanskrit/Hindi |
| 3 | Mahadev Sahasranama (1939 Naval Kishore) | Hindi |
| 4 | Lakshmi Sahasranama Stotra | Sanskrit |
| 5 | Mantra Shastra Manuscript | Hindi/Urdu |

### dharma_karma/ (4 items, 3 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Dharma-Karma-Rahasya (Bhawani Sankar) | Hindi |
| 2 | Dharma Artha Kama Moksha (Hemant Bhatt) | Hindi |
| 3 | Hindu Dharma Shastra Vol.5 (MN Datta) | English |
| 4 | Moksha Dharma - Mahabharata Shanti Parva | Sanskrit |

### gotra/ (3 items, 3 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Gotra Pravara Nibanda Kadambam (P. Chentsal Rao) | Sanskrit |
| 2 | Early Brahmanical Systems of Gotra (Brough/OUP) | English |
| 3 | Principles of Pravara & Gotra (452pg) | Sanskrit/English |

### special_jyotish/ (4 items, 9 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Brihat Samhita - Varahamihira (Hindi) | Hindi |
| 2 | Brihat Samhita (English, Subrahmanya 1946) | English |
| 3 | Shakun Shastra | Hindi |
| 4 | Swapna Vigyan (Dream Science, 1942) | Hindi |

### lal_kitab/ (3 items, 3 PDFs)
| # | Title | Language |
|---|---|---|
| 1 | Lal Kitab - BM Goswami (Full) | Hindi |
| 2 | Asli Prachin Lal Kitab | Hindi |
| 3 | Lal Kitab Upay Sahit | Hindi |

---

## Non-PDF Datasets

### SARIT Sanskrit Corpus (85 TEI-XML files, 204 MB)
Scholarly Sanskrit texts with critical apparatus. Location: `raw/sarit/SARIT-corpus/`
> Needs `xml_extractor.py` for ingestion

### SuttaCentral Pali Canon (42,000+ JSON files, 306 MB)
Full Tipitaka — Pali original + Bhikkhu Sujato English. Location: `raw/suttacentral/bilara-data/`
| Collection | Content |
|---|---|
| Digha Nikaya | Long Discourses (34 suttas) |
| Majjhima Nikaya | Middle Length (152 suttas) |
| Samyutta Nikaya | Connected (2,904 suttas) |
| Anguttara Nikaya | Numerical (8,777 suttas) |
| Khuddaka Nikaya | Minor Collection (Dhammapada, Sutta Nipata, Jataka, etc.) |
| Vinaya Pitaka | Monastic Rules |
| Abhidhamma | Higher Teachings |
> Needs `json_extractor.py` for ingestion

### HuggingFace Datasets (on VM)
| Dataset | Size | Content |
|---|---|---|
| Bhagavad Gita | ~5 MB | 18 chapters, 719 shlokas, Sanskrit+English |
| VedAstro | ~100 MB | Vedic astrology structured data |
| Ramayana (itihasa) | ~50 MB | Sanskrit + English parallel |
| Spiritual AI QA | ~5 MB | Fine-tuning data |

---

## Processing Strategy
1. **Small text PDFs first** (<10MB, text-based): Fast, pypdf extraction
2. **Medium scanned PDFs** (10-100MB): PaddleOCR with script detection
3. **Large scanned PDFs** (>100MB): Process with --max-pages for testing first
4. **Giant PDFs** (>500MB): Muhurta Chintamani (1.3GB) and BPHS (643MB) - process last, may need page limits

## Estimated Processing Time (on L4 GPU)
| Type | Count | Est. Time |
|---|---|---|
| Text PDFs (<50MB) | ~60 | ~10-15 min |
| Scanned PDFs (50-100MB) | ~15 | ~30-60 min |
| Large scanned (100-250MB) | ~8 | ~1-2 hours |
| Giant (>500MB) | 2 | ~2-4 hours |
| **TOTAL** | **122** | **~4-7 hours** |
