#!/usr/bin/env python3
"""
Brahm AI - Gradio Web Interface v2 with Kundali Calculator
Tab 1: RAG Chat (Vedic scriptures, 1.1M+ chunks)
Tab 2: Kundali Calculator (Vedic birth chart, pyswisseph)

Upload to VM:
    gdown / scp / heredoc → ~/books/scripts/08_gradio_kundali.py

Run:
    cd ~/books
    source ~/ai-env/bin/activate
    # Kill any running instances first:
    pkill -f gradio_app || pkill -f 07_gradio
    python3 scripts/08_gradio_kundali.py
"""

import os, json, time, threading, sys, math, re, numpy as np, gradio as gr
from datetime import datetime

# Import panchang from same scripts/ directory
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
try:
    from panchang import Panchang
    PANCHANG_AVAILABLE = True
except Exception as _pe:
    PANCHANG_AVAILABLE = False
    print(f"WARNING: panchang.py import failed — Panchang tab disabled. {_pe}")

# ── CONFIG ────────────────────────────────────────────────────
BASE_DIR    = os.path.expanduser("~/books")
INDEXES_DIR = os.path.join(BASE_DIR, "indexes")
DOCS_PATH   = os.path.join(INDEXES_DIR, "documents.jsonl")
LOG_DIR     = os.path.join(BASE_DIR, "data/processed/logs")
EPHE_PATH   = os.path.join(BASE_DIR, "data/swiss_ephe")
EMBED_MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
LLM_MODEL   = "Qwen/Qwen2.5-7B-Instruct"
FAISS_K, BM25_K, FINAL_K = 20, 20, 5
MAX_HISTORY = 4

SYSTEM_PROMPT = """You are Brahm AI, expert in Sanatan Dharma, Vedic scriptures, Sanskrit, Jyotish, Hindu philosophy.
STRICT RULES:
1. Answer ONLY from the provided context chunks. Do NOT use your training knowledge.
2. NEVER generate or quote Sanskrit/Hindi verses not word-for-word in the context.
3. If context lacks the answer, say: "I could not find relevant context for this question."
4. Always cite source name.
5. Reply in same language as the question (Hindi → Hindi, English → English).
6. NEVER respond in Chinese. Use only English, Hindi, or Sanskrit.
7. Max 300 words unless asked for detail."""

# ── INDIAN CITIES LOOKUP ──────────────────────────────────────
CITIES = {
    "New Delhi":     (28.6139, 77.2090),
    "Mumbai":        (19.0760, 72.8777),
    "Kolkata":       (22.5726, 88.3639),
    "Chennai":       (13.0827, 80.2707),
    "Bangalore":     (12.9716, 77.5946),
    "Hyderabad":     (17.3850, 78.4867),
    "Pune":          (18.5204, 73.8567),
    "Ahmedabad":     (23.0225, 72.5714),
    "Varanasi":      (25.3176, 82.9739),
    "Prayagraj":     (25.4358, 81.8463),
    "Lucknow":       (26.8467, 80.9462),
    "Patna":         (25.5941, 85.1376),
    "Jaipur":        (26.9124, 75.7873),
    "Bhopal":        (23.2599, 77.4126),
    "Indore":        (22.7196, 75.8577),
    "Nagpur":        (21.1458, 79.0882),
    "Surat":         (21.1702, 72.8311),
    "Kanpur":        (26.4499, 80.3319),
    "Agra":          (27.1767, 78.0081),
    "Mathura":       (27.4924, 77.6737),
    "Vrindavan":     (27.5794, 77.6964),
    "Haridwar":      (29.9457, 78.1642),
    "Rishikesh":     (30.0869, 78.2676),
    "Ujjain":        (23.1828, 75.7772),
    "Tirupati":      (13.6288, 79.4192),
    "Amritsar":      (31.6340, 74.8723),
    "Chandigarh":    (30.7333, 76.7794),
    "Dehradun":      (30.3165, 78.0322),
    "Ranchi":        (23.3441, 85.3096),
    "Robertsganj":   (24.6917, 83.0818),
    "Ayodhya":       (26.7922, 82.1998),
    "Dwarka":        (22.2394, 68.9678),
    "Puri":          (19.8135, 85.8312),
    "Kashi (Varanasi)": (25.3176, 82.9739),
    "Nashik":        (19.9975, 73.7898),
    "Coimbatore":    (11.0168, 76.9558),
    "Madurai":       (9.9252, 78.1198),
    "Srinagar":      (34.0837, 74.7973),
    "Guwahati":      (26.1445, 91.7362),
    "Bhubaneswar":   (20.2961, 85.8245),
    "Thiruvananthapuram": (8.5241, 76.9366),
}
CITY_NAMES = ["(use lat/lon below)"] + sorted(CITIES.keys())

