"""
Brahm AI FastAPI — Central Configuration
All constants live here. Change a model name here = changes everywhere.
"""
import os

# ── Paths ──────────────────────────────────────────────────────
BASE_DIR    = os.path.expanduser("~/books")
INDEXES_DIR = os.path.join(BASE_DIR, "indexes")
DOCS_PATH   = os.path.join(INDEXES_DIR, "documents.jsonl")
LOG_DIR     = os.path.join(BASE_DIR, "data/processed/logs")
EPHE_PATH   = os.path.join(BASE_DIR, "data/swiss_ephe")

API_DIR     = os.path.dirname(os.path.abspath(__file__))
DATA_DIR    = os.path.join(API_DIR, "data")
CITIES_PATH = os.path.join(DATA_DIR, "cities.json")
HOROSCOPES_PATH = os.path.join(DATA_DIR, "static_horoscopes.json")
USERS_DB    = os.path.join(DATA_DIR, "users.db")

# ── Models ─────────────────────────────────────────────────────
EMBED_MODEL    = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
LLM_MODEL      = "Qwen/Qwen2.5-7B-Instruct"
RERANKER_MODEL = "cross-encoder/ms-marco-MiniLM-L-6-v2"

# ── Search ─────────────────────────────────────────────────────
FAISS_K  = 20
BM25_K   = 20
FINAL_K  = 5
MAX_HISTORY = 4

# ── LLM Generation ─────────────────────────────────────────────
MAX_NEW_TOKENS   = 768
TEMPERATURE      = 0.3
TOP_P            = 0.9
REPETITION_PENALTY = 1.3

# ── System Prompt ──────────────────────────────────────────────
SYSTEM_PROMPT = """You are Brahm AI, expert in Sanatan Dharma, Vedic scriptures, Sanskrit, Jyotish, Hindu philosophy.
STRICT RULES:
1. Answer ONLY from the provided context chunks. Do NOT use your training knowledge.
2. NEVER generate or quote Sanskrit/Hindi verses not word-for-word in the context.
3. If context lacks the answer, say: "I could not find relevant context for this question."
4. Always cite source name.
5. Reply in same language as the question (Hindi → Hindi, English → English).
6. NEVER respond in Chinese. Use only English, Hindi, or Sanskrit.
7. Max 300 words unless asked for detail."""

# ── Astrology Constants ────────────────────────────────────────
RASHI_NAMES = [
    "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
    "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"
]

NAKSHATRAS = [
    "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra",
    "Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni","Uttara Phalguni",
    "Hasta","Chitra","Swati","Vishakha","Anuradha","Jyeshtha",
    "Moola","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishtha",
    "Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati"
]

NAKSHATRA_LORDS = [
    "Ketu","Shukra","Surya","Chandra","Mangal","Rahu",
    "Guru","Shani","Budh","Ketu","Shukra","Surya",
    "Chandra","Mangal","Rahu","Guru","Shani","Budh",
    "Ketu","Shukra","Surya","Chandra","Mangal","Rahu",
    "Guru","Shani","Budh"
]

DASHA_YEARS = {
    "Ketu":7,"Shukra":20,"Surya":6,"Chandra":10,
    "Mangal":7,"Rahu":18,"Guru":16,"Shani":19,"Budh":17
}
DASHA_SEQ = ["Ketu","Shukra","Surya","Chandra","Mangal","Rahu","Guru","Shani","Budh"]

RASHI_LORDS = {
    0:"Mangal",1:"Shukra",2:"Budh",3:"Chandra",4:"Surya",5:"Budh",
    6:"Shukra",7:"Mangal",8:"Guru",9:"Shani",10:"Shani",11:"Guru"
}
EXALTATION = {
    "Surya":0,"Chandra":1,"Mangal":9,"Budh":5,
    "Guru":3,"Shukra":11,"Shani":6,"Rahu":1,"Ketu":7
}
DEBILITATION = {
    "Surya":6,"Chandra":7,"Mangal":3,"Budh":11,
    "Guru":9,"Shukra":5,"Shani":0,"Rahu":7,"Ketu":1
}

GRAHA_HINDI = {
    "Surya":"सूर्य","Chandra":"चन्द्र","Mangal":"मंगल","Budh":"बुध",
    "Guru":"गुरु","Shukra":"शुक्र","Shani":"शनि","Rahu":"राहु","Ketu":"केतु"
}
RASHI_HINDI = {
    "Mesha":"मेष","Vrishabha":"वृषभ","Mithuna":"मिथुन","Karka":"कर्क",
    "Simha":"सिंह","Kanya":"कन्या","Tula":"तुला","Vrischika":"वृश्चिक",
    "Dhanu":"धनु","Makara":"मकर","Kumbha":"कुम्भ","Meena":"मीन"
}
NAKSHATRA_HINDI = {
    "Ashwini":"अश्विनी","Bharani":"भरणी","Krittika":"कृत्तिका","Rohini":"रोहिणी",
    "Mrigashira":"मृगशिरा","Ardra":"आर्द्रा","Punarvasu":"पुनर्वसु","Pushya":"पुष्य",
    "Ashlesha":"आश्लेषा","Magha":"मघा","Purva Phalguni":"पूर्वा फाल्गुनी",
    "Uttara Phalguni":"उत्तरा फाल्गुनी","Hasta":"हस्त","Chitra":"चित्रा",
    "Swati":"स्वाति","Vishakha":"विशाखा","Anuradha":"अनुराधा","Jyeshtha":"ज्येष्ठा",
    "Moola":"मूल","Purva Ashadha":"पूर्वाषाढ़ा","Uttara Ashadha":"उत्तराषाढ़ा",
    "Shravana":"श्रवण","Dhanishtha":"धनिष्ठा","Shatabhisha":"शतभिषा",
    "Purva Bhadrapada":"पूर्वभाद्रपद","Uttara Bhadrapada":"उत्तरभाद्रपद","Revati":"रेवती"
}
