"""
Shadbala (Six-fold Planetary Strength) Calculator.
Simplified classical method using positional data.
"""
from api.config import RASHI_NAMES, RASHI_LORDS, EXALTATION, DEBILITATION

# Required minimum Shadbala in Rupas (1 Rupa = 60 Virupas)
REQUIRED_SHADBALA = {
    "Surya": 6.5, "Chandra": 6.0, "Mangal": 5.0,
    "Budh": 7.0, "Guru": 6.5, "Shukra": 5.5, "Shani": 5.0,
}

# Naisargika Bala (Natural strength) — fixed values in Virupas
NAISARGIKA_BALA = {
    "Surya": 60, "Chandra": 51.43, "Mangal": 17.14,
    "Budh": 25.71, "Guru": 34.28, "Shukra": 42.86, "Shani": 8.57,
}

# Dig Bala directions — planets strong in specific houses
# 1=East(H1), 4=North(H4/Nadir?), 7=West(H7), 10=South(H10/MC)
DIG_BALA_HOUSES = {
    "Surya": 10, "Mangal": 10,  # Strong in 10th (MC)
    "Guru": 1, "Budh": 1,       # Strong in 1st (Asc)
    "Chandra": 4, "Shukra": 4,  # Strong in 4th (IC)
    "Shani": 7,                  # Strong in 7th (Desc)
}

_PLANETS_7 = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani"]


def _sthana_bala(gname: str, rashi_i: int, degree: float) -> float:
    """
    Positional strength based on sign placement.
    Uccha Bala (exaltation) + Saptavargaja Bala (sign-based).
    """
    bala = 0.0

    # Uccha Bala — max 60 when at exact exaltation point, 0 at debilitation
    exalt_rashi = EXALTATION.get(gname)
    debil_rashi = DEBILITATION.get(gname)
    if exalt_rashi is not None and debil_rashi is not None:
        # Distance from debilitation point (in rashis)
        dist = (rashi_i - debil_rashi) % 12
        # Max at exaltation (opposite of debilitation is ~6 signs away)
        uccha_bala = (dist / 6.0) * 60 if dist <= 6 else ((12 - dist) / 6.0) * 60
        bala += uccha_bala
    else:
        bala += 30  # Rahu/Ketu default

    # Oja-Yugma Bala — odd/even sign/navamsha bonus
    is_odd_sign = rashi_i % 2 == 0  # 0=Mesha(odd), 1=Vrishabha(even)
    if gname in ("Surya", "Mangal", "Guru"):
        bala += 15 if is_odd_sign else 0
    elif gname in ("Chandra", "Shukra"):
        bala += 15 if not is_odd_sign else 0
    else:
        bala += 7.5  # Budh, Shani neutral

    # Kendradi Bala — planets in kendra get 60, panapara 30, apoklima 15
    # (This needs house info, approximated from sign)
    bala += 30  # Average estimate without house

    return round(bala, 2)


def _dig_bala(gname: str, house: int) -> float:
    """Directional strength — max 60 when in strongest house, 0 when opposite."""
    strong_house = DIG_BALA_HOUSES.get(gname, 1)
    # Distance from strongest house
    dist = abs(house - strong_house)
    if dist > 6:
        dist = 12 - dist
    # Max at strong house (dist=0 → 60), min at opposite (dist=6 → 0)
    return round((1 - dist / 6.0) * 60, 2)


def _kaala_bala(gname: str) -> float:
    """
    Temporal strength — simplified.
    Full calculation needs hora lord, vara lord, masa lord etc.
    Using average values for now.
    """
    # Simplified averages from classical texts
    averages = {
        "Surya": 40, "Chandra": 35, "Mangal": 25,
        "Budh": 30, "Guru": 35, "Shukra": 30, "Shani": 20,
    }
    return averages.get(gname, 25)


def _chesta_bala(gname: str, speed: float) -> float:
    """
    Motional strength — based on speed.
    Retrograde planets get higher Chesta Bala.
    """
    if gname in ("Surya", "Chandra"):
        return 30  # Sun/Moon always direct, fixed value

    # Retrograde = strong (60), stationary = very strong, fast = weak
    if speed < -0.1:
        return 55  # Retrograde
    elif abs(speed) < 0.05:
        return 60  # Stationary
    elif speed > 0:
        return max(15, 45 - speed * 10)  # Direct, slower = stronger
    return 30