# ── KUNDALI CONSTANTS ─────────────────────────────────────────
try:
    import swisseph as swe
    SWE_AVAILABLE = True
except ImportError:
    SWE_AVAILABLE = False
    print("WARNING: pyswisseph not installed. Kundali tab disabled.")

GRAHAS_MAP = None  # populated after swe import

def _init_swe_grahas():
    global GRAHAS_MAP
    if SWE_AVAILABLE and GRAHAS_MAP is None:
        GRAHAS_MAP = {
            swe.SUN: "Surya", swe.MOON: "Chandra", swe.MARS: "Mangal",
            swe.MERCURY: "Budh", swe.JUPITER: "Guru", swe.VENUS: "Shukra",
            swe.SATURN: "Shani", swe.MEAN_NODE: "Rahu",
        }

RASHI_SHORT = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
               "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"]

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
HOUSE_HINDI = {
    1:"तनु भाव (स्वयं/शरीर)",2:"धन भाव (धन/वाणी)",3:"सहज भाव (भाई-बहन)",
    4:"सुख भाव (माता/घर)",5:"सन्तान भाव (विद्या/संतान)",6:"रोग भाव (शत्रु/ऋण)",
    7:"दारा भाव (विवाह/साझीदार)",8:"आयु भाव (मृत्यु/रहस्य)",
    9:"भाग्य भाव (धर्म/भाग्य)",10:"कर्म भाव (कार्य/राज्य)",
    11:"लाभ भाव (आय/मित्र)",12:"व्यय भाव (मोक्ष/व्यय)"
}

