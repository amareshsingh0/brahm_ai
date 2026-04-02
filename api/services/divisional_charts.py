"""
Divisional Charts (Varga) Calculator — D1 through D60.
All 16 standard Parashari Varga charts.
"""
from api.config import RASHI_NAMES, RASHI_LORDS, EXALTATION, DEBILITATION

# ── Navamsha start signs by element ──────────────────────────────────────────
# Fire (0,4,8) → Mesha, Earth (1,5,9) → Makara, Air (2,6,10) → Tula, Water (3,7,11) → Karka
_ELEMENT_START = {0:0, 1:9, 2:6, 3:3}  # fire→0, earth→9, air→6, water→3

VARGA_NAMES = {
    1: ("D-1",  "Rashi",             "Body, Physical, General"),
    2: ("D-2",  "Hora",              "Wealth, Family"),
    3: ("D-3",  "Drekkana",          "Siblings, Courage"),
    4: ("D-4",  "Chaturthamsha",     "Property, Fortune"),
    5: ("D-5",  "Panchamsha",        "Fame, Power, Authority"),
    6: ("D-6",  "Shashthamsha",      "Health, Enemies, Debts"),
    7: ("D-7",  "Saptamsha",         "Children, Progeny"),
    8: ("D-8",  "Ashtamsha",         "Sudden Events, Obstacles"),
    9: ("D-9",  "Navamsha",          "Marriage, Dharma, Soul"),
    10: ("D-10", "Dashamsha",        "Career, Profession"),
    11: ("D-11", "Ekadashamsha",     "Gains, Income, Elder Siblings"),
    12: ("D-12", "Dwadashamsha",     "Parents"),
    16: ("D-16", "Shodashamsha",     "Vehicles, Happiness"),
    20: ("D-20", "Vimshamsha",       "Spiritual Progress"),
    24: ("D-24", "Chaturvimshamsha", "Education, Learning"),
    27: ("D-27", "Saptavimshamsha",  "Strength, Vitality"),
    30: ("D-30", "Trimshamsha",      "Evils, Misfortunes"),
    40: ("D-40", "Khavedamsha",      "Auspicious/Inauspicious"),
    45: ("D-45", "Akshavedamsha",    "General Well-being"),
    60: ("D-60", "Shashtiamsha",     "Past Life Karma, All Matters"),
}


def _element_of(rashi_i: int) -> int:
    """0=Fire, 1=Earth, 2=Air, 3=Water"""
    return rashi_i % 4 if rashi_i % 3 == 0 else (
        1 if rashi_i in [1, 5, 9] else
        2 if rashi_i in [2, 6, 10] else
        3  # 3, 7, 11
    )


def _element_simple(rashi_i: int) -> int:
    """Element index: Fire=0, Earth=1, Air=2, Water=3"""
    return rashi_i % 4


