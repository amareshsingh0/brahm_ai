"""
Marriage (Vivah) Analysis Router — Classical Jyotish Implementation

POST /api/marriage

Full Vedic analysis based on Parashari/Varahamihira rules:
  - 7th house lord identification + placement
  - Vimshottari Dasha scoring for marriage windows
  - Delay/denial factor detection (Saturn, Rahu, Ketu, Mangal in 7th)
  - Spouse profile from 7th house + Venus/Jupiter + Navamsa
  - Marriage yoga detection (Kalatra, Vivah, Daara Karaka, Manglik, etc.)
  - Gemini AI narrative
"""
from datetime import datetime, date
from typing import Optional
from fastapi import APIRouter
from pydantic import BaseModel
import os

from api.services.kundali_service import calc_kundali
from api.config import RASHI_LORDS, RASHI_NAMES, EXALTATION, DEBILITATION

router = APIRouter()


# ── Request model ─────────────────────────────────────────────────────────────
class MarriageRequest(BaseModel):
    name:   str = ""
    date:   str           # YYYY-MM-DD
    time:   str           # HH:MM
    place:  str = ""
    lat:    float
    lon:    float
    tz:     float = 5.5
    gender: str   = "male"   # "male" | "female"


# ── Constants ──────────────────────────────────────────────────────────────────

# Sanskrit rashi index → English name
RASHI_EN = {
    "Mesha": "Aries", "Vrishabha": "Taurus", "Mithuna": "Gemini",
    "Karka": "Cancer", "Simha": "Leo", "Kanya": "Virgo",
    "Tula": "Libra", "Vrischika": "Scorpio", "Dhanu": "Sagittarius",
    "Makara": "Capricorn", "Kumbha": "Aquarius", "Meena": "Pisces",
}
RASHI_EN_BY_IDX = {i: RASHI_EN.get(RASHI_NAMES[i], RASHI_NAMES[i]) for i in range(12)}

# Classical aspects for each planet (house offsets from planet position)
# Parashari aspects: all planets aspect 7th; Shani 3rd+10th; Mangal 4th+8th; Guru 5th+9th
ASPECT_HOUSES = {
    "Surya":   [7],
    "Chandra": [7],
    "Mangal":  [4, 7, 8],
    "Budh":    [7],
    "Guru":    [5, 7, 9],
    "Shukra":  [7],
    "Shani":   [3, 7, 10],
    "Rahu":    [7],
    "Ketu":    [7],
}

# Planet debilitation rashi indices (from config.py)
DEBIL = DEBILITATION  # {"Surya":6, "Chandra":7, ... }

# Spouse direction hints from 7th lord rashi
DIRECTION_MAP = {
    "Mesha":"East","Vrishabha":"South","Mithuna":"West","Karka":"North",
    "Simha":"East","Kanya":"South","Tula":"West","Vrischika":"North",
    "Dhanu":"East","Makara":"South","Kumbha":"West","Meena":"North",
}

# Physical/trait hints from planet in 7th house
PLANET_IN_7TH_TRAITS = {
    "Surya":   {
        "traits": ["Authoritative", "Leadership quality", "Proud", "Self-reliant"],
        "profession": "Government service, management, leadership roles",
        "nature": "Dominant and respected personality",
    },
    "Chandra": {
        "traits": ["Emotional", "Nurturing", "Sensitive", "Imaginative"],
        "profession": "Caretaking, arts, hospitality, or public roles",
        "nature": "Changeable moods, very caring",
    },
    "Mangal":  {
        "traits": ["Energetic", "Athletic", "Assertive", "Courageous"],
        "profession": "Engineering, military, sports, real estate",
        "nature": "Passionate but possibly argumentative",
    },
    "Budh":    {
        "traits": ["Intellectual", "Communicative", "Witty", "Youthful"],
        "profession": "Writing, teaching, IT, media, commerce",
        "nature": "Analytical and expressive",
    },
    "Guru":    {
        "traits": ["Wise", "Educated", "Spiritual", "Generous", "Ethical"],
        "profession": "Teaching, law, finance, philosophy, medicine",
        "nature": "Noble, principled, respected in society",
    },
    "Shukra":  {
        "traits": ["Beautiful", "Artistic", "Refined", "Luxury-loving"],
        "profession": "Arts, fashion, entertainment, beauty, finance",
        "nature": "Charming, romantic, fond of pleasures",
    },
    "Shani":   {
        "traits": ["Mature", "Disciplined", "Serious", "Hardworking"],
        "profession": "Law, engineering, agriculture, real estate",
        "nature": "Reserved, possibly older or from different background",
    },
    "Rahu":    {
        "traits": ["Unconventional", "Ambitious", "Different background", "Worldly"],
        "profession": "Technology, foreign fields, unconventional careers",
        "nature": "Surprising personality, may be from different culture/caste",
    },
    "Ketu":    {
        "traits": ["Spiritual", "Detached", "Mysterious", "Past-life connection"],
        "profession": "Healing, spirituality, research, occult",
        "nature": "Introspective, unusual connection from past",
    },
}

