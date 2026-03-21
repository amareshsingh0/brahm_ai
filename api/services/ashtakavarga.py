"""
Ashtakavarga Calculator — SAV (Sarvashtakavarga) and BAV (Bhinnashtakavarga).
Classical Parashari method for 7 planets (Sun through Saturn).
"""
from api.config import RASHI_NAMES, RASHI_LORDS

# ── BAV Benefic Point Tables ─────────────────────────────────────────────────
# For each planet, the houses from each reference point (Lagna, Sun, Moon, Mars,
# Mercury, Jupiter, Venus, Saturn) where it contributes a bindu (1 point).
# Source: Brihat Parashara Hora Shastra, Chapter 66-72.
# Key: planet -> list of 8 lists (one per reference: [from_Sun, from_Moon, from_Mars,
#   from_Mercury, from_Jupiter, from_Venus, from_Saturn, from_Lagna])
# Each inner list = houses (1-12) where the planet gives a bindu from that reference.

BAV_TABLES = {
    "Surya": [
        [1, 2, 4, 7, 8, 9, 10, 11],       # from Sun
        [3, 6, 10, 11],                     # from Moon
        [1, 2, 4, 7, 8, 9, 10, 11],       # from Mars
        [3, 5, 6, 9, 10, 11, 12],          # from Mercury
        [5, 6, 9, 11],                      # from Jupiter
        [6, 7, 12],                         # from Venus
        [1, 2, 4, 7, 8, 9, 10, 11],       # from Saturn
        [3, 4, 6, 10, 11, 12],            # from Lagna
    ],
    "Chandra": [
        [3, 6, 7, 8, 10, 11],              # from Sun
        [1, 3, 6, 7, 10, 11],              # from Moon
        [2, 3, 5, 6, 9, 10, 11],           # from Mars
        [1, 3, 4, 5, 7, 8, 10, 11],       # from Mercury
        [1, 4, 7, 8, 10, 11, 12],          # from Jupiter
        [3, 4, 5, 7, 9, 10, 11],           # from Venus
        [3, 5, 6, 11],                      # from Saturn
        [3, 6, 10, 11],                    # from Lagna
    ],
    "Mangal": [
        [3, 5, 6, 10, 11],                 # from Sun
        [3, 6, 11],                         # from Moon
        [1, 2, 4, 7, 8, 10, 11],          # from Mars
        [3, 5, 6, 11],                     # from Mercury
        [6, 10, 11, 12],                    # from Jupiter
        [6, 8, 11, 12],                    # from Venus
        [1, 4, 7, 8, 9, 10, 11],          # from Saturn
        [1, 3, 6, 10, 11],                # from Lagna
    ],
    "Budh": [
        [5, 6, 9, 11, 12],                 # from Sun
        [2, 4, 6, 8, 10, 11],              # from Moon
        [1, 2, 4, 7, 8, 9, 10, 11],       # from Mars
        [1, 3, 5, 6, 9, 10, 11, 12],      # from Mercury
        [6, 8, 11, 12],                     # from Jupiter
        [1, 2, 3, 4, 5, 8, 9, 11],        # from Venus
        [1, 2, 4, 7, 8, 9, 10, 11],       # from Saturn
        [1, 2, 4, 6, 8, 10, 11],          # from Lagna
    ],
    "Guru": [
        [1, 2, 3, 4, 7, 8, 9, 10, 11],    # from Sun
        [2, 5, 7, 9, 11],                   # from Moon
        [1, 2, 4, 7, 8, 10, 11],           # from Mars
        [1, 2, 4, 5, 6, 9, 10, 11],       # from Mercury
        [1, 2, 3, 4, 7, 8, 10, 11],       # from Jupiter
        [2, 5, 6, 9, 10, 11],              # from Venus
        [3, 5, 6, 12],                      # from Saturn
        [1, 2, 4, 5, 6, 7, 9, 10, 11],   # from Lagna
    ],
    "Shukra": [
        [8, 11, 12],                        # from Sun
        [1, 2, 3, 4, 5, 8, 9, 11, 12],    # from Moon
        [3, 5, 6, 9, 11, 12],              # from Mars
        [3, 5, 6, 9, 11],                  # from Mercury
        [5, 8, 9, 10, 11],                  # from Jupiter
        [1, 2, 3, 4, 5, 8, 9, 10, 11],    # from Venus
        [3, 4, 5, 8, 9, 10, 11],           # from Saturn
        [1, 2, 3, 4, 5, 8, 9, 11],        # from Lagna
    ],
    "Shani": [
        [1, 2, 4, 7, 8, 10, 11],           # from Sun
        [3, 6, 11],                          # from Moon
        [3, 5, 6, 10, 11, 12],             # from Mars
        [6, 8, 9, 10, 11, 12],             # from Mercury
        [5, 6, 11, 12],                      # from Jupiter
        [6, 11, 12],                        # from Venus
        [3, 5, 6, 11],                      # from Saturn
        [1, 3, 4, 6, 10, 11],             # from Lagna
    ],
}