# ── KUNDALI CALCULATION ───────────────────────────────────────
def calc_kundali(year, month, day, hour, minute, lat, lon, tz, name="", place=""):
    """Calculate full Vedic birth chart. Returns dict."""
    if not SWE_AVAILABLE:
        return None, "pyswisseph not installed. Run: pip install pyswisseph"

    _init_swe_grahas()
    swe.set_ephe_path(EPHE_PATH)
    swe.set_sid_mode(swe.SIDM_LAHIRI)

    ut_hour = hour + minute / 60.0 - tz
    jd = swe.julday(year, month, day, ut_hour)
    ayanamsha = swe.get_ayanamsa(jd)

    # Grahas
    grahas = {}
    for pid, gname in GRAHAS_MAP.items():
        flags = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
        res = swe.calc_ut(jd, pid, flags)
        lon_g = res[0][0]
        speed = res[0][3]
        rashi_i = int(lon_g / 30)
        nak_i = int(lon_g / (360/27))
        grahas[gname] = {
            "longitude": lon_g, "speed": speed,
            "rashi": rashi_i, "rashi_name": RASHI_SHORT[rashi_i],
            "degree": lon_g % 30,
            "nakshatra": NAKSHATRAS[nak_i], "nakshatra_idx": nak_i,
            "pada": int((lon_g % (360/27)) / (360/108)) + 1,
            "retro": speed < 0, "house": 0,
        }

    # Ketu
    rahu_lon = grahas["Rahu"]["longitude"]
    ketu_lon = (rahu_lon + 180) % 360
    ketu_rashi = int(ketu_lon / 30)
    ketu_nak = int(ketu_lon / (360/27))
    grahas["Ketu"] = {
        "longitude": ketu_lon, "speed": 0,
        "rashi": ketu_rashi, "rashi_name": RASHI_SHORT[ketu_rashi],
        "degree": ketu_lon % 30,
        "nakshatra": NAKSHATRAS[ketu_nak], "nakshatra_idx": ketu_nak,
        "pada": int((ketu_lon % (360/27)) / (360/108)) + 1,
        "retro": True, "house": 0,
    }

    # Houses (Placidus sidereal)
    cusps, ascmc = swe.houses_ex(jd, lat, lon, b'P', swe.FLG_SIDEREAL)
    lagna = ascmc[0]
    houses = []
    for i in range(12):
        c = cusps[i]
        ri = int(c / 30)
        houses.append({
            "house": i+1, "cusp": c,
            "rashi": ri, "rashi_name": RASHI_SHORT[ri],
            "degree": c % 30, "lord": RASHI_LORDS[ri],
        })

    # Assign house to each graha
    def get_house(lon_val):
        for i in range(12):
            start = cusps[i]
            end = cusps[(i+1) % 12]
            if end < start:
                if lon_val >= start or lon_val < end:
                    return i + 1
            else:
                if start <= lon_val < end:
                    return i + 1
        return 1
    for g in grahas.values():
        g["house"] = get_house(g["longitude"])

    # Graha status
    def graha_status(gname):
        r = grahas[gname]["rashi"]
        if EXALTATION.get(gname) == r: return "Uchcha (Exalted)"
        if DEBILITATION.get(gname) == r: return "Neecha (Debilitated)"
        if RASHI_LORDS.get(r) == gname: return "Svakshetra (Own)"
        return "Normal"

    # Vimshottari Dasha
    moon = grahas["Chandra"]
    nak_i = moon["nakshatra_idx"]
    nak_lord = NAKSHATRA_LORDS[nak_i]
    nak_span = 360/27
    nak_start = nak_i * nak_span
    elapsed = moon["longitude"] - nak_start
    remaining = 1 - (elapsed / nak_span)
    start_idx = DASHA_SEQ.index(nak_lord)
    birth_dt = datetime(year, month, day, hour, minute)
    dashas = []
    cur = birth_dt
    lord = DASHA_SEQ[start_idx]
    days = int(DASHA_YEARS[lord] * remaining * 365.25)
    end = datetime.fromordinal(cur.toordinal() + days)
    dashas.append({"lord": lord, "years": round(DASHA_YEARS[lord]*remaining,2),
                   "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d")})
    cur = end
    for i in range(1, 10):
        lord = DASHA_SEQ[(start_idx + i) % 9]
        days = int(DASHA_YEARS[lord] * 365.25)
        end = datetime.fromordinal(cur.toordinal() + days)
        dashas.append({"lord": lord, "years": DASHA_YEARS[lord],
                       "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d")})
        cur = end

    # Yogas
    yogas = []
    def rashi_of(n): return grahas[n]["rashi"]
    def house_of(n): return grahas[n]["house"]

    # Gajakesari
    diff = (rashi_of("Guru") - rashi_of("Chandra")) % 12
    if diff in [0,3,6,9]:
        yogas.append({"name":"Gajakesari Yoga","desc":"गुरु चन्द्र से केन्द्र में — बुद्धि, यश, समृद्धि","strength":"Strong"})
    # Budhaditya
    if rashi_of("Surya") == rashi_of("Budh"):
        yogas.append({"name":"Budhaditya Yoga","desc":"सूर्य-बुध एक राशि — तीव्र बुद्धि, वाक्पटुता","strength":"Strong"})
    # Chandra-Mangal
    if rashi_of("Chandra") == rashi_of("Mangal"):
        yogas.append({"name":"Chandra-Mangal Yoga","desc":"चन्द्र-मंगल एक राशि — साहस से धन प्राप्ति","strength":"Strong"})
    # Panch Mahapurusha
    for g,(yn,yd) in [("Mangal",("Ruchaka Yoga","शौर्य, नेतृत्व")),
                      ("Budh",("Bhadra Yoga","बुद्धि, व्यापार")),
                      ("Guru",("Hamsa Yoga","आध्यात्मिकता, ज्ञान")),
                      ("Shukra",("Malavya Yoga","सौन्दर्य, कला")),
                      ("Shani",("Sasa Yoga","शक्ति, अनुशासन"))]:
        h = house_of(g); r = rashi_of(g)
        if h in [1,4,7,10] and (RASHI_LORDS.get(r)==g or EXALTATION.get(g)==r):
            yogas.append({"name":yn,"desc":yd,"strength":"Very Strong"})
    # Mangal Dosha
    mh = house_of("Mangal")
    if mh in [1,2,4,7,8,12]:
        yogas.append({"name":"Mangal Dosha","desc":f"मंगल भाव {mh} में — विवाह में सावधानी आवश्यक","strength":"Applicable"})
    # Raj Yoga
    kendra_lords = set(); trikona_lords = set()
    for h in houses:
        if h["house"] in [1,4,7,10]: kendra_lords.add(h["lord"])
        if h["house"] in [1,5,9]: trikona_lords.add(h["lord"])
    ry = kendra_lords & trikona_lords
    if ry:
        yogas.append({"name":"Raj Yoga","desc":f"केन्द्र-त्रिकोण स्वामी ({', '.join(ry)}) — सफलता, यश","strength":"Strong"})

    return {
        "name": name, "place": place,
        "birth_date": birth_dt.strftime("%d %B %Y, %I:%M %p"),
        "lat": lat, "lon": lon, "tz": tz,
        "ayanamsha": round(ayanamsha, 4),
        "lagna": {
            "degree": lagna, "rashi": RASHI_SHORT[int(lagna/30)],
            "nakshatra": NAKSHATRAS[int(lagna/(360/27))],
            "degree_in_rashi": round(lagna % 30, 2),
        },
        "grahas": grahas,
        "houses": houses,
        "dashas": dashas,
        "yogas": yogas,
        "graha_status": {g: graha_status(g) for g in grahas},
    }, None