# 7th house sign traits for spouse general description
RASHI_7TH_TRAITS = {
    "Mesha":     ["Dynamic","Independent","Assertive","Athletic"],
    "Vrishabha": ["Steady","Sensual","Loyal","Artistic"],
    "Mithuna":   ["Communicative","Witty","Adaptable","Dual-natured"],
    "Karka":     ["Caring","Emotional","Home-loving","Intuitive"],
    "Simha":     ["Proud","Generous","Leader","Creative"],
    "Kanya":     ["Analytical","Service-oriented","Perfectionist","Intelligent"],
    "Tula":      ["Balanced","Charming","Justice-oriented","Refined"],
    "Vrischika": ["Intense","Passionate","Secretive","Transformative"],
    "Dhanu":     ["Philosophical","Freedom-loving","Adventurous","Honest"],
    "Makara":    ["Ambitious","Disciplined","Practical","Status-conscious"],
    "Kumbha":    ["Humanitarian","Unconventional","Intellectual","Independent"],
    "Meena":     ["Compassionate","Spiritual","Dreamy","Empathetic"],
}

# Dasha lord scoring for marriage
MARRIAGE_LORDS = {"Shukra", "Guru"}  # Strong natural karakas


# ── Core calculation functions ─────────────────────────────────────────────────

def _get_7th_house(lagna_rashi_i: int) -> int:
    """Return rashi index of 7th house (0-11)."""
    return (lagna_rashi_i + 6) % 12

def _get_house_number_of_planet(planet_rashi: int, lagna_rashi_i: int) -> int:
    """Return house number (1-12) where a planet sits."""
    return (planet_rashi - lagna_rashi_i) % 12 + 1

def _does_planet_aspect_house(planet_name: str, planet_house: int, target_house: int) -> bool:
    """Check if planet aspects the target house using Parashari aspects."""
    offsets = ASPECT_HOUSES.get(planet_name, [7])
    for offset in offsets:
        if (planet_house - 1 + offset - 1) % 12 + 1 == target_house:
            return True
    return False

def _is_debilitated(planet_name: str, rashi_idx: int) -> bool:
    return DEBIL.get(planet_name) == rashi_idx

def _is_exalted(planet_name: str, rashi_idx: int) -> bool:
    return EXALTATION.get(planet_name) == rashi_idx