def _drik_bala(gname: str, house: int, all_houses: dict) -> float:
    """
    Aspectual strength — simplified.
    Benefics aspecting = positive, malefics = negative.
    """
    benefics = {"Guru", "Shukra", "Budh"}
    malefics = {"Surya", "Mangal", "Shani"}

    bala = 0
    for other_name, other_house in all_houses.items():
        if other_name == gname:
            continue
        aspect_dist = (house - other_house) % 12
        # 7th aspect (opposition) always applies
        has_aspect = aspect_dist == 6
        # Special aspects: Mars=4,8; Jupiter=5,9; Saturn=3,10
        if other_name == "Mangal" and aspect_dist in [3, 7]:
            has_aspect = True
        elif other_name == "Guru" and aspect_dist in [4, 8]:
            has_aspect = True
        elif other_name == "Shani" and aspect_dist in [2, 9]:
            has_aspect = True

        if has_aspect:
            if other_name in benefics:
                bala += 10
            elif other_name in malefics:
                bala -= 5

    return max(0, round(30 + bala, 2))


def calc_shadbala(grahas: dict) -> dict:
    """
    Calculate Shadbala for 7 planets.
    grahas: internal dict with rashi (int), degree, house, speed.
    Returns per-planet breakdown + totals.
    """
    all_houses = {gn: grahas[gn]["house"] for gn in _PLANETS_7 if gn in grahas}

    result = {}
    for gname in _PLANETS_7:
        if gname not in grahas:
            continue
        g = grahas[gname]
        rashi_i = g["rashi"]
        degree = g["degree"]
        house = g["house"]
        speed = g.get("speed", 0)

        sthana = _sthana_bala(gname, rashi_i, degree)
        dig = _dig_bala(gname, house)
        kaala = _kaala_bala(gname)
        chesta = _chesta_bala(gname, speed)
        naisargika = NAISARGIKA_BALA.get(gname, 20)
        drik = _drik_bala(gname, house, all_houses)

        total_virupas = sthana + dig + kaala + chesta + naisargika + drik
        total_rupas = round(total_virupas / 60, 2)
        required = REQUIRED_SHADBALA.get(gname, 5.0)
        ratio = round(total_rupas / required, 2) if required > 0 else 1.0

        result[gname] = {
            "sthana_bala": sthana,
            "dig_bala": dig,
            "kaala_bala": kaala,
            "chesta_bala": chesta,
            "naisargika_bala": naisargika,
            "drik_bala": drik,
            "total_virupas": round(total_virupas, 2),
            "total_rupas": total_rupas,
            "required_rupas": required,
            "ratio": ratio,
            "is_strong": ratio >= 1.0,
        }

    return {"planets": result}


def calc_bhavabala(houses: list, grahas: dict, shadbala: dict) -> list:
    """
    Calculate Bhavabala (House strength).
    Based on: lord's Shadbala + occupant strength + aspects.
    """
    result = []
    for h in houses:
        house_num = h["house"]
        lord = h["lord"]
        rashi_i = h["rashi"]

        # Lord strength (from Shadbala)
        lord_strength = 0
        if lord in shadbala.get("planets", {}):
            lord_strength = shadbala["planets"][lord]["total_rupas"]

        # Occupant bonus
        occupant_bonus = 0
        for gn in ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu"]:
            if gn in grahas and grahas[gn]["house"] == house_num:
                if gn in ("Guru", "Shukra", "Budh"):
                    occupant_bonus += 1.0  # Benefic occupant
                elif gn in ("Surya", "Mangal", "Shani"):
                    occupant_bonus += 0.3  # Malefic occupant (some strength but less)

        total = round(lord_strength + occupant_bonus, 2)
        result.append({
            "house": house_num,
            "rashi": RASHI_NAMES[rashi_i] if isinstance(rashi_i, int) else rashi_i,
            "lord": lord,
            "strength": total,
        })

    # Add rank
    sorted_houses = sorted(result, key=lambda x: x["strength"], reverse=True)
    for rank, h in enumerate(sorted_houses, 1):
        for r in result:
            if r["house"] == h["house"]:
                r["rank"] = rank
                break

    return result