def format_kundali_md(data: dict) -> str:
    """Format Kundali dict as Markdown for Gradio display."""
    lines = []
    lines.append("## 🕉️ जन्म कुण्डली (Janam Kundali)")
    if data["name"]:
        lines.append(f"**नाम (Name):** {data['name']}")
    lines.append(f"**जन्म (Birth):** {data['birth_date']}")
    if data["place"]:
        lines.append(f"**स्थान (Place):** {data['place']}  ({data['lat']:.4f}°N, {data['lon']:.4f}°E)")
    lines.append(f"**अयनांश — Lahiri:** {data['ayanamsha']}°")

    lines.append("\n---")
    lines.append("### लग्न (Lagna / Ascendant)")
    lg = data["lagna"]
    rh = RASHI_HINDI.get(lg["rashi"], lg["rashi"])
    nh = NAKSHATRA_HINDI.get(lg["nakshatra"], lg["nakshatra"])
    lines.append(f"**राशि:** {rh} ({lg['rashi']})  |  **अंश:** {lg['degree_in_rashi']}°  |  **नक्षत्र:** {nh}")

    lines.append("\n### ग्रह स्थिति (Planetary Positions)")
    lines.append("| ग्रह | राशि | अंश | नक्षत्र | पाद | भाव | स्थिति |")
    lines.append("|------|------|-----|---------|-----|-----|--------|")
    graha_order = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"]
    for gn in graha_order:
        g = data["grahas"][gn]
        st = data["graha_status"][gn]
        retro = " (R)" if g["retro"] else ""
        gh = GRAHA_HINDI.get(gn, gn)
        rh = RASHI_HINDI.get(g["rashi_name"], g["rashi_name"])
        nh = NAKSHATRA_HINDI.get(g["nakshatra"], g["nakshatra"])
        lines.append(f"| **{gh}** | {rh} | {g['degree']:.1f}° | {nh} | {g['pada']} | {g['house']} | {st}{retro} |")

    lines.append("\n### भाव चक्र (Bhava Chakra / Houses)")
    lines.append("| भाव | राशि | अंश | स्वामी | अर्थ |")
    lines.append("|-----|------|-----|--------|------|")
    for h in data["houses"]:
        rh = RASHI_HINDI.get(h["rashi_name"], h["rashi_name"])
        lh = GRAHA_HINDI.get(h["lord"], h["lord"])
        hm = HOUSE_HINDI.get(h["house"], "")
        lines.append(f"| {h['house']} | {rh} | {h['degree']:.1f}° | {lh} | {hm} |")

    lines.append("\n### विंशोत्तरी महादशा (Vimshottari Mahadasha)")
    lines.append("| दशा स्वामी | अवधि | आरम्भ | समाप्ति |")
    lines.append("|-----------|------|-------|---------|")
    now = datetime.now()
    for d in data["dashas"]:
        s = datetime.strptime(d["start"], "%Y-%m-%d")
        e = datetime.strptime(d["end"], "%Y-%m-%d")
        cur = " **← वर्तमान**" if s <= now <= e else ""
        gh = GRAHA_HINDI.get(d["lord"], d["lord"])
        lines.append(f"| **{gh}** ({d['lord']}) | {d['years']} वर्ष | {d['start']} | {d['end']}{cur} |")

    lines.append("\n### योग (Yogas Detected)")
    if data["yogas"]:
        for y in data["yogas"]:
            lines.append(f"**{y['name']}** `[{y['strength']}]`")
            lines.append(f"→ {y['desc']}")
            lines.append("")
    else:
        lines.append("कोई प्रमुख योग नहीं पाया गया।")

    lines.append("\n---")
    lines.append("*गणना: pyswisseph + Lahiri Ayanamsha | Brahm AI Jyotish Module*")
    return "\n".join(lines)


def calculate_kundali_ui(name, date_str, time_str, city_sel, lat_str, lon_str, tz_str):
    """Gradio callback for Kundali tab."""
    try:
        dt = datetime.strptime(f"{date_str} {time_str}", "%Y-%m-%d %H:%M")
    except ValueError as e:
        return f"**तिथि/समय त्रुटि:** {e}\n\nFormat: Date=YYYY-MM-DD, Time=HH:MM"

    if city_sel and city_sel != "(use lat/lon below)" and city_sel in CITIES:
        lat, lon = CITIES[city_sel]
        place = city_sel
    else:
        try:
            lat = float(lat_str)
            lon = float(lon_str)
            place = f"{lat:.4f}°N, {lon:.4f}°E"
        except (ValueError, TypeError):
            return "**स्थान त्रुटि:** Please select a city or enter valid Latitude/Longitude."

    try:
        tz = float(tz_str) if tz_str else 5.5
    except ValueError:
        tz = 5.5

    data, err = calc_kundali(
        year=dt.year, month=dt.month, day=dt.day,
        hour=dt.hour, minute=dt.minute,
        lat=lat, lon=lon, tz=tz,
        name=name.strip(), place=place
    )
    if err:
        return f"**Error:** {err}"
    return format_kundali_md(data)


# ── RAG GLOBAL STATE ──────────────────────────────────────────
G = {}