def _score_dasha_lord(lord: str, seventh_lord: str, fifth_lord: str,
                      eleventh_lord: str, second_lord: str,
                      grahas: dict, seventh_house: int, lagna_rashi_i: int) -> int:
    """
    Score a dasha/antardasha lord for marriage probability.
    Based on Parashari rules: 7th lord, Venus, Jupiter most important.
    """
    score = 0
    if lord not in grahas:
        return 0

    g = grahas[lord]
    planet_house = g["house"]
    planet_rashi_idx = RASHI_NAMES.index(g["rashi"]) if g["rashi"] in RASHI_NAMES else -1

    # Natural karakas for marriage
    if lord == "Shukra":  score += 3   # Venus — strongest karaka
    if lord == "Guru":    score += 2   # Jupiter — 2nd karaka (especially for women)

    # 7th lord running as dasha lord — very strong
    if lord == seventh_lord: score += 3

    # Lords of marriage-related houses
    if lord == fifth_lord:   score += 2   # 5th = romance
    if lord == eleventh_lord: score += 1  # 11th = fulfillment
    if lord == second_lord:  score += 1   # 2nd = family

    # Planet placed in marriage houses
    if planet_house in [7, 5, 11]: score += 2

    # Planet aspects 7th house
    if _does_planet_aspect_house(lord, planet_house, 7): score += 2

    # Planet aspected by Venus or Jupiter
    shukra = grahas.get("Shukra")
    guru   = grahas.get("Guru")
    if shukra:
        if _does_planet_aspect_house("Shukra", shukra["house"], planet_house): score += 1
    if guru:
        if _does_planet_aspect_house("Guru", guru["house"], planet_house): score += 1

    # Penalty: planet debilitated reduces effectiveness
    if planet_rashi_idx >= 0 and _is_debilitated(lord, planet_rashi_idx): score -= 1

    return max(score, 0)

def _score_windows(dashas: list, seventh_lord: str, fifth_lord: str,
                   eleventh_lord: str, second_lord: str,
                   grahas: dict, seventh_house: int, lagna_rashi_i: int,
                   birth_year: int) -> dict:
    """
    Score each Mahadasha and its Antardashas for marriage probability.
    Returns: current_period, favorable_windows, next_strong_window.
    """
    today = date.today()
    today_str = today.strftime("%Y-%m-%d")

    current_period = None
    favorable_windows = []
    next_strong_window = None

    for maha in dashas:
        maha_lord = maha["lord"]
        maha_start = maha["start"]
        maha_end   = maha["end"]
        maha_score = _score_dasha_lord(
            maha_lord, seventh_lord, fifth_lord, eleventh_lord, second_lord,
            grahas, seventh_house, lagna_rashi_i
        )

        antars = maha.get("antardashas", [])
        if not antars:
            # No antardasha data — use mahadasha level
            prob = _score_to_prob(maha_score)
            entry = {
                "period": f"{maha_lord} Mahadasha",
                "start": maha_start, "end": maha_end,
                "score": maha_score, "probability": prob,
            }
            if maha_start <= today_str <= maha_end:
                current_period = entry
            if maha_score >= 4 and maha_start >= today_str:
                favorable_windows.append(entry)
            continue

        for antar in antars:
            antar_lord = antar["lord"]
            antar_start = antar["start"]
            antar_end   = antar["end"]

            # Combined score: mahadasha + antardasha
            antar_score = _score_dasha_lord(
                antar_lord, seventh_lord, fifth_lord, eleventh_lord, second_lord,
                grahas, seventh_house, lagna_rashi_i
            )
            combined = maha_score + antar_score
            prob = _score_to_prob(combined)

            entry = {
                "period": f"{maha_lord} MD / {antar_lord} AD",
                "start": antar_start, "end": antar_end,
                "score": combined, "probability": prob,
            }

            if antar_start <= today_str <= antar_end:
                current_period = entry

            if combined >= 5 and antar_end >= today_str:
                favorable_windows.append(entry)

    # Sort favorable windows by start date, keep top 5
    favorable_windows.sort(key=lambda x: x["start"])
    favorable_windows = favorable_windows[:5]

    # Next strong window = first favorable future window not currently running
    for w in favorable_windows:
        if w["start"] > today_str:
            next_strong_window = f"{w['start'][:7]} to {w['end'][:7]}"
            break

    if not next_strong_window and favorable_windows:
        # current period is the best window
        next_strong_window = "Current period is favorable"

    return {
        "current_period": current_period,
        "favorable_windows": favorable_windows,
        "next_strong_window": next_strong_window or "No strong window in current dasha cycle",
    }

def _score_to_prob(score: int) -> str:
    if score >= 7:  return "Very High"
    if score >= 5:  return "High"
    if score >= 3:  return "Moderate"
    return "Low"

def _prob_to_pct(prob: str) -> int:
    return {"Very High": 88, "High": 72, "Moderate": 50, "Low": 28}.get(prob, 40)