# Reference planet order matching BAV_TABLES inner list indices
_REF_ORDER = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Lagna"]
_PLANETS_7 = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani"]


def calc_ashtakavarga(grahas: dict, lagna_rashi_i: int) -> dict:
    """
    Calculate Bhinnashtakavarga (BAV) for each of 7 planets
    and Sarvashtakavarga (SAV) — sum of all BAVs per rashi.

    grahas: dict with graha name -> {"rashi": int, ...}
    lagna_rashi_i: lagna rashi index (0-11)

    Returns: {
        bav: { planet: [12 ints] },   # points per rashi 0-11
        sav: [12 ints],                # total SAV per rashi
        sav_total: int,                # should be 337 always
    }
    """
    # Get rashi indices for reference points
    ref_rashis = {}
    for gn in _PLANETS_7:
        ref_rashis[gn] = grahas[gn]["rashi"]
    ref_rashis["Lagna"] = lagna_rashi_i

    bav = {}
    sav = [0] * 12

    for planet in _PLANETS_7:
        planet_bav = [0] * 12
        table = BAV_TABLES[planet]

        for ref_idx, ref_name in enumerate(_REF_ORDER):
            ref_rashi = ref_rashis[ref_name]
            benefic_houses = table[ref_idx]

            for house_num in benefic_houses:
                # house_num (1-12) from reference rashi
                target_rashi = (ref_rashi + house_num - 1) % 12
                planet_bav[target_rashi] += 1

        bav[planet] = planet_bav
        for i in range(12):
            sav[i] += planet_bav[i]

    # Reduced Ashtakavarga (Trikona Shodhana - simplified)
    # For each planet's BAV: reduce by subtracting minimum of (rashi1, rashi5, rashi9) from each
    reduced_bav = {}
    for planet, points in bav.items():
        reduced = points[:]
        # Trikona groups: (0,4,8), (1,5,9), (2,6,10), (3,7,11)
        for base in range(4):
            trikona = [base, base+4, base+8]
            min_val = min(reduced[i] for i in trikona)
            for i in trikona:
                reduced[i] -= min_val
        # Ekadhipatya shodhana (planets that rule 2 rashis)
        # Simplified: just return trikona-reduced for now
        reduced_bav[planet] = reduced

    reduced_sav = [0] * 12
    for pts in reduced_bav.values():
        for i in range(12):
            reduced_sav[i] += pts[i]

    return {
        "bav": {
            planet: {
                "points": points,
                "rashi_names": RASHI_NAMES,
                "total": sum(points),
            }
            for planet, points in bav.items()
        },
        "sav": {
            "points": sav,
            "rashi_names": RASHI_NAMES,
            "total": sum(sav),
        },
        "reduced_bav": {
            planet: {"points": pts, "total": sum(pts)}
            for planet, pts in reduced_bav.items()
        },
        "reduced_sav": {
            "points": reduced_sav,
            "total": sum(reduced_sav),
        },
    }