def load_all():
    import faiss
    from sentence_transformers import SentenceTransformer, CrossEncoder
    from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
    from rank_bm25 import BM25Okapi
    import torch

    print("Loading FAISS indexes...")
    G["indexes"] = {}
    for lang in ["sanskrit", "hindi", "english"]:
        p = os.path.join(INDEXES_DIR, f"{lang}.index")
        if os.path.exists(p):
            G["indexes"][lang] = faiss.read_index(p)
            print(f"  {lang}: {G['indexes'][lang].ntotal}")

    print("Loading documents + BM25...")
    docs = {}
    lang_docs = {"sanskrit": [], "hindi": [], "english": []}
    with open(DOCS_PATH) as f:
        for line in f:
            d = json.loads(line)
            lang_docs[d.get("language", "sanskrit")].append(d)
    all_keys, all_texts = [], []
    for lang in lang_docs:
        for local_idx, d in enumerate(sorted(lang_docs[lang], key=lambda x: x.get("faiss_id", 0))):
            docs[(lang, local_idx)] = d
            all_keys.append((lang, local_idx))
            all_texts.append(d.get("text", "").lower().split())
    G["docs"] = docs
    G["all_keys"] = all_keys
    G["bm25"] = BM25Okapi(all_texts)
    print(f"  {len(docs)} docs, BM25 ready")

    print("Loading embedding model...")
    G["embed"] = SentenceTransformer(EMBED_MODEL)

    print("Loading cross-encoder...")
    G["reranker"] = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")

    print("Loading Qwen LLM...")
    bnb = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_compute_dtype=torch.float16, bnb_4bit_quant_type="nf4")
    G["tokenizer"] = AutoTokenizer.from_pretrained(LLM_MODEL)
    G["llm"] = AutoModelForCausalLM.from_pretrained(LLM_MODEL, quantization_config=bnb, device_map="auto")
    vram = torch.cuda.memory_allocated() / 1e9
    print(f"  Loaded. VRAM: {vram:.1f} GB")

    G["ocr_quality"] = {}
    try:
        cr = json.load(open(os.path.join(LOG_DIR, "correction_report.json")))
        for b in cr.get("book_scores", []):
            G["ocr_quality"][b["source"]] = min(1.0, b["avg_score"] / 0.80)
        print(f"  OCR quality: {len(G['ocr_quality'])} files")
    except Exception:
        pass

    print("All components loaded!")


def src_boost(src):
    s = src.lower()
    if any(s.startswith(p) for p in ["bhagavadgita/","sarit/","gretil/","suttacentral/"]):
        return 1.1
    if s.startswith("openphilology/"): return 1.0
    if any(s.startswith(p) for p in ["wikisource/","wikipedia_"]): return 0.95
    if "sanskrit_mono" in s or "hf/mono" in s: return 0.30
    if s.startswith("hf/"): return 0.85
    if s.endswith(".pdf") and re.search(r"\d{4}", s): return 0.75
    return 0.90


def hybrid_search(query, lang_filter=None):
    indexes, docs = G["indexes"], G["docs"]
    all_keys, bm25 = G["all_keys"], G["bm25"]
    embed, reranker = G["embed"], G["reranker"]
    ocr_quality = G.get("ocr_quality", {})

    qv = embed.encode([query], normalize_embeddings=True).astype(np.float32)
    deva = sum(1 for c in query if "\u0900" <= c <= "\u097F")
    if lang_filter and lang_filter != "all":
        search_langs = [lang_filter]
    elif deva / max(len(query.replace(" ","")), 1) > 0.2:
        search_langs = ["sanskrit", "hindi"]
    else:
        search_langs = list(indexes.keys())

    faiss_res = {}
    for sl in search_langs:
        if sl not in indexes: continue
        D, I = indexes[sl].search(qv, FAISS_K)
        for rank, (dist, idx) in enumerate(zip(D[0], I[0])):
            if idx >= 0:
                faiss_res[(sl, int(idx))] = {"faiss_rank": rank, "faiss_dist": float(dist)}

    tokens = query.lower().split()
    bm25_scores = bm25.get_scores(tokens)
    top_idx = np.argsort(bm25_scores)[::-1][:BM25_K]
    bm25_res = {}
    for rank, idx in enumerate(top_idx):
        key = all_keys[idx]
        if lang_filter and lang_filter != "all" and key[0] != lang_filter: continue
        if bm25_scores[idx] > 0:
            bm25_res[key] = {"bm25_rank": rank}

    k = 60
    rrf = {}
    for key in set(faiss_res) | set(bm25_res):
        score = 0
        if key in faiss_res: score += 1.0 / (k + faiss_res[key]["faiss_rank"])
        if key in bm25_res:  score += 1.0 / (k + bm25_res[key]["bm25_rank"])
        rrf[key] = score

    top_keys = sorted(rrf, key=lambda x: rrf[x], reverse=True)[:FAISS_K]
    candidates = []
    for key in top_keys:
        doc = docs.get(key)
        if doc:
            candidates.append({"key": key, "text": doc.get("text",""), "source": doc.get("source","?"),
                                "language": key[0], "rrf_score": rrf[key]})

    if candidates:
        pairs = [(query, c["text"][:512]) for c in candidates]
        scores = reranker.predict(pairs)
        for i, c in enumerate(candidates):
            c["rerank_score"] = float(scores[i])
        for c in candidates:
            q = ocr_quality.get(c["source"].split("/")[-1], ocr_quality.get(c["source"], 1.0))
            c["rerank_score"] *= src_boost(c["source"]) * q
        candidates.sort(key=lambda x: x["rerank_score"], reverse=True)

    return candidates[:FINAL_K]