def _evaluate_delay_factors(grahas: dict, seventh_house: int, seventh_lord: str,
                              lagna_rashi_i: int) -> list:
    """
    Check classical delay/denial indicators per Parashari rules.
    """
    factors = []
    today = date.today()

    for planet_name, g in grahas.items():
        ph = g["house"]
        ri = RASHI_NAMES.index(g["rashi"]) if g["rashi"] in RASHI_NAMES else -1

        # Saturn in 7th — delay, older spouse possible
        if planet_name == "Shani" and ph == 7:
            factors.append({
                "factor": "Saturn (Shani) in 7th house",
                "effect": "Delay in marriage; age difference with spouse possible",
                "severity": "High",
            })
        # Saturn aspects 7th house
        elif planet_name == "Shani" and _does_planet_aspect_house("Shani", ph, 7):
            factors.append({
                "factor": "Saturn aspects 7th house",
                "effect": "Moderate delay; marriage after thorough consideration",
                "severity": "Moderate",
            })

        # Rahu in 7th — unconventional marriage
        if planet_name == "Rahu" and ph == 7:
            factors.append({
                "factor": "Rahu in 7th house",
                "effect": "Unconventional or inter-cultural marriage; some delay",
                "severity": "Moderate",
            })

        # Ketu in 7th — disinterest, karmic bond
        if planet_name == "Ketu" and ph == 7:
            factors.append({
                "factor": "Ketu in 7th house",
                "effect": "Spiritual or karmic relationship; possible disinterest in worldly marriage",
                "severity": "Moderate",
            })

        # Mangal (Kuja Dosha) in 1/4/7/8/12
        if planet_name == "Mangal" and ph in [1, 4, 7, 8, 12]:
            factors.append({
                "factor": f"Mangal Dosha — Mars in {ph}th house",
                "effect": "Partner matching advised; possible temperament clashes",
                "severity": "Moderate" if ph == 7 else "Low",
            })

    # 7th lord debilitated
    if seventh_lord in grahas:
        sl_rashi = RASHI_NAMES.index(grahas[seventh_lord]["rashi"]) if grahas[seventh_lord]["rashi"] in RASHI_NAMES else -1
        if sl_rashi >= 0 and _is_debilitated(seventh_lord, sl_rashi):
            factors.append({
                "factor": f"7th lord ({seventh_lord}) debilitated",
                "effect": "Challenges in marital life; requires careful partner selection",
                "severity": "High",
            })

        # 7th lord in dusthana (6/8/12)
        sl_house = grahas[seventh_lord]["house"]
        if sl_house in [6, 8, 12]:
            factors.append({
                "factor": f"7th lord in {sl_house}th house (dusthana)",
                "effect": "Obstacles in marriage path; delayed or troubled initial approach",
                "severity": "Moderate",
            })

    # Venus combust
    if grahas.get("Shukra", {}).get("combust"):
        factors.append({
            "factor": "Venus (Shukra) combust by Sun",
            "effect": "Delayed love life; emotional sensitivity in relationships",
            "severity": "Low",
        })

    return factors

