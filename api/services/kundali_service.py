"""
Kundali calculation service.
Extracted from scripts/08_gradio_kundali.py — single source of truth.
Supports: ayanamsha selection, true/mean Rahu, antardasha, raw longitudes,
          graha relationships, house significations.
"""
from datetime import datetime, timedelta
from typing import Tuple, Optional

try:
    import swisseph as swe
    SWE_AVAILABLE = True
except ImportError:
    SWE_AVAILABLE = False

from api.config import (
    EPHE_PATH, RASHI_NAMES, NAKSHATRAS, NAKSHATRA_LORDS,
    DASHA_YEARS, DASHA_SEQ, RASHI_LORDS, EXALTATION, DEBILITATION,
    GRAHA_HINDI, RASHI_HINDI, NAKSHATRA_HINDI,
    AYANAMSHA_MODES, GRAHA_EN, RASHI_EN,
    GRAHA_FRIENDS, GRAHA_ENEMIES, GRAHA_KARAKA,
    HOUSE_SIGNIFICATIONS,
)
from api.services.divisional_charts import calc_varga_chart, VARGA_NAMES

# ── Nakshatra characteristic tables ─────────────────────────────────
_NAK_NADI = ["Aadi","Madhya","Antya"] * 9  # repeating pattern for 27 nakshatras
_NAK_GANA = [
    "Deva","Manushya","Rakshasa","Manushya","Deva","Manushya","Deva","Deva","Rakshasa",
    "Rakshasa","Manushya","Manushya","Deva","Rakshasa","Deva","Rakshasa","Deva","Rakshasa",
    "Rakshasa","Manushya","Manushya","Deva","Rakshasa","Rakshasa","Manushya","Manushya","Deva",
]
_NAK_YONI = [
    "Horse","Elephant","Sheep","Snake","Snake","Dog","Cat","Sheep","Cat",
    "Rat","Rat","Cow","Buffalo","Tiger","Buffalo","Tiger","Deer","Deer",
    "Dog","Monkey","Mongoose","Monkey","Lion","Horse","Lion","Cow","Elephant",
]
_NAK_VARNA = [
    "Vaishya","Shudra","Brahmin","Shudra","Vaishya","Shudra","Brahmin","Kshatriya","Shudra",
    "Kshatriya","Brahmin","Kshatriya","Vaishya","Shudra","Vaishya","Shudra","Shudra","Brahmin",
    "Kshatriya","Brahmin","Kshatriya","Shudra","Vaishya","Shudra","Brahmin","Kshatriya","Brahmin",
]
_NAK_PAYA_BY_LORD = {
    "Surya":"Silver","Chandra":"Silver","Mangal":"Gold",
    "Budh":"Silver","Guru":"Silver","Shukra":"Gold",
    "Shani":"Iron","Rahu":"Iron","Ketu":"Copper",
}
_RASHI_PAYA     = ["Gold","Silver","Gold","Silver","Gold","Silver","Gold","Silver","Gold","Silver","Gold","Silver"]
_RASHI_GENDER   = ["Mas","Fem","Mas","Fem","Mas","Fem","Mas","Fem","Mas","Fem","Mas","Fem"]
_RASHI_MODALITY = ["Movable","Fixed","Common","Movable","Fixed","Common",
                   "Movable","Fixed","Common","Movable","Fixed","Common"]
_RASHI_TATTVA   = ["Fire","Earth","Air","Water","Fire","Earth","Air","Water","Fire","Earth","Air","Water"]
_RASHI_VASHYA   = [
    "Chatushpada","Chatushpada","Manava","Jalachar","Vanachara","Manava",
    "Manava","Keeta","Chatushpada","Jalachar","Manava","Jalachar",
]
_YOGA_NAMES = [
    "Vishkumbha","Preeti","Ayushman","Saubhagya","Shobhana","Atiganda","Sukarman",
    "Dhriti","Shoola","Ganda","Vriddhi","Dhruva","Vyaghata","Harshana","Vajra",
    "Siddhi","Vyatipata","Variyan","Parigha","Shiva","Siddha","Sadhya","Shubha",
    "Shukla","Brahma","Aindra","Vaidhriti",
]
_KARANA_FIXED  = ["Kimstughna","Shakuni","Chatushpada","Naga"]
_KARANA_MOVABLE = ["Bava","Balava","Kaulava","Taitila","Garaja","Vanija","Vishti"]
_TITHI_NAMES   = [
    "Pratipada","Dwitiya","Tritiya","Chaturthi","Panchami",
    "Shashthi","Saptami","Ashtami","Navami","Dashami",
    "Ekadashi","Dwadashi","Trayodashi","Chaturdashi","Purnima",
]
_VARA_NAMES    = ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"]
_COMBUST_ORB   = {"Chandra":12,"Mangal":17,"Budh":14,"Guru":11,"Shukra":10,"Shani":15}
_TARA_NAMES    = ["Janma","Sampat","Vipat","Kshema","Pratyak","Sadhana","Naidhana","Mitra","Parama Mitra"]

