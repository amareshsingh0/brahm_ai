"""
Prashna Kundali (Horary Astrology) Service.
Computes a chart for the current moment of asking and derives a verdict.
"""
from datetime import datetime, timezone, timedelta
from typing import Tuple, Optional
from api.services.kundali_service import calc_kundali
from api.config import RASHI_LORDS, EXALTATION

# Hora lords — weekday→24-hour sequence (0=Monday in Python, 6=Sunday)
# First hora of each day is ruled by the day's planet
_HORA_SEQ_BY_WEEKDAY = {
    6: ["Surya","Shukra","Budh","Chandra","Shani","Guru","Mangal"],   # Sunday
    0: ["Chandra","Shani","Guru","Mangal","Surya","Shukra","Budh"],   # Monday
    1: ["Mangal","Surya","Shukra","Budh","Chandra","Shani","Guru"],   # Tuesday
    2: ["Budh","Chandra","Shani","Guru","Mangal","Surya","Shukra"],   # Wednesday
    3: ["Guru","Mangal","Surya","Shukra","Budh","Chandra","Shani"],   # Thursday
    4: ["Shukra","Budh","Chandra","Shani","Guru","Mangal","Surya"],   # Friday
    5: ["Shani","Guru","Mangal","Surya","Shukra","Budh","Chandra"],   # Saturday
}

# Question type → relevant houses to examine
QUESTION_HOUSES = {
    "wealth":       [2, 11],
    "career":       [10, 6],
    "relationship": [7, 5],
    "health":       [1, 6, 8],
    "property":     [4],
    "travel":       [9, 12],
    "education":    [4, 5],
    "spiritual":    [9, 12],
    "children":     [5],
    "general":      [1, 7],
}

BENEFICS  = {"Guru", "Shukra", "Budh", "Chandra"}
MALEFICS  = {"Surya", "Mangal", "Shani", "Rahu", "Ketu"}
KENDRAS   = {1, 4, 7, 10}
TRIKONAS  = {1, 5, 9}
DUSTHANAS = {6, 8, 12}


def _hora_lord(now_local: datetime) -> str:
    weekday = now_local.weekday()  # 0=Mon...6=Sun
    seq = _HORA_SEQ_BY_WEEKDAY.get(weekday, _HORA_SEQ_BY_WEEKDAY[6])
    return seq[now_local.hour % 7]


def _prashna_verdict(chart: dict, question_type: str) -> Tuple[str, list]:
    grahas = chart.get("grahas", {})
    houses = chart.get("houses", [])
    lagna_rashi = chart.get("lagna", {}).get("rashi", "")
    lagna_rashi_i = next((h["rashi"] for h in houses if h["house"] == 1), None)

    lagna_lord_name = ""
    if lagna_rashi_i is not None:
        from api.config import RASHI_NAMES
        idx = RASHI_NAMES.index(lagna_rashi) if lagna_rashi in RASHI_NAMES else 0
        lagna_lord_name = RASHI_LORDS.get(idx, "")

    lagna_lord_house = grahas.get(lagna_lord_name, {}).get("house", 0)
    moon_house = grahas.get("Chandra", {}).get("house", 0)

    positive = 0
    negative = 0
    factors  = []

    # Benefics in kendras → positive
    for b in BENEFICS:
        g = grahas.get(b)
        if g and g["house"] in KENDRAS:
            positive += 1
            factors.append(f"{b} in {g['house']}H (kendra) — auspicious presence")

    # Malefics in kendras → negative
    for m in MALEFICS:
        g = grahas.get(m)
        if g and g["house"] in KENDRAS:
            negative += 1
            factors.append(f"{m} in {g['house']}H (kendra) — challenging")

    # Lagna lord placement
    if lagna_lord_name and lagna_lord_house:
        if lagna_lord_house in KENDRAS | TRIKONAS:
            positive += 2
            factors.append(f"Lagna lord {lagna_lord_name} in {lagna_lord_house}H — very favorable")
        elif lagna_lord_house in DUSTHANAS:
            negative += 2
            factors.append(f"Lagna lord {lagna_lord_name} in {lagna_lord_house}H (dusthana) — unfavorable")

    # Moon placement
    if moon_house in KENDRAS | TRIKONAS:
        positive += 1
        factors.append(f"Moon in {moon_house}H — mind supports the query")
    elif moon_house in DUSTHANAS:
        negative += 1
        factors.append(f"Moon in {moon_house}H (dusthana) — mental obstacles present")

    # Question-specific house strength
    q_houses = QUESTION_HOUSES.get(question_type, QUESTION_HOUSES["general"])
    for qh in q_houses:
        planets_here = [g for g, d in grahas.items() if d.get("house") == qh]
        benefics_here = [p for p in planets_here if p in BENEFICS]
        malefics_here = [p for p in planets_here if p in MALEFICS]
        if benefics_here:
            positive += 1
            factors.append(f"Benefic {', '.join(benefics_here)} in relevant house {qh}H — supports {question_type}")
        if malefics_here:
            negative += 1
            factors.append(f"Malefic {', '.join(malefics_here)} in relevant house {qh}H — hinders {question_type}")

    if positive >= negative + 2:
        verdict = "YES"
    elif negative >= positive + 2:
        verdict = "NO"
    else:
        verdict = "MIXED"

    return verdict, factors


def calc_prashna(
    lat: float, lon: float, tz: float,
    question: str = "",
    question_type: str = "general",
    ayanamsha_mode: str = "lahiri",
) -> Tuple[Optional[dict], Optional[str]]:
    """Compute Prashna Kundali for the current moment."""
    now_utc = datetime.now(timezone.utc)
    now_local = now_utc + timedelta(hours=tz)

    chart, err = calc_kundali(
        now_local.year, now_local.month, now_local.day,
        now_local.hour, now_local.minute,
        lat, lon, tz,
        name="Prashna",
        place="",
        ayanamsha_mode=ayanamsha_mode,
        calc_options=[],
    )
    if err:
        return None, err

    hora = _hora_lord(now_local)
    verdict, factors = _prashna_verdict(chart, question_type)

    chart["prashna_question"]  = question
    chart["prashna_type"]      = question_type
    chart["prashna_verdict"]   = verdict
    chart["prashna_factors"]   = factors
    chart["hora_lord"]         = hora
    chart["prashna_datetime"]  = now_local.strftime("%d %B %Y, %H:%M")

    return chart, None