def _analyze_spouse_profile(grahas: dict, seventh_sign: str, seventh_lord: str,
                              navamsa: dict, gender: str, lagna_rashi_i: int) -> dict:
    """
    Derive spouse characteristics from classical rules.
    """
    # Planets in 7th house
    planets_in_7th = [name for name, g in grahas.items() if g["house"] == 7]

    # Primary karaka based on gender
    primary_karaka = "Shukra" if gender == "male" else "Guru"
    karaka_data = grahas.get(primary_karaka, {})

    # Navamsa 7th house
    nav_lagna_rashi = -1
    if navamsa:
        # navamsa lagna is the house 1 planet (or we derive from navamsa houses data)
        # navamsa dict has: {planet: {rashi, house, ...}}
        # Find lagna of navamsa by finding which rashi is house 1 in navamsa
        for pname, pd in navamsa.items():
            # We can't directly get navamsa lagna from planet dict easily
            # Use Guru or Shukra in navamsa for spouse
            pass

    nav_7th_sign = ""
    nav_7th_en   = ""
    if navamsa and primary_karaka in navamsa:
        nav_pk_rashi = navamsa[primary_karaka].get("rashi", "")
        nav_pk_rashi_en = RASHI_EN.get(nav_pk_rashi, nav_pk_rashi)
        nav_pk_status = navamsa[primary_karaka].get("status", "Normal")
        nav_7th_sign = nav_pk_rashi
        nav_7th_en   = f"{nav_pk_rashi_en} — {nav_pk_status} ({primary_karaka} in Navamsa)"

    # Base traits from 7th house sign
    base_traits = RASHI_7TH_TRAITS.get(seventh_sign, [])

    # Additional traits from planet(s) in 7th house
    additional_traits = []
    profession_hints = []
    planet_notes = []
    for p in planets_in_7th:
        meta = PLANET_IN_7TH_TRAITS.get(p, {})
        additional_traits.extend(meta.get("traits", []))
        if meta.get("profession"):
            profession_hints.append(meta["profession"])
        if meta.get("nature"):
            planet_notes.append(meta["nature"])

    # Karaka in its own sign or exalted → strong placement
    karaka_strength = ""
    if karaka_data:
        k_rashi_idx = RASHI_NAMES.index(karaka_data["rashi"]) if karaka_data.get("rashi") in RASHI_NAMES else -1
        if k_rashi_idx >= 0:
            if _is_exalted(primary_karaka, k_rashi_idx):
                karaka_strength = f"{primary_karaka} exalted — highly beneficial for marriage"
            elif RASHI_LORDS.get(k_rashi_idx) == primary_karaka:
                karaka_strength = f"{primary_karaka} in own sign — strong marriage promise"
            elif _is_debilitated(primary_karaka, k_rashi_idx):
                karaka_strength = f"{primary_karaka} debilitated — delayed or challenged love life"

    # Merge traits (deduplicate)
    all_traits = list(dict.fromkeys(base_traits + additional_traits))[:6]

    return {
        "7th_house_sign":  seventh_sign,
        "7th_house_sign_en": RASHI_EN.get(seventh_sign, seventh_sign),
        "7th_lord":        seventh_lord,
        "7th_lord_house":  grahas.get(seventh_lord, {}).get("house", "—"),
        "7th_lord_sign":   grahas.get(seventh_lord, {}).get("rashi", "—"),
        "planets_in_7th":  planets_in_7th if planets_in_7th else ["None"],
        "traits":          all_traits,
        "profession_hint": profession_hints[0] if profession_hints else f"Based on {seventh_sign} 7th house",
        "planet_notes":    planet_notes,
        "direction":       DIRECTION_MAP.get(seventh_sign, "West"),
        "karaka_strength": karaka_strength,
        "navamsa_karaka":  nav_7th_en or f"{primary_karaka} in Navamsa — refines spouse details",
    }