# ── Yoga remedies (mantra / gemstone / deity / remedy) ──────────────────────
YOGA_REMEDIES = {
    "Ruchaka Yoga":      {"mantra": "Om Angarakaya Namah (108× Tuesday)", "gemstone": "Red Coral (Moonga)", "deity": "Kartikeya", "remedy": "Donate red lentils and jaggery on Tuesdays"},
    "Bhadra Yoga":       {"mantra": "Om Budhaya Namah (108× Wednesday)", "gemstone": "Emerald (Panna)", "deity": "Lord Vishnu", "remedy": "Donate green cloth and moong dal on Wednesdays"},
    "Hamsa Yoga":        {"mantra": "Om Gurave Namah (108× Thursday)", "gemstone": "Yellow Sapphire (Pukhraj)", "deity": "Brihaspati / Dakshinamurti", "remedy": "Donate turmeric, yellow sweets and books on Thursdays"},
    "Malavya Yoga":      {"mantra": "Om Shukraya Namah (108× Friday)", "gemstone": "Diamond / White Sapphire", "deity": "Goddess Lakshmi", "remedy": "Offer white flowers and rice to Lakshmi on Fridays"},
    "Shasha Yoga":       {"mantra": "Om Shanicharaya Namah (108× Saturday)", "gemstone": "Blue Sapphire (Neelam)", "deity": "Shani Dev / Bhairava", "remedy": "Donate sesame oil and black cloth on Saturdays"},
    "Budhaditya Yoga":   {"mantra": "Om Hreem Suryaya Budhaya Namah", "gemstone": "Ruby + Emerald (consult astrologer)", "deity": "Surya-Budha / Lord Vishnu", "remedy": "Offer water to Sun at sunrise on Sundays and Wednesdays"},
    "Gajakesari Yoga":   {"mantra": "Om Gurave Namah + Om Som Somaya Namah", "gemstone": "Yellow Sapphire + Pearl", "deity": "Brihaspati / Lord Ganesha", "remedy": "Donate yellow sweets on Thursdays; fast on Mondays"},
    "Raj Yoga":          {"mantra": "Om Namo Bhagavate Vasudevaya (108× daily)", "gemstone": "As per kendra-trikona lords", "deity": "Lord Vishnu / Indra", "remedy": "Recite Vishnu Sahasranama on Ekadashi; donate to temples"},
    "Dhana Yoga":        {"mantra": "Om Shreem Maha Lakshmiyei Namah + Om Kuberaya Namah", "gemstone": "Per 2nd and 11th lords", "deity": "Kubera / Lakshmi", "remedy": "Keep a Kubera Yantra at home; donate food on Thursdays"},
    "Kala Sarpa Yoga":   {"mantra": "Om Namah Shivaya / Maha Mrityunjaya", "gemstone": "Hessonite (Gomed) + Cat's Eye", "deity": "Lord Shiva / Naga Devata", "remedy": "Perform Kala Sarpa Shanti Puja; offer milk to Shiva Lingam on Mondays; Naga Panchami puja"},
    "Mangal Dosha":      {"mantra": "Om Angarakaya Namah (108× Tuesday)", "gemstone": "Red Coral (if Mangal benefic for lagna)", "deity": "Kartikeya / Hanuman", "remedy": "Kuja Dosha Nivaran Puja; Hanuman Chalisa on Tuesdays; marry a Manglik or perform Kumbh Vivah"},
    "Kemadruma Yoga":    {"mantra": "Om Som Somaya Namah (108× Monday)", "gemstone": "Pearl (Moti)", "deity": "Lord Shiva / Chandra Dev", "remedy": "Observe Monday fasts; offer milk and white flowers to Shiva; keep silver at home"},
    "Lakshmi Yoga":      {"mantra": "Om Shrim Maha Lakshmiyei Namah (108× Friday)", "gemstone": "Diamond or White Sapphire", "deity": "Goddess Lakshmi", "remedy": "Worship Lakshmi with lotus flowers on Fridays; light a ghee lamp"},
    "Saraswati Yoga":    {"mantra": "Om Aim Saraswatyei Namah (108× daily)", "gemstone": "Emerald (Panna)", "deity": "Goddess Saraswati", "remedy": "Worship Saraswati on Panchami; donate books and pens to students"},
    "Chatussagara Yoga": {"mantra": "Om Namo Narayanaya", "gemstone": "Per lagna lord", "deity": "Lord Vishnu", "remedy": "Donate to charity on auspicious days; visit all four dhams"},
    "Vipareeta Raja Yoga":{"mantra": "Om Namah Shivaya — Maha Mrityunjaya", "gemstone": "Per yoga-forming planet", "deity": "Lord Shiva / Kali", "remedy": "Worship on the day of the yoga planet; Durga Saptashati recitation"},
    "Sunapha Yoga":      {"mantra": "Per yoga-forming planet", "gemstone": "Per yoga-forming planet", "deity": "Per yoga planet", "remedy": "Strengthen the yoga-forming planet through its specific day remedy"},
    "Anapha Yoga":       {"mantra": "Per yoga-forming planet", "gemstone": "Per yoga-forming planet", "deity": "Per yoga planet", "remedy": "Donate on the day of the yoga-forming planet"},
    "Parvata Yoga":      {"mantra": "Om Namo Bhagavate Vasudevaya", "gemstone": "Per lagna lord", "deity": "Lord Vishnu", "remedy": "Donate on benefic days (Thursday, Friday)"},
    "Adhi Yoga":         {"mantra": "Om Aim Hreem Shreem (Trimurti mantra)", "gemstone": "Per yoga planets (Budh/Guru/Shukra)", "deity": "Trimurti (Brahma-Vishnu-Shiva)", "remedy": "Strengthen Budh, Guru, Shukra — offer on their respective days"},
    "Mahabhagya Yoga":   {"mantra": "Om Surya Chandra Lagneshu Namah", "gemstone": "Per lagna lord", "deity": "Surya / Chandra", "remedy": "Observe Ekadashi fasts; donate on Sun/Moon days"},
    "Chandra-Mangal Yoga":{"mantra": "Om Chandraya Namah + Om Angarakaya Namah", "gemstone": "Pearl + Red Coral", "deity": "Chandra Dev + Kartikeya", "remedy": "Worship on Mondays and Tuesdays; offer white and red flowers"},
    "Shakata Yoga":      {"mantra": "Om Gurave Namah + Om Som Somaya Namah", "gemstone": "Yellow Sapphire + Pearl", "deity": "Brihaspati / Chandra", "remedy": "Thursday Jupiter puja; fast on Mondays to strengthen Moon"},
    "Amala Yoga":        {"mantra": "Om Namo Bhagavate Vasudevaya", "gemstone": "Per benefic in 10th", "deity": "Lord Vishnu / Lakshmi", "remedy": "Maintain ethical conduct; donate on the 10th-house planet's day"},
}
_DEFAULT_REMEDY = {
    "mantra": "Om Namah Shivaya (universal remedy)",
    "gemstone": "Consult astrologer for your lagna",
    "deity": "Ishta Devata (your chosen deity)",
    "remedy": "Maintain dharmic living; offer to your lagna lord deity",
}


_GRAHAS_MAP_MEAN = None
_GRAHAS_MAP_TRUE = None


def _init_grahas():
    global _GRAHAS_MAP_MEAN, _GRAHAS_MAP_TRUE
    if not SWE_AVAILABLE:
        return
    if _GRAHAS_MAP_MEAN is None:
        _GRAHAS_MAP_MEAN = {
            swe.SUN: "Surya", swe.MOON: "Chandra", swe.MARS: "Mangal",
            swe.MERCURY: "Budh", swe.JUPITER: "Guru", swe.VENUS: "Shukra",
            swe.SATURN: "Shani", swe.MEAN_NODE: "Rahu",
        }
    if _GRAHAS_MAP_TRUE is None:
        _GRAHAS_MAP_TRUE = {
            swe.SUN: "Surya", swe.MOON: "Chandra", swe.MARS: "Mangal",
            swe.MERCURY: "Budh", swe.JUPITER: "Guru", swe.VENUS: "Shukra",
            swe.SATURN: "Shani", swe.TRUE_NODE: "Rahu",
        }


def _graha_relationship(gname: str, rashi_i: int) -> str:
    """Determine graha's relationship with the sign lord (Friend/Neutral/Enemy)."""
    if gname in ("Rahu", "Ketu"):
        return "Neutral"
    sign_lord = RASHI_LORDS.get(rashi_i, "")
    if sign_lord == gname:
        return "Own"
    if sign_lord in GRAHA_FRIENDS.get(gname, set()):
        return "Friend"
    if sign_lord in GRAHA_ENEMIES.get(gname, set()):
        return "Enemy"
    return "Neutral"