def generate_stream(query, retrieved, history):
    import torch
    from transformers import TextIteratorStreamer
    def _clean(t): return re.sub(r"Transliteration:[^\n]*\n?", "", t).strip()

    ctx = "\n\n---\n\n".join(
        f"[Source {i}: {d['source']} ({d['language']})]\n{_clean(d['text'][:1000])}"
        for i, d in enumerate(retrieved, 1)
    )
    msgs = [{"role": "system", "content": SYSTEM_PROMPT}]
    for u, a in history[-MAX_HISTORY:]:
        msgs.append({"role": "user", "content": u})
        msgs.append({"role": "assistant", "content": a})
    msgs.append({"role": "user", "content":
                 f"Context:\n{ctx}\n\n---\nQuestion: {query}\n\nAnswer strictly from context above."})

    tok = G["tokenizer"]
    llm = G["llm"]
    text = tok.apply_chat_template(msgs, tokenize=False, add_generation_prompt=True)
    inputs = tok(text, return_tensors="pt").to(llm.device)
    streamer = TextIteratorStreamer(tok, skip_prompt=True, skip_special_tokens=True)
    gen_kwargs = dict(**inputs, max_new_tokens=768, temperature=0.3, top_p=0.9,
                      do_sample=True, repetition_penalty=1.3, streamer=streamer)
    threading.Thread(target=llm.generate, kwargs=gen_kwargs).start()
    for token in streamer:
        yield token


# Detect kundali intent to hint user to switch tab
_KUNDALI_KEYWORDS = {"kundali","कुंडली","जन्म पत्रिका","birth chart","horoscope",
                     "lagna","लग्न","dasha","दशा","janma","janam"}

_PANCHANG_KEYWORDS = {"panchang","panchanga","पञ्चाङ्ग","पंचांग","tithi","तिथि",
                      "nakshatra","नक्षत्र","rahukaal","राहुकाल","muhurta","मुहूर्त",
                      "vara","aaj ka panchang","today panchang"}

def is_kundali_query(query: str) -> bool:
    ql = query.lower()
    return any(kw in ql for kw in _KUNDALI_KEYWORDS)

def is_panchang_query(query: str) -> bool:
    ql = query.lower()
    return any(kw in ql for kw in _PANCHANG_KEYWORDS)


def respond(message, chat_history, lang_filter):
    if not message.strip():
        yield chat_history, ""
        return

    # Kundali routing hint
    if is_kundali_query(message):
        hint = ("**Kundali/Jyotish Calculator →** Please switch to the **Kundali** tab above "
                "to calculate a birth chart.\n\n"
                "यदि आप किसी ज्योतिष सिद्धान्त के बारे में पूछना चाहते हैं तो यहाँ पूछें।")
        chat_history = chat_history + [{"role": "user", "content": message},
                                        {"role": "assistant", "content": hint}]
        yield chat_history, ""
        return

    # Panchang routing hint
    if is_panchang_query(message):
        hint = ("**Panchang Calculator →** Please switch to the **Panchang** tab above "
                "to view today's Tithi, Nakshatra, Yoga, Karana, Rahukaal, and Muhurta.\n\n"
                "पञ्चाङ्ग टैब में जाएं — तिथि, नक्षत्र, राहुकाल, मुहूर्त सब मिलेगा।")
        chat_history = chat_history + [{"role": "user", "content": message},
                                        {"role": "assistant", "content": hint}]
        yield chat_history, ""
        return

    lang = None if lang_filter == "all" else lang_filter
    retrieved = hybrid_search(message, lang)
    sources_txt = "\n".join(f"[{i+1}] {r['source']} ({r['language']})" for i, r in enumerate(retrieved))

    partial = ""
    history_tuples = [(m["content"], chat_history[i+1]["content"])
                      for i, m in enumerate(chat_history[:-1]) if m["role"] == "user"]
    chat_history = chat_history + [{"role": "user", "content": message},
                                    {"role": "assistant", "content": ""}]

    for token in generate_stream(message, retrieved, history_tuples):
        partial += token
        chat_history[-1]["content"] = partial
        yield chat_history, sources_txt