def _check_marriage_yogas(grahas: dict, lagna_rashi_i: int, seventh_lord: str) -> list:
    """
    Check classical marriage yoga combinations.
    """
    yogas = []
    seventh_house = 7  # house number

    shukra = grahas.get("Shukra", {})
    guru   = grahas.get("Guru", {})
    mangal = grahas.get("Mangal", {})
    shani  = grahas.get("Shani", {})
    rahu   = grahas.get("Rahu", {})
    ketu   = grahas.get("Ketu", {})

    sh_house = shukra.get("house", 0)
    gu_house = guru.get("house", 0)
    ma_house = mangal.get("house", 0)
    sa_house = shani.get("house", 0)
    ra_house = rahu.get("house", 0)
    ke_house = ketu.get("house", 0)

    seventh_lord_house = grahas.get(seventh_lord, {}).get("house", 0)

    # 1. Kalatra Yoga — 7th lord + Venus in strong mutual relationship
    if seventh_lord in grahas and seventh_lord == "Shukra":
        yogas.append({
            "name": "Kalatra Yoga",
            "present": True,
            "description": "7th lord is Venus — very strong marriage yoga. Spouse will be charming and the marriage bond will be powerful.",
        })
    elif seventh_lord in grahas:
        sl_house = grahas[seventh_lord]["house"]
        # Check if 7th lord and Venus are in 1/7 or same house
        if sh_house == sl_house or (sh_house + sl_house in [8, 14]):
            yogas.append({
                "name": "Kalatra Yoga",
                "present": True,
                "description": f"7th lord ({seventh_lord}) and Venus conjoined or in mutual 7th — strong marriage promise.",
            })
        else:
            yogas.append({"name": "Kalatra Yoga", "present": False,
                          "description": "7th lord and Venus not in strong mutual relationship."})
    else:
        yogas.append({"name": "Kalatra Yoga", "present": False,
                      "description": "7th lord placement unclear."})

    # 2. Daara Karaka Yoga — Venus in 7th
    if sh_house == 7:
        yogas.append({
            "name": "Daara Karaka Yoga",
            "present": True,
            "description": "Venus in 7th house — beautiful, artistic, and loving spouse. Excellent for marriage happiness.",
        })
    else:
        yogas.append({"name": "Daara Karaka Yoga", "present": False,
                      "description": "Venus not placed in 7th house."})

    # 3. Vivah Yoga — Jupiter aspects 7th house
    if gu_house > 0 and _does_planet_aspect_house("Guru", gu_house, 7):
        yogas.append({
            "name": "Vivah Yoga",
            "present": True,
            "description": f"Jupiter aspects 7th house from {gu_house}th — auspicious, educated and noble spouse. Marriage will be blessed.",
        })
    else:
        yogas.append({"name": "Vivah Yoga", "present": False,
                      "description": "Jupiter does not aspect 7th house."})

    # 4. Manglik Yoga (Kuja Dosha)
    if ma_house in [1, 4, 7, 8, 12]:
        yogas.append({
            "name": "Manglik (Kuja Dosha)",
            "present": True,
            "description": f"Mars in {ma_house}th house — Manglik yoga present. Partner matching (Kundali Milan) is strongly recommended. Two Manglik individuals cancel the dosha.",
        })
    else:
        yogas.append({"name": "Manglik (Kuja Dosha)", "present": False,
                      "description": "Mars not in Manglik positions. No Kuja Dosha."})

    # 5. Guru Chandala in 7th (Rahu+Jupiter together in 7th)
    if ra_house == 7 and gu_house == 7:
        yogas.append({
            "name": "Guru Chandala in 7th",
            "present": True,
            "description": "Rahu and Jupiter together in 7th — unconventional marriage. Possible cross-cultural or inter-caste union.",
        })
    else:
        yogas.append({"name": "Guru Chandala in 7th", "present": False,
                      "description": "No Rahu-Jupiter conjunction in 7th."})

    # 6. Chandra in 7th — emotional, caring spouse
    chandra_house = grahas.get("Chandra", {}).get("house", 0)
    if chandra_house == 7:
        yogas.append({
            "name": "Chandra in 7th",
            "present": True,
            "description": "Moon in 7th — emotionally sensitive and caring spouse. Strong emotional bond in marriage.",
        })
    else:
        yogas.append({"name": "Chandra in 7th", "present": False,
                      "description": "Moon not in 7th house."})

    # 7. 7th lord in own house or exalted → Jagannadha Yoga
    if seventh_lord in grahas:
        sl_ri = RASHI_NAMES.index(grahas[seventh_lord]["rashi"]) if grahas[seventh_lord]["rashi"] in RASHI_NAMES else -1
        if sl_ri >= 0 and (RASHI_LORDS.get(sl_ri) == seventh_lord or _is_exalted(seventh_lord, sl_ri)):
            yogas.append({
                "name": "Jagannadha Yoga",
                "present": True,
                "description": f"7th lord ({seventh_lord}) is strong (own/exalted) — happy and prosperous married life.",
            })

    return yogas