def _calc_pratyantardashas(antar_lord: str, antar_start: datetime, antar_years: float,
                            include_sukshma: bool = False) -> list:
    """Calculate Pratyantar Dashas (3rd level) within an Antardasha."""
    start_idx = DASHA_SEQ.index(antar_lord)
    pratyantars = []
    cur = antar_start
    for i in range(9):
        pl = DASHA_SEQ[(start_idx + i) % 9]
        prat_years = antar_years * DASHA_YEARS[pl] / 120
        prat_days = prat_years * 365.25
        end = cur + timedelta(days=prat_days)
        entry = {
            "lord": pl,
            "days": round(prat_days),
            "start": cur.strftime("%Y-%m-%d"),
            "end": end.strftime("%Y-%m-%d"),
        }
        if include_sukshma:
            entry["sukshmadashas"] = _calc_sukshma_dashas(pl, cur, prat_years)
        pratyantars.append(entry)
        cur = end
    return pratyantars


def _calc_sukshma_dashas(pratyantar_lord: str, pratyantar_start: datetime, pratyantar_years: float) -> list:
    """Calculate Sukshma Dashas (4th level) within a Pratyantar Dasha."""
    start_idx = DASHA_SEQ.index(pratyantar_lord)
    sukshmas = []
    cur = pratyantar_start
    for i in range(9):
        pl = DASHA_SEQ[(start_idx + i) % 9]
        days = (pratyantar_years * DASHA_YEARS[pl] / 120) * 365.25
        end = cur + timedelta(days=days)
        sukshmas.append({
            "lord": pl,
            "hours": round(days * 24),
            "start": cur.strftime("%Y-%m-%d %H:%M"),
            "end": end.strftime("%Y-%m-%d %H:%M"),
        })
        cur = end
    return sukshmas


def _is_combust(gname: str, g_lon: float, sun_lon: float, g_retro: bool) -> bool:
    """Check if a planet is combust (too close to Sun)."""
    if gname in ("Surya", "Rahu", "Ketu"):
        return False
    orb = _COMBUST_ORB.get(gname, 15)
    if gname == "Budh" and g_retro:
        orb = 12
    elif gname == "Shukra" and g_retro:
        orb = 8
    diff = abs((g_lon - sun_lon + 180) % 360 - 180)
    return diff <= orb


def _calc_bhav_chalit(jd: float, lat: float, lon: float, ayanamsha: float, grahas: dict) -> dict:
    """Calculate Bhav Chalit positions using Placidus house cusps (sidereal).
    Falls back to Sripati if Placidus is unavailable."""
    try:
        try:
            cusps_trop, _ = swe.houses_ex(jd, lat, lon, b'P')  # Placidus
        except Exception:
            cusps_trop, _ = swe.houses_ex(jd, lat, lon, b'S')  # Sripati fallback
        cusps_sid = [(c - ayanamsha) % 360 for c in cusps_trop[:12]]
        planet_chalit = {}
        for gname, g in grahas.items():
            plon = g["longitude"]
            assigned = False
            for i in range(12):
                s = cusps_sid[i]
                e = cusps_sid[(i + 1) % 12]
                if e > s:
                    in_h = s <= plon < e
                else:
                    in_h = plon >= s or plon < e
                if in_h:
                    planet_chalit[gname] = i + 1
                    assigned = True
                    break
            if not assigned:
                planet_chalit[gname] = 1
        return {"cusps_sid": [round(c, 4) for c in cusps_sid], "planets": planet_chalit}
    except Exception:
        return {}


def _calc_birth_panchang(jd: float, sun_lon: float, moon_lon: float,
                          lat: float, lon_e: float, tz: float,
                          moon_nak_i: int = 0, sun_nak_i: int = 0) -> dict:
    """Calculate birth-moment Panchang: Tithi, Yoga, Karana, Vara, Sunrise, Sunset."""
    diff = (moon_lon - sun_lon) % 360

    # Tithi
    tithi_num = int(diff / 12) + 1  # 1-30
    paksha    = "Shukla" if tithi_num <= 15 else "Krishna"
    tithi_name = _TITHI_NAMES[min((tithi_num - 1) % 15, 14)]

    # 27 Yoga
    yoga_lon  = (sun_lon + moon_lon) % 360
    yoga_name = _YOGA_NAMES[int(yoga_lon / (360 / 27)) % 27]

    # Karana
    karana_num = int(diff / 6)  # 0-59
    if karana_num == 0:
        karana = _KARANA_FIXED[0]
    elif karana_num >= 57:
        karana = _KARANA_FIXED[min(karana_num - 56, 3)]
    else:
        karana = _KARANA_MOVABLE[(karana_num - 1) % 7]

    # Vara (weekday)
    vara = _VARA_NAMES[int((jd + 1.5) % 7)]

    # Sunrise / Sunset via pyswisseph
    geo     = (lon_e, lat, 0)
    sunrise = sunset = "N/A"
    try:
        def _jd_hhmm(t):
            h_utc = (t % 1) * 24
            h_loc = (h_utc + tz) % 24
            return f"{int(h_loc):02d}:{int((h_loc % 1) * 60):02d}"
        _, t_r = swe.rise_trans(jd - 0.5, swe.SUN, b"", swe.BIT_DISC_CENTER, geo, 0, 0, swe.CALC_RISE)
        _, t_s = swe.rise_trans(jd - 0.5, swe.SUN, b"", swe.BIT_DISC_CENTER, geo, 0, 0, swe.CALC_SET)
        if t_r: sunrise = _jd_hhmm(t_r[0])
        if t_s: sunset  = _jd_hhmm(t_s[0])
    except Exception:
        pass

    moon_rashi_i = int(moon_lon / 30)
    sun_rashi_i  = int(sun_lon / 30)
    return {
        "tithi": tithi_name, "tithi_num": tithi_num, "paksha": paksha,
        "yoga": yoga_name, "karana": karana, "vara": vara,
        "sunrise": sunrise, "sunset": sunset,
        "moonsign": RASHI_NAMES[moon_rashi_i],
        "sunsign":  RASHI_NAMES[sun_rashi_i],
        "moon_nakshatra": NAKSHATRAS[moon_nak_i] if moon_nak_i < 27 else "",
        "surya_nakshatra": NAKSHATRAS[sun_nak_i]  if sun_nak_i  < 27 else "",
    }


def _personal_chars(moon_nak_i: int, moon_rashi_i: int, lagna_rashi_i: int, nak_lord: str) -> dict:
    """Personal characteristics from birth nakshatra and rashi."""
    return {
        "nadi":           _NAK_NADI[moon_nak_i],
        "gana":           _NAK_GANA[moon_nak_i],
        "yoni":           _NAK_YONI[moon_nak_i],
        "varna":          _NAK_VARNA[moon_nak_i],
        "tattva":         _RASHI_TATTVA[moon_rashi_i],
        "vashya":         _RASHI_VASHYA[moon_rashi_i],
        "nakshatra_paya": _NAK_PAYA_BY_LORD.get(nak_lord, "Silver"),
        "rashi_paya":     _RASHI_PAYA[lagna_rashi_i],
        "yunja":          "Adya" if moon_nak_i < 9 else ("Madhya" if moon_nak_i < 18 else "Antya"),
        "tara":           _TARA_NAMES[moon_nak_i % 9],
    }