# ── PANCHANG UI FUNCTION ──────────────────────────────────────
def calculate_panchang_ui(p_date, p_city, p_lat, p_lon, p_tz, p_hour, p_min):
    if not PANCHANG_AVAILABLE:
        return "**Error:** panchang.py not found on server."
    try:
        # Resolve lat/lon
        lat = float(p_lat) if p_lat.strip() else None
        lon = float(p_lon) if p_lon.strip() else None
        if (lat is None or lon is None) and p_city and p_city in CITIES:
            lat, lon = CITIES[p_city]
        if lat is None or lon is None:
            return "**Error:** Please select a city or enter lat/lon."

        tz = float(p_tz) if p_tz.strip() else 5.5

        # Parse date
        if not p_date.strip():
            dt = datetime.now()
        else:
            dt = datetime.strptime(p_date.strip(), "%Y-%m-%d")

        hour = int(p_hour) if str(p_hour).strip() else 6
        minute = int(p_min) if str(p_min).strip() else 0

        p = Panchang(dt.year, dt.month, dt.day,
                     hour=hour, minute=minute,
                     lat=lat, lon=lon, tz=tz)
        d = p.to_dict()

        # Format as Markdown
        location_name = p_city if (p_city and p_city != "(use lat/lon below)") else f"{lat:.4f}°N, {lon:.4f}°E"
        md = f"""## पञ्चाङ्ग — {d['date']}
**स्थान:** {location_name} &nbsp;|&nbsp; **UTC+{tz}**

| | |
|---|---|
| **सूर्योदय (Sunrise)** | {d['sunrise']} |
| **सूर्यास्त (Sunset)** | {d['sunset']} |
| **अयनांश (Lahiri)** | {d['ayanamsha_lahiri']}° |

---

### पञ्चाङ्ग के पाँच अंग

| अंग | विवरण |
|---|---|
| **वार (Vara)** | {d['vara']['hindi']} ({d['vara']['name']}) — स्वामी: {d['vara']['lord']} |
| **तिथि (Tithi)** | {d['tithi']['paksha_hindi']} **{d['tithi']['hindi']}** ({d['tithi']['paksha']} {d['tithi']['name']}) — समाप्ति ~{d['tithi']['end_time']} |
| **नक्षत्र (Nakshatra)** | **{d['nakshatra']['hindi']}** ({d['nakshatra']['name']}) पाद {d['nakshatra']['pada']}/4 — स्वामी: {d['nakshatra']['lord']} |
| **योग (Yoga)** | **{d['yoga']['hindi']}** ({d['yoga']['name']}) — {'शुभ ✓' if d['yoga']['is_auspicious'] else 'अशुभ ✗'} |
| **करण (Karana)** | **{d['karana']['hindi']}** ({d['karana']['name']}) — {'चर' if not d['karana']['is_fixed'] else 'स्थिर'} |

---

### मुहूर्त / काल

| | समय |
|---|---|
| **अभिजित मुहूर्त** | {d['abhijit_muhurta']['start']} — {d['abhijit_muhurta']['end']} ✨ (सर्वश्रेष्ठ) |
| **राहुकाल** | {d['rahukaal']['start']} — {d['rahukaal']['end']} ⚠️ (वर्जित) |

---
*गणना: pyswisseph + Lahiri Ayanamsha | Brahm AI Panchang*"""
        return md

    except ValueError as e:
        return f"**Input Error:** {e}\n\nDate format: YYYY-MM-DD (e.g. 2026-03-15)"
    except Exception as e:
        import traceback
        return f"**Error:** {e}\n\n```\n{traceback.format_exc()}\n```"