def _estimate_age_range(delay_factors: list, timing_windows: dict, birth_year: int) -> str:
    """Estimate probable marriage age range based on delay factors and dasha windows."""
    today_year = date.today().year
    current_age = today_year - birth_year

    high_delay = any(f["severity"] == "High" for f in delay_factors)
    moderate_delay = any(f["severity"] == "Moderate" for f in delay_factors)

    # Get earliest favorable window
    windows = timing_windows.get("favorable_windows", [])
    earliest_window_year = None
    for w in windows:
        try:
            wy = int(w["start"][:4])
            if wy >= today_year:
                earliest_window_year = wy
                break
        except Exception:
            pass

    if earliest_window_year:
        est_age = earliest_window_year - birth_year
        if high_delay:
            est_age = max(est_age, 30)
        elif moderate_delay:
            est_age = max(est_age, 27)
        return f"{est_age}–{est_age + 3}"
    else:
        if high_delay:    return "30–36"
        if moderate_delay: return "27–32"
        return "24–28"


def _gemini_narrative(name: str, lagna: str, seventh_sign: str, seventh_lord: str,
                       timing: dict, spouse: dict, yogas: list, delay_factors: list,
                       gender: str) -> str:
    """Generate Gemini AI narrative for marriage analysis."""
    try:
        import google.genai as genai
        api_key = os.environ.get("GEMINI_API_KEY", "")
        if not api_key:
            return ""
        client = genai.Client(api_key=api_key)

        present_yogas = [y["name"] for y in yogas if y.get("present")]
        delay_names   = [f["factor"] for f in delay_factors]

        current = timing.get("current_period") or {}
        current_str = f"{current.get('period','—')} (score: {current.get('score','—')}, probability: {current.get('probability','—')})" if current else "Not currently in a marriage Dasha"

        favorable = timing.get("favorable_windows", [])
        fav_str = "; ".join([f"{w['period']} ({w['start'][:7]}–{w['end'][:7]}, {w['probability']})" for w in favorable[:3]])

        prompt = f"""You are a Vedic astrology expert with deep knowledge of Parashari Jyotish, Brihat Parashara Hora Shastra, and Varahamihira's Brihat Jataka.

Analyze the marriage prospects for this person based on their birth chart:

- Name: {name or 'the native'}
- Gender: {gender}
- Lagna (Ascendant): {lagna} → {RASHI_EN.get(lagna, lagna)}
- 7th house: {seventh_sign} → {RASHI_EN.get(seventh_sign, seventh_sign)}
- 7th lord: {seventh_lord}
- 7th lord placed in: {spouse.get('7th_lord_house', '—')}th house

Current Dasha: {current_str}
Favorable marriage windows: {fav_str or 'None identified in current cycle'}
Estimated age range: {timing.get('estimated_age_range', '—')}

Active marriage yogas: {', '.join(present_yogas) if present_yogas else 'None significant'}
Delay/challenge factors: {', '.join(delay_names) if delay_names else 'None significant'}

Spouse profile hints: {', '.join(spouse.get('traits', [])[:4])}

Write a detailed, accurate Vedic astrology marriage analysis covering:
1. Overall marriage prospects (favorable/challenging/mixed)
2. Why the current or next period is/isn't favorable (dasha reasoning)
3. Key factors shaping the spouse's personality
4. Any special advice (Kundali matching, mantra, remedies if applicable)

Rules:
- 250–350 words
- Reference actual Parashari/Jyotish principles (name the rules/yogas)
- Be direct and specific, not vague or generic
- Use clear English; no excessive Sanskrit without explanation
- End with a one-line summary verdict"""

        response = client.models.generate_content(
            model="models/gemini-2.5-flash",
            contents=prompt,
        )
        return response.text.strip() if response.text else ""
    except Exception:
        return ""


# ── Main endpoint ─────────────────────────────────────────────────────────────