def _detect_yogas(grahas: dict, houses: list, lagna_rashi_i: int) -> list:
    """Detect 35+ classical Vedic yogas."""
    yogas = []

    def r(n):   return grahas[n]["rashi"]
    def h(n):   return grahas[n]["house"]
    def is_own(n):     return RASHI_LORDS.get(r(n)) == n
    def is_exalted(n): return EXALTATION.get(n) == r(n)
    def is_kendra(n):  return h(n) in [1, 4, 7, 10]
    def is_trikona(n): return h(n) in [1, 5, 9]
    def is_strong(n):  return is_own(n) or is_exalted(n) or is_kendra(n) or is_trikona(n)

    BENEFICS = ["Guru", "Shukra", "Budh", "Chandra"]
    MALEFICS  = ["Surya", "Mangal", "Shani", "Rahu", "Ketu"]

    kendra_lords  = set()
    trikona_lords = set()
    for hse in houses:
        lord = RASHI_LORDS[hse["rashi"]]
        if hse["house"] in [1, 4, 7, 10]: kendra_lords.add(lord)
        if hse["house"] in [1, 5, 9]:     trikona_lords.add(lord)
    lagna_lord = RASHI_LORDS.get(lagna_rashi_i, "")

    def add(name, desc, strength, category, present=True):
        rem = YOGA_REMEDIES.get(name, _DEFAULT_REMEDY)
        yogas.append({"name": name, "desc": desc, "present": present,
                      "strength": strength, "category": category,
                      "mantra": rem["mantra"], "gemstone": rem["gemstone"],
                      "deity": rem["deity"], "remedy": rem["remedy"]})

    # ── Pancha Mahapurusha ────────────────────────────────────────────
    for g, (yn, yd, cat) in [
        ("Mangal", ("Ruchaka Yoga",  "Courage and leadership",   "Power")),
        ("Budh",   ("Bhadra Yoga",   "Intellect and commerce",   "Intellect")),
        ("Guru",   ("Hamsa Yoga",    "Spirituality and wisdom",  "Spiritual")),
        ("Shukra", ("Malavya Yoga",  "Beauty, luxury and arts",  "Wealth")),
        ("Shani",  ("Shasha Yoga",   "Power and discipline",     "Power")),
    ]:
        if is_kendra(g) and (is_own(g) or is_exalted(g)):
            add(yn, yd, "Very Strong", cat)

    # ── Budhaditya ────────────────────────────────────────────────────
    if r("Surya") == r("Budh"):
        add("Budhaditya Yoga", "Sun-Mercury conjunction — sharp intellect, eloquence", "Strong", "Intellect")

    # ── Gajakesari ────────────────────────────────────────────────────
    if (r("Guru") - r("Chandra")) % 12 in [0, 3, 6, 9]:
        add("Gajakesari Yoga", "Jupiter in kendra from Moon — wisdom, fame, prosperity", "Strong", "Power")

    # ── Sunapha / Anapha / Durudhara ─────────────────────────────────
    non_sun = ["Mangal", "Budh", "Guru", "Shukra", "Shani"]
    moon_r  = r("Chandra")
    in_2nd  = [g for g in non_sun if r(g) == (moon_r + 1) % 12]
    in_12th = [g for g in non_sun if r(g) == (moon_r - 1) % 12]
    if in_2nd and not in_12th:
        add("Sunapha Yoga", f"{', '.join(in_2nd)} in 2nd from Moon — resourcefulness, prosperity", "Strong", "Wealth")
    if in_12th and not in_2nd:
        add("Anapha Yoga", f"{', '.join(in_12th)} in 12th from Moon — virtuous, philanthropic", "Strong", "Wealth")
    if in_2nd and in_12th:
        add("Durudhara Yoga", "Planets both 2nd and 12th from Moon — resourceful and philanthropic", "Strong", "Wealth")

    # ── Veshi / Vasi / Ubhayachari ───────────────────────────────────
    non_moon = ["Mangal", "Budh", "Guru", "Shukra", "Shani"]
    sun_r   = r("Surya")
    in_2s   = [g for g in non_moon if r(g) == (sun_r + 1) % 12]
    in_12s  = [g for g in non_moon if r(g) == (sun_r - 1) % 12]
    if in_2s:
        add("Veshi Yoga", f"{', '.join(in_2s)} in 2nd from Sun — fortunate, helpful nature", "Moderate", "Character")
    if in_12s:
        add("Vasi Yoga", f"{', '.join(in_12s)} in 12th from Sun — virtuous, esteemed", "Moderate", "Character")
    if in_2s and in_12s:
        add("Ubhayachari Yoga", "Planets in both 2nd and 12th from Sun — royal speech and presence", "Strong", "Power")

    # ── Kemadruma ─────────────────────────────────────────────────────
    conj_moon = [g for g in non_sun if r(g) == moon_r]
    if not in_2nd and not in_12th and not conj_moon:
        add("Kemadruma Yoga", "Moon isolated — emotional challenges, self-reliance", "Applicable", "Adversity")

    # ── Kala Sarpa ────────────────────────────────────────────────────
    rahu_l = grahas["Rahu"]["longitude"]
    ketu_l = grahas["Ketu"]["longitude"]
    all_lons = [grahas[g]["longitude"] for g in ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani"]]
    def _btw(lon, r_l=rahu_l, k_l=ketu_l):
        if r_l > k_l: return k_l <= lon <= r_l
        return lon >= r_l or lon <= k_l
    if all(_btw(pl) for pl in all_lons):
        add("Kala Sarpa Yoga", "All planets hemmed between Rahu-Ketu — intense karmic life", "Applicable", "Karma")

    # ── Raj Yoga ──────────────────────────────────────────────────────
    ry = kendra_lords & trikona_lords
    if ry:
        add("Raj Yoga", f"Kendra-trikona lords ({', '.join(ry)}) — success and fame", "Strong", "Power")

    # ── Dhana Yoga ────────────────────────────────────────────────────
    for dl in [RASHI_LORDS.get(houses[1]["rashi"],""), RASHI_LORDS.get(houses[10]["rashi"],"")]:
        if dl and dl in grahas and h(dl) in [1,4,5,7,9,10]:
            add("Dhana Yoga", f"{dl} (wealth lord) in house {h(dl)} — financial prosperity", "Strong", "Wealth")
            break

    # ── Chandra-Mangal ────────────────────────────────────────────────
    if r("Chandra") == r("Mangal"):
        add("Chandra-Mangal Yoga", "Moon-Mars conjunction — courage brings wealth", "Strong", "Wealth")

    # ── Mangal Dosha ─────────────────────────────────────────────────
    mh = h("Mangal")
    if mh in [1, 2, 4, 7, 8, 12]:
        add("Mangal Dosha", f"Mars in house {mh} — caution in marriage", "Applicable", "Marriage")

    # ── Parvata Yoga ──────────────────────────────────────────────────
    benefics_in_kendra = any(g in BENEFICS and is_kendra(g) for g in BENEFICS if g in grahas)
    malefics_only_dusthana = all(
        h(g) in [6, 8, 12] for g in MALEFICS if g in grahas and g not in ("Rahu","Ketu")
    )
    if benefics_in_kendra and malefics_only_dusthana:
        add("Parvata Yoga", "Benefics in kendras, malefics only in dusthanas — prosperity, fame", "Strong", "Power")

    # ── Adhi Yoga ─────────────────────────────────────────────────────
    adhi_rashis = [(moon_r + i) % 12 for i in [5, 6, 7]]
    adhi_found  = [g for g in ["Budh","Guru","Shukra"] if r(g) in adhi_rashis]
    if adhi_found:
        add("Adhi Yoga", f"{', '.join(adhi_found)} in 6/7/8 from Moon — leadership, long life", "Strong", "Power")

    # ── Amala Yoga ────────────────────────────────────────────────────
    h10_rashi = houses[9]["rashi"]
    in_h10 = [g for g in grahas if r(g) == h10_rashi]
    if in_h10 and all(g in BENEFICS for g in in_h10):
        add("Amala Yoga", "Only benefics in 10th — spotless reputation, moral excellence", "Strong", "Character")

    # ── Pushkala Yoga ─────────────────────────────────────────────────
    moon_lord = RASHI_LORDS.get(moon_r, "")
    if moon_lord and moon_lord in grahas and is_kendra(moon_lord) and (is_own("Chandra") or is_exalted("Chandra")):
        add("Pushkala Yoga", "Strong Moon + Moon's lord in kendra — wealth and respect", "Strong", "Wealth")

    # ── Shankha Yoga ─────────────────────────────────────────────────
    lord_5 = RASHI_LORDS.get(houses[4]["rashi"],"")
    lord_6 = RASHI_LORDS.get(houses[5]["rashi"],"")
    if lord_5 and lord_6 and lord_5 in grahas and lord_6 in grahas:
        if abs(h(lord_5) - h(lord_6)) in [3, 9]:
            add("Shankha Yoga", "Lords of 5th and 6th in mutual kendra — righteous, long-lived", "Strong", "Character")

    # ── Parijata Yoga ─────────────────────────────────────────────────
    if lagna_lord and lagna_lord in grahas:
        exs = EXALTATION.get(lagna_lord, -1)
        if exs >= 0:
            exs_lord = RASHI_LORDS.get(exs,"")
            if exs_lord and exs_lord in grahas and (is_kendra(exs_lord) or is_trikona(exs_lord) or is_exalted(exs_lord)):
                add("Parijata Yoga", "Exaltation chain of lagna lord — enduring fame and dignity", "Strong", "Power")

    # ── Lakshmi Yoga ─────────────────────────────────────────────────
    if (is_own("Shukra") or is_exalted("Shukra")) and lagna_lord and lagna_lord in grahas and is_strong(lagna_lord):
        add("Lakshmi Yoga", "Strong Venus + lagna lord — wealth, beauty, contentment", "Very Strong", "Wealth")

    # ── Vipareeta Raja Yoga ───────────────────────────────────────────
    dusthana = [6, 8, 12]
    d_lords  = {RASHI_LORDS.get(houses[d-1]["rashi"],""): d for d in dusthana}
    for dl, orig in d_lords.items():
        if dl and dl in grahas and h(dl) in dusthana and h(dl) != orig:
            add("Vipareeta Raja Yoga", f"{dl} (dusthana lord) in another dusthana — success through adversity", "Strong", "Power")
            break

    # ── Mahabhagya Yoga ───────────────────────────────────────────────
    is_day = h("Surya") in [7,8,9,10,11,12]
    odd = lambda rashi_i: rashi_i % 2 == 0  # Mesha(0)=Mas=odd
    if is_day and all(odd(x) for x in [lagna_rashi_i, r("Surya"), r("Chandra")]):
        add("Mahabhagya Yoga", "Day birth, odd lagna/sun/moon — greatness and fortune", "Strong", "Power")
    elif not is_day and all(not odd(x) for x in [lagna_rashi_i, r("Surya"), r("Chandra")]):
        add("Mahabhagya Yoga", "Night birth, even lagna/sun/moon — greatness and fortune", "Strong", "Power")

    # ── Chatussagara Yoga ─────────────────────────────────────────────
    occ = {h(g) for g in grahas}
    if {1,4,7,10}.issubset(occ):
        add("Chatussagara Yoga", "All four kendras occupied — wealth, fame, greatness", "Very Strong", "Power")

    # ── Vasumati Yoga ─────────────────────────────────────────────────
    upachaya_r = [(moon_r + i) % 12 for i in [2, 5, 9, 10]]
    if all(r(g) in upachaya_r for g in BENEFICS if g in grahas):
        add("Vasumati Yoga", "All benefics in upachaya from Moon — independent wealth", "Strong", "Wealth")

    # ── Kahala Yoga ───────────────────────────────────────────────────
    lord_4 = RASHI_LORDS.get(houses[3]["rashi"],"")
    lord_9 = RASHI_LORDS.get(houses[8]["rashi"],"")
    if lord_4 and lord_9 and lord_4 in grahas and lord_9 in grahas:
        if abs(h(lord_4)-h(lord_9)) in [3, 9]:
            add("Kahala Yoga", "Lords of 4th and 9th in mutual kendra — bold, obstinate leader", "Moderate", "Character")

    # ── Saraswati Yoga ────────────────────────────────────────────────
    if all(is_kendra(g) or is_trikona(g) for g in ["Budh","Guru","Shukra"]):
        add("Saraswati Yoga", "Mercury, Jupiter, Venus in kendra/trikona — exceptional intellect", "Very Strong", "Intellect")

    # ── Shakata Yoga ─────────────────────────────────────────────────
    if (r("Guru") - r("Chandra")) % 12 in [5, 7, 11]:
        add("Shakata Yoga", "Jupiter in 6/8/12 from Moon — fluctuating fortune, wheel of fate", "Applicable", "Adversity")

    # ── Rajalakshana Yoga ─────────────────────────────────────────────
    if lagna_lord and lagna_lord in grahas and is_kendra(lagna_lord) and any(is_exalted(g) and is_kendra(g) for g in grahas):
        add("Rajalakshana Yoga", "Lagna lord in kendra with exalted planet — born leader", "Moderate", "Power")

    return yogas


def _calc_antardashas(maha_lord: str, maha_start: datetime, maha_years: float,
                      include_pratyantar: bool = False,
                      include_sukshma: bool = False) -> list:
    """Calculate Antardashas (sub-periods) within a Mahadasha."""
    total_days = maha_years * 365.25
    start_idx = DASHA_SEQ.index(maha_lord)
    antardashas = []
    cur = maha_start

    for i in range(9):
        antar_lord = DASHA_SEQ[(start_idx + i) % 9]
        antar_days = (maha_years * DASHA_YEARS[antar_lord] / 120) * 365.25
        end = cur + timedelta(days=antar_days)
        antar_years = antar_days / 365.25
        entry = {
            "lord": antar_lord,
            "years": round(antar_years, 2),
            "start": cur.strftime("%Y-%m-%d"),
            "end": end.strftime("%Y-%m-%d"),
        }
        if include_pratyantar:
            entry["pratyantardashas"] = _calc_pratyantardashas(
                antar_lord, cur, antar_years, include_sukshma=include_sukshma
            )
        antardashas.append(entry)
        cur = end

    return antardashas


def _detect_alerts(grahas_out: dict, sun_lon: float) -> list:
    """Detect planetary war (Graha Yuddha) and combustion alerts."""
    alerts = []
    YUDDHA_PLANETS = ["Mangal", "Budh", "Guru", "Shukra", "Shani"]
    eligible = [(g, grahas_out[g]) for g in YUDDHA_PLANETS if g in grahas_out]

    for i, (g1, d1) in enumerate(eligible):
        for g2, d2 in eligible[i + 1:]:
            diff = abs(d1["longitude"] - d2["longitude"])
            if diff > 180:
                diff = 360 - diff
            if diff <= 1.0:
                lat1 = d1.get("lat_ecl", 0.0)
                lat2 = d2.get("lat_ecl", 0.0)
                winner = g1 if lat1 >= lat2 else g2
                loser  = g2 if winner == g1 else g1
                alerts.append({
                    "type": "graha_yuddha",
                    "planet1": g1, "planet2": g2,
                    "winner": winner, "loser": loser,
                    "orb": round(diff, 2),
                    "message": (
                        f"Graha Yuddha: {g1} & {g2} ({diff:.1f}° apart) — "
                        f"{loser} loses the war and its karakas are weakened"
                    ),
                })

    for gname, gdata in grahas_out.items():
        if gdata.get("combust"):
            alerts.append({
                "type": "combust",
                "planet": gname,
                "message": f"{gname} is Combust (asta) — too close to Sun, its significations are severely weakened",
            })

    return alerts


def calc_kundali(
    year: int, month: int, day: int,
    hour: int, minute: int,
    lat: float, lon: float, tz: float,
    name: str = "", place: str = "",
    ayanamsha_mode: str = "lahiri",
    rahu_mode: str = "mean",
    calc_options: list = None,
) -> Tuple[Optional[dict], Optional[str]]:
    """
    Calculate full Vedic birth chart.
    ayanamsha_mode: "lahiri" | "raman" | "kp" | "true_citra"
    rahu_mode: "mean" | "true"
    calc_options: list of extra calcs to include, e.g. ["antardasha", "d3", "d10", ...]
    Returns (data_dict, error_str).
    """
    if not SWE_AVAILABLE:
        return None, "pyswisseph not installed"

    _init_grahas()
    swe.set_ephe_path(EPHE_PATH)

    # Set ayanamsha
    ayan_id = AYANAMSHA_MODES.get(ayanamsha_mode, 1)
    swe.set_sid_mode(ayan_id)

    # Select Rahu mode
    grahas_map = _GRAHAS_MAP_TRUE if rahu_mode == "true" else _GRAHAS_MAP_MEAN

    if calc_options is None:
        calc_options = []

    include_pratyantar = "pratyantar" in calc_options
    include_sukshma = "sukshma" in calc_options

    # Convert local time → UTC, handling date rollover
    dt_utc = datetime(year, month, day, hour, minute) - timedelta(hours=tz)
    jd = swe.julday(dt_utc.year, dt_utc.month, dt_utc.day,
                    dt_utc.hour + dt_utc.minute / 60.0)
    ayanamsha = swe.get_ayanamsa(jd)

    # ── Grahas ─────────────────────────────────────────────────────
    grahas = {}
    for pid, gname in grahas_map.items():
        flags = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
        res = swe.calc_ut(jd, pid, flags)
        lon_g = res[0][0]
        speed = res[0][3]
        lat_ecl = res[0][1]  # ecliptic latitude
        try:
            res_eq = swe.calc_ut(jd, pid, swe.FLG_SWIEPH | swe.FLG_EQUATORIAL)
            ra_val  = res_eq[0][0]
            dec_val = res_eq[0][1]
        except Exception:
            ra_val = dec_val = 0.0
        rashi_i = int(lon_g / 30)
        nak_i = int(lon_g / (360 / 27))
        grahas[gname] = {
            "longitude": lon_g, "speed": speed,
            "rashi": rashi_i, "rashi_name": RASHI_NAMES[rashi_i],
            "degree": round(lon_g % 30, 4),
            "nakshatra": NAKSHATRAS[nak_i], "nakshatra_idx": nak_i,
            "pada": int((lon_g % (360 / 27)) / (360 / 108)) + 1,
            "retro": speed < 0, "house": 0,
            "lat_ecl": round(lat_ecl, 4),
            "ra": round(ra_val, 4),
            "dec": round(dec_val, 4),
        }

    # Ketu (180° opposite Rahu)
    rahu_lon = grahas["Rahu"]["longitude"]
    ketu_lon = (rahu_lon + 180) % 360
    ketu_rashi = int(ketu_lon / 30)
    ketu_nak = int(ketu_lon / (360 / 27))
    grahas["Ketu"] = {
        "longitude": ketu_lon, "speed": 0,
        "rashi": ketu_rashi, "rashi_name": RASHI_NAMES[ketu_rashi],
        "degree": round(ketu_lon % 30, 4),
        "nakshatra": NAKSHATRAS[ketu_nak], "nakshatra_idx": ketu_nak,
        "pada": int((ketu_lon % (360 / 27)) / (360 / 108)) + 1,
        "retro": True, "house": 0,
        "lat_ecl": 0.0, "ra": 0.0, "dec": 0.0,
    }

    # ── Lagna (Ascendant) ──────────────────────────────────────────
    # Compute tropical ascendant then subtract ayanamsa explicitly.
    # Using FLG_SIDEREAL in houses_ex can give inconsistent results
    # across pyswisseph versions compared to manual subtraction.
    _, ascmc_trop = swe.houses_ex(jd, lat, lon, b'P')
    lagna = (ascmc_trop[0] - ayanamsha) % 360
    lagna_rashi_i = int(lagna / 30)

    # Whole Sign houses
    houses = []
    for i in range(12):
        ri = (lagna_rashi_i + i) % 12
        houses.append({
            "house": i + 1,
            "rashi": ri, "rashi_name": RASHI_NAMES[ri],
            "lord": RASHI_LORDS[ri],
        })

    # Assign house to each graha
    def get_house(lon_val: float) -> int:
        rashi_i = int(lon_val / 30)
        return (rashi_i - lagna_rashi_i) % 12 + 1

    for g in grahas.values():
        g["house"] = get_house(g["longitude"])

    # ── Graha status + relationship ────────────────────────────────
    def graha_status(gname: str) -> str:
        r = grahas[gname]["rashi"]
        if EXALTATION.get(gname) == r: return "Uchcha (Exalted)"
        if DEBILITATION.get(gname) == r: return "Neecha (Debilitated)"
        if RASHI_LORDS.get(r) == gname: return "Svakshetra (Own)"
        return "Normal"

    # ── Navamsha (D-9) via divisional_charts ───────────────────────
    nav_chart = calc_varga_chart(grahas, lagna, 9)
    navamsha_out = {}
    for gn, gd in nav_chart["grahas"].items():
        navamsha_out[gn] = {
            "rashi": gd["rashi"],
            "house": gd["house"],
            "degree": gd.get("degree"),
            "status": gd["status"],
            "retro": gd["retro"],
        }
    navamsha_lagna = nav_chart["lagna"]
    navamsha_houses = nav_chart["houses"]

    # ── Bhav Chalit ────────────────────────────────────────────────
    bhav_chalit = _calc_bhav_chalit(jd, lat, lon, ayanamsha, grahas)

    # ── Vimshottari Dasha ──────────────────────────────────────────
    moon = grahas["Chandra"]
    nak_i = moon["nakshatra_idx"]
    nak_lord = NAKSHATRA_LORDS[nak_i]
    nak_span = 360 / 27
    nak_start = nak_i * nak_span
    elapsed = moon["longitude"] - nak_start
    remaining = 1 - (elapsed / nak_span)
    start_idx = DASHA_SEQ.index(nak_lord)
    birth_dt = datetime(year, month, day, hour, minute)
    dashas = []
    cur = birth_dt

    # First dasha (partial — from birth to end of current Maha)
    lord = DASHA_SEQ[start_idx]
    maha_years = round(DASHA_YEARS[lord] * remaining, 4)
    end = datetime.fromordinal(cur.toordinal() + round(maha_years * 365.25))
    dasha_entry = {
        "lord": lord, "years": round(maha_years, 2),
        "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d"),
    }
    if "antardasha" in calc_options:
        # Correctly identify the running antardasha at birth:
        # reconstruct the full Maha's antardasha sequence from its virtual start,
        # then keep only sub-periods that end on or after the birth date.
        full_maha_years = DASHA_YEARS[lord]
        elapsed_years = full_maha_years - maha_years
        virtual_start = birth_dt - timedelta(days=elapsed_years * 365.25)
        all_antars = _calc_antardashas(lord, virtual_start, full_maha_years,
                                       include_pratyantar=include_pratyantar,
                                       include_sukshma=include_sukshma)
        birth_date = birth_dt.date()
        post_birth = [a for a in all_antars
                      if datetime.strptime(a["end"], "%Y-%m-%d").date() >= birth_date]
        if post_birth:
            post_birth[0] = {**post_birth[0], "start": birth_dt.strftime("%Y-%m-%d")}
        dasha_entry["antardashas"] = post_birth
    dashas.append(dasha_entry)
    cur = end

    # Remaining 9 dashas (full)
    for i in range(1, 10):
        lord = DASHA_SEQ[(start_idx + i) % 9]
        maha_years_full = DASHA_YEARS[lord]
        end = datetime.fromordinal(cur.toordinal() + round(maha_years_full * 365.25))
        dasha_entry = {
            "lord": lord, "years": maha_years_full,
            "start": cur.strftime("%Y-%m-%d"), "end": end.strftime("%Y-%m-%d"),
        }
        if "antardasha" in calc_options:
            dasha_entry["antardashas"] = _calc_antardashas(
                lord, cur, maha_years_full, include_pratyantar=include_pratyantar,
                include_sukshma=include_sukshma
            )
        dashas.append(dasha_entry)
        cur = end

    # ── Yoga detection ─────────────────────────────────────────────
    yogas = _detect_yogas(grahas, houses, lagna_rashi_i)

    # ── Build clean API response ───────────────────────────────────
    graha_order = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu"]
    grahas_out = {}
    for gn in graha_order:
        g = grahas[gn]
        nak_lord_g = NAKSHATRA_LORDS[g["nakshatra_idx"]]
        relationship = _graha_relationship(gn, g["rashi"])
        grahas_out[gn] = {
            "rashi": g["rashi_name"],
            "rashi_en": RASHI_EN.get(g["rashi_name"], ""),
            "house": g["house"],
            "degree": g["degree"],
            "longitude": round(g["longitude"], 4),
            "nakshatra": g["nakshatra"],
            "nakshatra_hindi": NAKSHATRA_HINDI.get(g["nakshatra"], ""),
            "nakshatra_lord": nak_lord_g,
            "pada": g["pada"],
            "retro": g["retro"],
            "status": graha_status(gn),
            "relationship": relationship,
            "house_lord": RASHI_LORDS.get(g["rashi"], ""),
            "graha_en": GRAHA_EN.get(gn, gn),
            "graha_hindi": GRAHA_HINDI.get(gn, ""),
            "karaka": GRAHA_KARAKA.get(gn, ""),
            "speed": round(g["speed"], 4),
            "combust": _is_combust(gn, g["longitude"], grahas["Surya"]["longitude"], g["retro"]),
            "ruler_of": [(r_i - lagna_rashi_i) % 12 + 1
                         for r_i, lord in RASHI_LORDS.items() if lord == gn]
                        + ([(10 - lagna_rashi_i) % 12 + 1] if gn == "Rahu" else [])   # Rahu co-rules Kumbha
                        + ([(7  - lagna_rashi_i) % 12 + 1] if gn == "Ketu" else []),   # Ketu co-rules Vrischika
            "lat_ecl": round(g.get("lat_ecl", 0.0), 4),
            "ra":      round(g.get("ra", 0.0), 4),
            "dec":     round(g.get("dec", 0.0), 4),
        }

    # House output with significations
    houses_out = []
    for h in houses:
        planets_in = [gn for gn in graha_order if grahas[gn]["house"] == h["house"]]
        houses_out.append({
            "house": h["house"],
            "rashi": h["rashi_name"],
            "rashi_en": RASHI_EN.get(h["rashi_name"], ""),
            "lord": h["lord"],
            "lord_en": GRAHA_EN.get(h["lord"], h["lord"]),
            "signification": HOUSE_SIGNIFICATIONS.get(h["house"], ""),
            "planets": planets_in,
        })

    # Compute aspects for each bhava — Rahu/Ketu excluded (chaya grahas, no graha drishti)
    def _aspects(gname, g_house):
        if gname in ("Rahu", "Ketu"):
            return set()
        asp = {(g_house + 5) % 12 + 1}  # 7th aspect (all planets)
        if gname == "Mangal":
            asp.update({(g_house + 2) % 12 + 1, (g_house + 6) % 12 + 1})
        elif gname == "Guru":
            asp.update({(g_house + 3) % 12 + 1, (g_house + 7) % 12 + 1})
        elif gname == "Shani":
            asp.update({(g_house + 1) % 12 + 1, (g_house + 8) % 12 + 1})
        return asp

    aspected_by_house = {i+1: [] for i in range(12)}
    for gn in graha_order:
        for asp_h in _aspects(gn, grahas[gn]["house"]):
            aspected_by_house[asp_h].append(gn)

    for h_out in houses_out:
        ri = next(h["rashi"] for h in houses if h["house"] == h_out["house"])
        h_out["gender"]      = _RASHI_GENDER[ri]
        h_out["modality"]    = _RASHI_MODALITY[ri]
        h_out["tattva"]      = _RASHI_TATTVA[ri]
        h_out["aspected_by"] = aspected_by_house.get(h_out["house"], [])

    # Lagna nakshatra
    lagna_nak_i = int(lagna / (360 / 27))
    lagna_data = {
        "rashi": RASHI_NAMES[lagna_rashi_i],
        "rashi_en": RASHI_EN.get(RASHI_NAMES[lagna_rashi_i], ""),
        "nakshatra": NAKSHATRAS[lagna_nak_i],
        "nakshatra_hindi": NAKSHATRA_HINDI.get(NAKSHATRAS[lagna_nak_i], ""),
        "pada": int((lagna % (360/27)) / (360/108)) + 1,
        "degree": round(lagna % 30, 4),
        "full_degree": round(lagna, 4),
    }

    # Ayanamsha mode name for display
    ayan_label = ayanamsha_mode.replace("_", " ").title()
    if ayanamsha_mode == "lahiri":
        ayan_label = "Lahiri/Chitra Paksha"
    elif ayanamsha_mode == "kp":
        ayan_label = "Krishnamurti (KP)"
    elif ayanamsha_mode == "true_citra":
        ayan_label = "True Chitrapaksha"

    # Birth Panchang
    birth_panchang = _calc_birth_panchang(
        jd, grahas["Surya"]["longitude"], grahas["Chandra"]["longitude"], lat, lon, tz,
        moon_nak_i=grahas["Chandra"]["nakshatra_idx"],
        sun_nak_i=grahas["Surya"]["nakshatra_idx"],
    )

    # Personal characteristics
    moon_nak_lord = NAKSHATRA_LORDS[grahas["Chandra"]["nakshatra_idx"]]
    personal = _personal_chars(
        grahas["Chandra"]["nakshatra_idx"],
        grahas["Chandra"]["rashi"],
        lagna_rashi_i,
        moon_nak_lord,
    )

    result = {
        "name": name,
        "place": place,
        "birth_date": birth_dt.strftime("%d %B %Y, %I:%M %p"),
        "lat": lat, "lon": lon, "tz": tz,
        "ayanamsha": round(ayanamsha, 4),
        "ayanamsha_mode": ayanamsha_mode,
        "ayanamsha_label": ayan_label,
        "rahu_mode": rahu_mode,
        "lagna": lagna_data,
        "grahas": grahas_out,
        "houses": houses_out,
        "navamsha": navamsha_out,
        "navamsha_lagna": navamsha_lagna,
        "navamsha_houses": [
            {"house": h["house"], "rashi": h["rashi"], "lord": h["lord"]}
            for h in navamsha_houses
        ],
        "dashas": dashas,
        "yogas": yogas,
        "alerts": _detect_alerts(grahas_out, grahas["Surya"]["longitude"]),
        "birth_panchang": birth_panchang,
        "personal": personal,
        "bhav_chalit": bhav_chalit,
    }

    # ── Optional varga charts ──────────────────────────────────────
    requested_vargas = [opt for opt in calc_options if opt.startswith("d") and opt[1:].isdigit()]
    if requested_vargas:
        result["varga_charts"] = {}
        for rv in requested_vargas:
            div = int(rv[1:])
            if div in VARGA_NAMES and div != 1:  # D-1 is already the main chart
                vc = calc_varga_chart(grahas, lagna, div)
                result["varga_charts"][f"D-{div}"] = vc

    # ── Optional Ashtakavarga ────────────────────────────────────
    if "ashtakavarga" in calc_options:
        try:
            from api.services.ashtakavarga import calc_ashtakavarga
            result["ashtakavarga"] = calc_ashtakavarga(grahas, lagna_rashi_i)
        except Exception:
            pass  # Non-fatal — skip if calculation fails

    # ── Optional Shadbala ────────────────────────────────────────
    if "shadbala" in calc_options:
        try:
            from api.services.shadbala import calc_shadbala, calc_bhavabala
            sb = calc_shadbala(grahas)
            result["shadbala"] = sb
            result["bhavabala"] = calc_bhavabala(houses, grahas, sb)
        except Exception:
            pass  # Non-fatal — skip if calculation fails

    # ── Optional Upagraha ────────────────────────────────────────
    if "upagraha" in calc_options:
        try:
            from api.services.upagraha import calc_upagraha
            weekday = birth_dt.weekday()  # 0=Monday in Python
            # Python weekday: 0=Mon…6=Sun → convert to 0=Sun…6=Sat
            wday_sun = (weekday + 1) % 7
            # Get sunrise/sunset JDs for proper hora-based calculation
            _geo = (lon, lat, 0)
            try:
                _, _tr = swe.rise_trans(jd - 0.5, swe.SUN, b"", swe.FLG_SWIEPH, _geo, 0, 0, swe.CALC_RISE | swe.BIT_DISC_CENTER)
                _, _ts = swe.rise_trans(jd - 0.5, swe.SUN, b"", swe.FLG_SWIEPH, _geo, 0, 0, swe.CALC_SET | swe.BIT_DISC_CENTER)
                _sr_jd = _tr[0] if _tr else 0
                _ss_jd = _ts[0] if _ts else 0
            except Exception:
                _sr_jd = _ss_jd = 0
            result["upagraha"] = calc_upagraha(
                sun_lon=grahas["Surya"]["longitude"],
                moon_lon=grahas["Chandra"]["longitude"],
                weekday=wday_sun,
                sunrise_jd=_sr_jd,
                sunset_jd=_ss_jd,
                jd=jd, lat=lat, lon=lon, ayanamsha=ayanamsha,
            )
        except Exception:
            pass  # Non-fatal — skip if calculation fails

    return result, None