# ── GRADIO UI ─────────────────────────────────────────────────
with gr.Blocks(title="Brahm AI", theme=gr.themes.Soft()) as demo:
    gr.Markdown("# 🕉️ Brahm AI — Vedic Knowledge & Jyotish Assistant")

    with gr.Tabs():
        # ── TAB 1: RAG CHAT ───────────────────────────────────
        with gr.TabItem("📖 Vedic Chat"):
            gr.Markdown("*Search 1.1M+ chunks from Vedas, Upanishads, Gita, Puranas, Jyotish texts*")
            with gr.Row():
                with gr.Column(scale=3):
                    chatbot = gr.Chatbot(height=500, label="Chat", type="messages")
                    with gr.Row():
                        msg_box = gr.Textbox(
                            placeholder="Ask about Dharma, Karma, Yoga, Gita, Jyotish...",
                            show_label=False, scale=4)
                        send_btn = gr.Button("Send 🙏", variant="primary", scale=1)
                    with gr.Row():
                        lang_dd = gr.Dropdown(
                            ["all", "sanskrit", "hindi", "english"],
                            value="all", label="Language Filter", scale=1)
                        clear_btn = gr.Button("Clear Chat", scale=1)
                with gr.Column(scale=1):
                    sources_box = gr.Textbox(label="Retrieved Sources", lines=14, interactive=False)

            send_btn.click(respond, [msg_box, chatbot, lang_dd], [chatbot, sources_box])
            msg_box.submit(respond, [msg_box, chatbot, lang_dd], [chatbot, sources_box])
            clear_btn.click(lambda: ([], ""), outputs=[chatbot, sources_box])

        # ── TAB 2: KUNDALI ────────────────────────────────────
        with gr.TabItem("🔯 Kundali Calculator"):
            gr.Markdown("### जन्म कुण्डली — Vedic Birth Chart Calculator\n"
                        "*Lahiri Ayanamsha | Placidus Houses | Vimshottari Dasha | Yogas*")
            with gr.Row():
                with gr.Column(scale=1):
                    k_name   = gr.Textbox(label="नाम (Name)", placeholder="e.g. Ramesh Sharma")
                    k_date   = gr.Textbox(label="जन्म तिथि (Date)", placeholder="YYYY-MM-DD  e.g. 1990-05-15")
                    k_time   = gr.Textbox(label="जन्म समय (Time)", placeholder="HH:MM  e.g. 10:30 (24-hr)")
                    k_city   = gr.Dropdown(CITY_NAMES, value="New Delhi", label="जन्म स्थान — शहर चुनें")
                    with gr.Row():
                        k_lat = gr.Textbox(label="Latitude (अक्षांश)", placeholder="e.g. 28.6139")
                        k_lon = gr.Textbox(label="Longitude (देशांतर)", placeholder="e.g. 77.2090")
                    k_tz     = gr.Textbox(label="Timezone (UTC+)", value="5.5", placeholder="India=5.5")
                    k_btn    = gr.Button("कुण्डली बनाएं 🔯", variant="primary")
                with gr.Column(scale=2):
                    k_output = gr.Markdown(label="Kundali Output",
                                           value="*Enter birth details and click 'कुण्डली बनाएं'*")

            k_btn.click(
                calculate_kundali_ui,
                inputs=[k_name, k_date, k_time, k_city, k_lat, k_lon, k_tz],
                outputs=k_output
            )

            # Auto-fill lat/lon when city selected
            def city_selected(city):
                if city and city != "(use lat/lon below)" and city in CITIES:
                    lat, lon = CITIES[city]
                    return str(lat), str(lon)
                return "", ""

            k_city.change(city_selected, inputs=k_city, outputs=[k_lat, k_lon])

        # ── TAB 3: PANCHANG ───────────────────────────────────
        with gr.TabItem("📅 Panchang"):
            gr.Markdown("### पञ्चाङ्ग — Vedic Daily Almanac\n"
                        "*Tithi · Vara · Nakshatra · Yoga · Karana · Sunrise/Sunset · Rahukaal · Abhijit Muhurta*")
            with gr.Row():
                with gr.Column(scale=1):
                    p_date = gr.Textbox(label="तिथि (Date)", placeholder="YYYY-MM-DD  e.g. 2026-03-15",
                                        value=datetime.now().strftime("%Y-%m-%d"))
                    with gr.Row():
                        p_hour = gr.Number(label="घंटे (Hour)", value=6, minimum=0, maximum=23, precision=0)
                        p_min  = gr.Number(label="मिनट (Minute)", value=0, minimum=0, maximum=59, precision=0)
                    p_city = gr.Dropdown(CITY_NAMES, value="New Delhi", label="स्थान — शहर चुनें")
                    with gr.Row():
                        p_lat = gr.Textbox(label="Latitude", placeholder="e.g. 28.6139")
                        p_lon = gr.Textbox(label="Longitude", placeholder="e.g. 77.2090")
                    p_tz   = gr.Textbox(label="Timezone (UTC+)", value="5.5")
                    p_btn  = gr.Button("पञ्चाङ्ग देखें 📅", variant="primary")
                with gr.Column(scale=2):
                    p_output = gr.Markdown(value="*Enter date & location, then click 'पञ्चाङ्ग देखें'*")

            p_btn.click(
                calculate_panchang_ui,
                inputs=[p_date, p_city, p_lat, p_lon, p_tz, p_hour, p_min],
                outputs=p_output
            )

            def p_city_selected(city):
                if city and city != "(use lat/lon below)" and city in CITIES:
                    lat, lon = CITIES[city]
                    return str(lat), str(lon)
                return "", ""

            p_city.change(p_city_selected, inputs=p_city, outputs=[p_lat, p_lon])


if __name__ == "__main__":
    load_all()
    demo.launch(server_name="0.0.0.0", server_port=7860, share=True, show_error=True)