def calc_varga_rashi(longitude: float, division: int) -> int:
    """Calculate the rashi index (0-11) for a planet at given sidereal longitude in the given varga chart."""

    rashi_i = int(longitude / 30)
    deg_in_rashi = longitude % 30

    if division == 1:
        # D-1: Rashi chart — same as natal
        return rashi_i

    elif division == 2:
        # D-2: Hora — first 15° = Sun's hora, last 15° = Moon's hora
        # Odd signs: 0-15° → Simha(4), 15-30° → Karka(3)
        # Even signs: 0-15° → Karka(3), 15-30° → Simha(4)
        is_odd = (rashi_i % 2 == 0)  # 0-indexed: Mesha=0 is odd sign
        first_half = deg_in_rashi < 15
        if is_odd:
            return 4 if first_half else 3  # Simha / Karka
        else:
            return 3 if first_half else 4  # Karka / Simha

    elif division == 3:
        # D-3: Drekkana — 3 parts of 10° each
        # 1st (0-10°) → same sign, 2nd (10-20°) → 5th from sign, 3rd (20-30°) → 9th from sign
        part = int(deg_in_rashi / 10)
        offsets = [0, 4, 8]
        return (rashi_i + offsets[min(part, 2)]) % 12

    elif division == 4:
        # D-4: Chaturthamsha — 4 parts of 7°30' each
        # Parts cycle from sign itself: 1st→same, 2nd→4th, 3rd→7th, 4th→10th
        part = int(deg_in_rashi / 7.5)
        offsets = [0, 3, 6, 9]
        return (rashi_i + offsets[min(part, 3)]) % 12

    elif division == 5:
        # D-5: Panchamsha — 5 parts of 6° each
        # BPHS: Movable signs → Mesha (0), Fixed signs → Vrischika (7), Dual signs → Karka (3)
        part = int(deg_in_rashi / 6.0)
        part = min(part, 4)
        modality = rashi_i % 3  # 0=movable, 1=fixed, 2=dual
        starts = [0, 7, 3]  # Mesha, Vrischika, Karka
        return (starts[modality] + part) % 12

    elif division == 6:
        # D-6: Shashthamsha — 6 parts of 5° each
        # BPHS: Odd signs → from Mesha (0), Even signs → from Tula (6)
        part = int(deg_in_rashi / 5.0)
        part = min(part, 5)
        is_odd = (rashi_i % 2 == 0)
        start = 0 if is_odd else 6  # Mesha for odd, Tula for even
        return (start + part) % 12

    elif division == 7:
        # D-7: Saptamsha — 7 parts of 4°17'8.57"
        # Odd signs count from same sign, Even signs count from 7th sign
        part = int(deg_in_rashi / (30.0 / 7))
        part = min(part, 6)
        is_odd = (rashi_i % 2 == 0)  # 0-indexed
        start = rashi_i if is_odd else (rashi_i + 6) % 12
        return (start + part) % 12

    elif division == 8:
        # D-8: Ashtamsha — 8 parts of 3°45' each
        # BPHS: Movable signs → Mesha (0), Fixed signs → Vrischika (7), Dual signs → Karka (3)
        part = int(deg_in_rashi / 3.75)
        part = min(part, 7)
        modality = rashi_i % 3  # 0=movable, 1=fixed, 2=dual
        starts = [0, 7, 3]  # Mesha, Vrischika, Karka
        return (starts[modality] + part) % 12

    elif division == 9:
        # D-9: Navamsha — 9 parts of 3°20'
        # Fire signs start from Mesha, Earth from Makara, Air from Tula, Water from Karka
        part = int(deg_in_rashi / (30.0 / 9))
        part = min(part, 8)
        elem = _element_simple(rashi_i)
        start = _ELEMENT_START[elem]
        return (start + part) % 12

    elif division == 10:
        # D-10: Dashamsha — 10 parts of 3° each
        # Odd signs start from same sign, Even signs start from 9th sign
        part = int(deg_in_rashi / 3.0)
        part = min(part, 9)
        is_odd = (rashi_i % 2 == 0)
        start = rashi_i if is_odd else (rashi_i + 8) % 12
        return (start + part) % 12

    elif division == 11:
        # D-11: Ekadashamsha (Rudramsha) — 11 parts of 2°43'38" each
        # BPHS: Odd signs count from same sign, Even signs count from next sign
        part = int(deg_in_rashi / (30.0 / 11))
        part = min(part, 10)
        is_odd = (rashi_i % 2 == 0)
        start = rashi_i if is_odd else (rashi_i + 1) % 12
        return (start + part) % 12

    elif division == 12:
        # D-12: Dwadashamsha — 12 parts of 2°30'
        # Start from same sign, each part = next sign
        part = int(deg_in_rashi / 2.5)
        part = min(part, 11)
        return (rashi_i + part) % 12

    elif division == 16:
        # D-16: Shodashamsha — 16 parts of 1°52'30"
        # Movable signs start from Mesha, Fixed from Simha, Dual from Dhanu
        part = int(deg_in_rashi / (30.0 / 16))
        part = min(part, 15)
        modality = rashi_i % 3  # 0=movable, 1=fixed, 2=dual
        starts = [0, 4, 8]
        return (starts[modality] + part) % 12

    elif division == 20:
        # D-20: Vimshamsha — 20 parts of 1°30'
        # Movable→Mesha, Fixed→Dhanu, Dual→Simha
        part = int(deg_in_rashi / 1.5)
        part = min(part, 19)
        modality = rashi_i % 3
        starts = [0, 8, 4]
        return (starts[modality] + part) % 12

    elif division == 24:
        # D-24: Chaturvimshamsha — 24 parts of 1°15'
        # Odd signs start from Simha, Even from Karka
        part = int(deg_in_rashi / 1.25)
        part = min(part, 23)
        is_odd = (rashi_i % 2 == 0)
        start = 4 if is_odd else 3  # Simha / Karka
        return (start + part) % 12

    elif division == 27:
        # D-27: Saptavimshamsha / Bhamsha — 27 parts of 1°6'40"
        # Fire→Mesha, Earth→Karka, Air→Tula, Water→Makara
        part = int(deg_in_rashi / (30.0 / 27))
        part = min(part, 26)
        elem = _element_simple(rashi_i)
        starts = {0: 0, 1: 3, 2: 6, 3: 9}
        return (starts[elem] + part) % 12

    elif division == 30:
        # D-30: Trimshamsha — 5 unequal parts based on sign gender (Parashari)
        # Odd signs: 0-5°=Mesha, 5-10°=Kumbha, 10-18°=Dhanu, 18-25°=Mithuna, 25-30°=Tula
        # Even signs: reverse order
        is_odd = (rashi_i % 2 == 0)
        if is_odd:
            if deg_in_rashi < 5:    return 0   # Mesha
            elif deg_in_rashi < 10: return 10  # Kumbha
            elif deg_in_rashi < 18: return 8   # Dhanu
            elif deg_in_rashi < 25: return 2   # Mithuna
            else:                   return 6   # Tula
        else:
            if deg_in_rashi < 5:    return 6   # Tula
            elif deg_in_rashi < 12: return 2   # Mithuna
            elif deg_in_rashi < 20: return 8   # Dhanu
            elif deg_in_rashi < 25: return 10  # Kumbha
            else:                   return 0   # Mesha

    elif division == 40:
        # D-40: Khavedamsha — 40 parts of 0°45'
        # Odd signs start from Mesha, Even from Tula
        part = int(deg_in_rashi / 0.75)
        part = min(part, 39)
        is_odd = (rashi_i % 2 == 0)
        start = 0 if is_odd else 6
        return (start + part) % 12

    elif division == 45:
        # D-45: Akshavedamsha — 45 parts of 0°40'
        # Movable→Mesha, Fixed→Simha, Dual→Dhanu
        part = int(deg_in_rashi / (30.0 / 45))
        part = min(part, 44)
        modality = rashi_i % 3
        starts = [0, 4, 8]
        return (starts[modality] + part) % 12

    elif division == 60:
        # D-60: Shashtiamsha — 60 parts of 0°30'
        # Start from same sign, cycle all 12 rashis 5 times
        part = int(deg_in_rashi / 0.5)
        part = min(part, 59)
        return (rashi_i + part) % 12

    else:
        # Generic equal division: divide 30° into N parts, cycle from same sign
        part = int(deg_in_rashi / (30.0 / division))
        part = min(part, division - 1)
        return (rashi_i + part) % 12