@router.post("/marriage")
def analyze_marriage(req: MarriageRequest):
    try:
        dt = datetime.strptime(f"{req.date} {req.time}", "%Y-%m-%d %H:%M")
    except ValueError as e:
        return {"status": "error", "detail": f"Invalid date/time format: {e}"}

    # ── 1. Calculate kundali with antardasha ──────────────────────────────────
    data, err = calc_kundali(
        year=dt.year, month=dt.month, day=dt.day,
        hour=dt.hour, minute=dt.minute,
        lat=req.lat, lon=req.lon, tz=req.tz,
        name=req.name, place=req.place,
        calc_options=["antardasha"],
    )
    if err:
        return {"status": "error", "detail": err}

    grahas  = data["grahas"]       # {planet: {rashi, house, degree, ...}}
    dashas  = data.get("dashas") or data.get("dasha", [])  # [{lord, start, end, antardashas: [...]}]
    lagna   = data["lagna"]["rashi"]
    navamsa = data.get("navamsha", {})  # D9 chart

    lagna_rashi_i = RASHI_NAMES.index(lagna) if lagna in RASHI_NAMES else 0

    # ── 2. Key house lords ────────────────────────────────────────────────────
    seventh_rashi_i  = _get_7th_house(lagna_rashi_i)
    fifth_rashi_i    = (lagna_rashi_i + 4) % 12
    eleventh_rashi_i = (lagna_rashi_i + 10) % 12
    second_rashi_i   = (lagna_rashi_i + 1) % 12

    seventh_lord  = RASHI_LORDS.get(seventh_rashi_i, "Shukra")
    fifth_lord    = RASHI_LORDS.get(fifth_rashi_i, "Guru")
    eleventh_lord = RASHI_LORDS.get(eleventh_rashi_i, "Shani")
    second_lord   = RASHI_LORDS.get(second_rashi_i, "Shukra")

    seventh_sign  = RASHI_NAMES[seventh_rashi_i]

    # ── 3. Score dasha windows ────────────────────────────────────────────────
    timing = _score_windows(
        dashas, seventh_lord, fifth_lord, eleventh_lord, second_lord,
        grahas, 7, lagna_rashi_i, dt.year,
    )

    # ── 4. Delay factors ──────────────────────────────────────────────────────
    delay_factors = _evaluate_delay_factors(grahas, 7, seventh_lord, lagna_rashi_i)

    # ── 5. Age estimate ───────────────────────────────────────────────────────
    timing["estimated_age_range"] = _estimate_age_range(delay_factors, timing, dt.year)

    # ── 6. Probability score (from current or best window) ───────────────────
    current  = timing.get("current_period")
    best_prob = current["probability"] if current else (
        timing["favorable_windows"][0]["probability"] if timing["favorable_windows"] else "Moderate"
    )
    timing["overall_probability_pct"] = _prob_to_pct(best_prob)
    timing["overall_probability"]     = best_prob

    # ── 7. Spouse profile ─────────────────────────────────────────────────────
    spouse = _analyze_spouse_profile(
        grahas, seventh_sign, seventh_lord, navamsa, req.gender, lagna_rashi_i
    )

    # ── 8. Marriage yogas ─────────────────────────────────────────────────────
    yogas = _check_marriage_yogas(grahas, lagna_rashi_i, seventh_lord)

    # ── 9. Chart summary ──────────────────────────────────────────────────────
    chart_summary = {
        "lagna":       lagna,
        "lagna_en":    RASHI_EN.get(lagna, lagna),
        "7th_house":   seventh_sign,
        "7th_house_en": RASHI_EN.get(seventh_sign, seventh_sign),
        "7th_lord":    seventh_lord,
        "venus_placement": f"{grahas.get('Shukra', {}).get('house', '—')}th house ({grahas.get('Shukra', {}).get('rashi', '—')})",
        "jupiter_placement": f"{grahas.get('Guru', {}).get('house', '—')}th house ({grahas.get('Guru', {}).get('rashi', '—')})",
    }

    # ── 10. Gemini narrative ──────────────────────────────────────────────────
    ai_analysis = _gemini_narrative(
        req.name, lagna, seventh_sign, seventh_lord,
        timing, spouse, yogas, delay_factors, req.gender
    )

    return {
        "status":         "ok",
        "marriage_timing": timing,
        "delay_factors":  delay_factors,
        "marriage_yogas": yogas,
        "spouse_profile": spouse,
        "chart_summary":  chart_summary,
        "ai_analysis":    ai_analysis,
    }