def calc_varga_chart(
    grahas: dict,
    lagna_lon: float,
    division: int,
) -> dict:
    """
    Calculate full varga chart for given division.
    grahas = internal dict from calc_kundali with 'longitude', 'retro', etc.
    Returns: {lagna: {rashi}, houses: [...], grahas: {name: {rashi, house, status, retro}}}
    """
    # Varga lagna
    varga_lagna_i = calc_varga_rashi(lagna_lon, division)

    # Varga houses (whole sign from varga lagna)
    houses = []
    for i in range(12):
        ri = (varga_lagna_i + i) % 12
        houses.append({
            "house": i + 1,
            "rashi": RASHI_NAMES[ri],
            "lord": RASHI_LORDS[ri],
        })

    # Planet positions in varga
    def get_house(varga_rashi_i: int) -> int:
        return (varga_rashi_i - varga_lagna_i) % 12 + 1

    graha_order = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu"]
    varga_grahas = {}

    for gn in graha_order:
        if gn not in grahas:
            continue
        g = grahas[gn]
        vr = calc_varga_rashi(g["longitude"], division)
        status = (
            "Uchcha (Exalted)" if EXALTATION.get(gn) == vr
            else "Neecha (Debilitated)" if DEBILITATION.get(gn) == vr
            else "Svakshetra (Own)" if RASHI_LORDS.get(vr) == gn
            else "Normal"
        )
        varga_grahas[gn] = {
            "rashi": RASHI_NAMES[vr],
            "house": get_house(vr),
            "status": status,
            "retro": g.get("retro", False),
        }

    info = VARGA_NAMES.get(division, (f"D-{division}", f"Varga-{division}", ""))

    return {
        "division": division,
        "name": info[0],
        "full_name": info[1],
        "signification": info[2],
        "lagna": {"rashi": RASHI_NAMES[varga_lagna_i]},
        "houses": houses,
        "grahas": varga_grahas,
    }
