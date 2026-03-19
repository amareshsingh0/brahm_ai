"""
Compatibility router — full Ashtakoot (8-Kuta) 36-Guna matching.

Calculation based on pyswisseph Moon nakshatra and rashi.
Covers Hindu, Buddhist, and Jain traditions (all use the same Nirayana/sidereal Moon system).
Includes: Nadi Dosha, Gana Dosha, Bhakuta Dosha, Mangal Dosha, Rajju Dosha, Vedha Dosha.
"""
from datetime import datetime
from typing import Tuple, Optional, List
from fastapi import APIRouter, HTTPException
from api.models.compatibility import (
    CompatibilityRequest, CompatibilityResponse,
    GunaScore, MangalDosha, DoshaSummary,
    RajjuDosha, VedhaDosha, LifeAreaScore,
)
from api.services.kundali_service import calc_kundali
from api.config import NAKSHATRAS, RASHI_NAMES, RASHI_LORDS

router = APIRouter()

# ── Ashtakoot Lookup Tables ──────────────────────────────────────────────────

_VARNA_BY_NAK = [
    1, 0, 3, 0, 1, 0, 3, 2, 3, 2,
    2, 3, 1, 1, 2, 3, 2, 2, 2, 2,
    2, 0, 1, 0, 3, 2, 3,
]
_VARNA_BY_RASHI = [2, 1, 0, 3, 2, 1, 0, 3, 2, 1, 0, 3]
_VARNA_NAMES = ["Shudra", "Vaishya", "Kshatriya", "Brahmin"]

_GANA = [
    0, 1, 2, 1, 0, 1, 0, 0, 2, 2,
    1, 1, 0, 2, 0, 2, 0, 2, 2, 1,
    1, 0, 2, 2, 1, 1, 0,
]
_GANA_NAMES = ["Deva", "Manushya", "Rakshasa"]
_GANA_SCORE = [
    [6, 5, 0],  # Deva boy
    [5, 6, 0],  # Manushya boy
    [0, 0, 6],  # Rakshasa boy
]

_NADI = [
    0, 1, 2, 2, 1, 0, 0, 1, 2, 2,
    1, 0, 0, 1, 2, 2, 1, 0, 0, 1,
    2, 2, 1, 0, 0, 1, 2,
]
_NADI_NAMES = ["Aadi (Vata)", "Madhya (Pitta)", "Antya (Kapha)"]

_YONI = [
    "Ashwa",   "Gaja",    "Mesha",   "Sarpa",   "Sarpa",   "Shwan",   "Marjara",
    "Mesha",   "Marjara", "Mushaka", "Mushaka", "Gau",     "Mahisha", "Vyaghra",
    "Mahisha", "Vyaghra", "Mriga",   "Mriga",   "Shwan",   "Vanara",  "Nakula",
    "Vanara",  "Simha",   "Ashwa",   "Simha",   "Gau",     "Gaja",
]
_YONI_ENEMIES = {
    frozenset(["Ashwa",   "Mahisha"]),
    frozenset(["Gaja",    "Simha"  ]),
    frozenset(["Mesha",   "Vanara" ]),
    frozenset(["Sarpa",   "Nakula" ]),
    frozenset(["Shwan",   "Mriga"  ]),
    frozenset(["Marjara", "Mushaka"]),
    frozenset(["Gau",     "Vyaghra"]),
}
_YONI_FRIENDS = {
    frozenset(["Ashwa",   "Gaja"   ]),
    frozenset(["Gau",     "Mahisha"]),
    frozenset(["Mesha",   "Mriga"  ]),
    frozenset(["Shwan",   "Vanara" ]),
}
_YONI_LABEL = {
    "Ashwa":"Horse", "Gaja":"Elephant", "Mesha":"Ram", "Sarpa":"Serpent",
    "Shwan":"Dog", "Marjara":"Cat", "Mushaka":"Rat", "Gau":"Cow",
    "Mahisha":"Buffalo", "Vyaghra":"Tiger", "Mriga":"Deer", "Vanara":"Monkey",
    "Nakula":"Mongoose", "Simha":"Lion",
}

_RASHI_VASHYA = {
    0:  [4, 7],    1:  [3, 6],    2:  [5],      3:  [7, 8],
    4:  [6],       5:  [11, 2],   6:  [9],       7:  [3],
    8:  [11],      9:  [10],      10: [0],        11: [9],
}

_FRIENDSHIP = {
    "Surya":  {"f": ["Chandra","Mangal","Guru"],          "n": ["Budh"],                   "e": ["Shukra","Shani"]},
    "Chandra":{"f": ["Surya","Budh"],                      "n": ["Mangal","Guru","Shukra","Shani"], "e": []},
    "Mangal": {"f": ["Surya","Chandra","Guru"],            "n": ["Shukra","Shani"],         "e": ["Budh"]},
    "Budh":   {"f": ["Surya","Shukra"],                    "n": ["Mangal","Shani"],         "e": ["Chandra","Guru"]},
    "Guru":   {"f": ["Surya","Chandra","Mangal"],          "n": ["Shani"],                  "e": ["Budh","Shukra"]},
    "Shukra": {"f": ["Budh","Shani"],                      "n": ["Mangal","Guru"],          "e": ["Surya","Chandra"]},
    "Shani":  {"f": ["Budh","Shukra"],                     "n": ["Guru"],                   "e": ["Surya","Chandra","Mangal"]},
}

# ── Rajju (Rope) — 5 groups ──────────────────────────────────────────────────
# 0=Shiro(Head), 1=Kantha(Neck), 2=Udara(Navel), 3=Kati(Waist), 4=Pada(Feet)
_RAJJU = [
    4, 3, 2, 1, 0, 1, 2, 3, 4, 4,   # 0–9
    3, 2, 1, 0, 1, 2, 3, 4, 4, 3,   # 10–19
    2, 1, 0, 1, 2, 3, 4,            # 20–26
]
_RAJJU_NAMES = ["Shiro (Head)", "Kantha (Neck)", "Udara (Navel)", "Kati (Waist)", "Pada (Feet)"]
_RAJJU_SEVERITY = {0: "High", 1: "High", 2: "Medium", 3: "Medium", 4: "Low"}
_RAJJU_NOTES = {
    0: "Shiro Rajju — both nakshatras fall in the 'Head' group. Classical texts associate this with risk to the husband's longevity. Remedy: Maha Mrityunjaya Japa, Mrityunjaya homa.",
    1: "Kantha Rajju — both nakshatras fall in the 'Neck' group. Classical texts associate this with risk to the wife's longevity. Remedy: Devi Puja, Shanti karma, red silk donation.",
    2: "Udara Rajju — both nakshatras fall in the 'Navel' group. Associated with financial difficulties and poverty in married life. Remedy: Lakshmi Puja, grain and gold donation.",
    3: "Kati Rajju — both nakshatras fall in the 'Waist' group. Associated with unhappiness and possible separation over time. Remedy: Uma-Maheshwara Puja, joint pilgrimage to Shiva temple.",
    4: "Pada Rajju — both nakshatras fall in the 'Feet' group. Associated with restlessness and wandering tendencies. Remedy: Vishnu Sahasranama parayana, Tulsi Vivah.",
}

# ── Vedha (Obstruction) nakshatra pairs ─────────────────────────────────────
_VEDHA_PAIRS = {
    frozenset([0,  17]),  frozenset([1,  16]),  frozenset([2,  15]),
    frozenset([3,  14]),  frozenset([4,  13]),  frozenset([5,  12]),
    frozenset([6,  11]),  frozenset([7,  10]),  frozenset([8,   9]),
    frozenset([18, 26]),  frozenset([19, 25]),  frozenset([20, 24]),
    frozenset([21, 23]),
}


def _prel(p1: str, p2: str) -> int:
    d = _FRIENDSHIP.get(p1, {})
    if p2 in d.get("f", []): return 2
    if p2 in d.get("n", []): return 1
    return 0


# ── Per-Kuta interpretation generators ───────────────────────────────────────

def _interp_varna(score: int, va: str, vb: str) -> str:
    if score == 1:
        if va == vb:
            return f"Perfect spiritual alignment — both share the {va} temperament, creating natural harmony in values, life goals, and spiritual outlook."
        return f"{va} (groom) & {vb} (bride) — spiritually compatible temperaments that complement each other in the journey of life."
    return f"Varna mismatch ({va} × {vb}) — differences in life approach and spiritual temperament may need conscious mutual respect and adjustment."

def _interp_vashya(score: int, ra: str, rb: str) -> str:
    if score == 2:
        if ra == rb:
            return f"Same Moon sign ({ra}) — powerful magnetic attraction and deep natural attunement. This couple feels understood without words."
        return f"{ra} naturally draws and influences {rb} — a bond of strong mutual pull, devotion, and willing companionship."
    if score == 1:
        return f"{rb} gently leads the emotional dynamic — a relationship where the bride's energy guides; both can thrive beautifully with awareness."
    return f"{ra} and {rb} are independent energies — no natural pull exists; mutual attraction must be actively cultivated through shared experiences."

def _interp_tara(score: int, na: str, nb: str) -> str:
    if score == 3:
        return "Exceptional star harmony — both Taras are fully auspicious (Sampat/Kshema/Mitra tier). Luck, health, and prosperity naturally bless this union from all directions."
    if score == 2:
        return "Good Tara compatibility — mostly auspicious birth stars with minor areas of caution. Overall beneficial with natural flow of good fortune."
    if score == 1:
        return f"Mixed Tara ({na} × {nb}) — one directional star is inauspicious. Occasional obstacles in luck or health, but manageable with birth-star deity worship."
    return f"Challenging Tara ({na} × {nb}) — inauspicious stars in both directions. Remedial worship of Janma Nakshatra deities for both partners is strongly recommended."

def _interp_yoni(score: int, ya: str, yb: str) -> str:
    yla = _YONI_LABEL.get(ya, ya)
    ylb = _YONI_LABEL.get(yb, yb)
    if score == 4:
        return f"Perfect Yoni match — both share the {yla} Yoni. The deepest possible physical and instinctual harmony; intimate life will be deeply fulfilling and naturally satisfying."
    if score == 3:
        return f"Friendly Yoni ({yla} × {ylb}) — warm physical compatibility with natural affection and mutual comfort. Intimacy flows easily and joyfully."
    if score == 2:
        return f"Neutral Yoni ({yla} × {ylb}) — a workable physical relationship without strong instinctual pull. Emotional depth and emotional intimacy will compensate well."
    return f"Enemy Yoni ({yla} × {ylb}) — fundamental instinctual incompatibility. Intimate life may face friction and misunderstanding; spiritual practices and honest communication are essential."

def _interp_graha_maitri(score: int, la: str, lb: str) -> str:
    if score == 5:
        if la == lb:
            return f"Identical planetary lords ({la}) — exceptional mental resonance. Both partners think, feel, and perceive the world in deeply similar ways."
        return f"{la} and {lb} are mutual friends — exceptional mental harmony, shared values, similar thought patterns, and natural emotional support for each other."
    if score == 4:
        return f"Strong mental bond ({la} × {lb}) — one planet befriends the other, creating a warm psychological connection and good day-to-day understanding."
    if score == 3:
        return f"Neutral mental relationship ({la} × {lb}) — neither strongly attracted nor repelled mentally. With conscious effort, a stable and intellectually satisfying partnership is very achievable."
    if score == 2:
        return f"Uneven mental dynamic ({la} × {lb}) — one-sided planetary friendship creates some imbalance. One partner may feel more understood; open dialogue bridges this gap."
    if score == 1:
        return f"Mental friction ({la} × {lb}) — different psychological orientations and thought patterns. Patience, active listening, and shared spiritual practice will be key."
    return f"Mental incompatibility ({la} × {lb}) — significantly different psychological dispositions. Requires sustained conscious effort and spiritual practice to build mutual understanding."

def _interp_gana(score: int, ga: str, gb: str) -> str:
    if score == 6:
        return f"Perfect Gana match — both are {ga} Gana. Identical temperaments create effortless harmony in daily life, habits, emotional responses, and long-term life goals."
    if score == 5:
        return f"Compatible temperaments ({ga} × {gb}) — very minor differences, easily resolved with goodwill. Generally harmonious cohabitation and mutual understanding."
    if score == 1:
        return f"Temperament contrast ({ga} × {gb}) — the intensity of Rakshasa energy may occasionally challenge the gentler disposition. Mutual respect and spiritual grounding help greatly."
    return f"Gana Dosha ({ga} × {gb}) — significant temperament clash in fundamental nature. Deep patience, joint spiritual practice, and conscious communication are non-negotiable for harmony."

def _interp_bhakuta(score: int, ra: str, rb: str, pair_str: str = "") -> str:
    if score == 7:
        return f"Auspicious Bhakuta ({ra} × {rb}) — favorable moon sign distance for prosperity, children, family growth, and material and spiritual wellbeing of the couple."
    return f"Bhakuta Dosha — {ra} and {rb} form a {pair_str} position (inauspicious distance). May create obstacles in financial growth, family matters, or health of one partner. Graha Shanti puja recommended."

def _interp_nadi(score: int, nadi_a: str, nadi_b: str, na_name: str, nb_name: str) -> str:
    if score == 8:
        return f"Different Nadi ({nadi_a} × {nadi_b}) — perfect genetic and constitutional compatibility. Health, progeny, and longevity are naturally blessed in this union."
    return f"Nadi Dosha — both have {nadi_a} Nadi ({na_name} × {nb_name}). Constitutional similarity may affect progeny health and marital longevity. Most critical of all 8 doshas; Nadi Nirvana puja strongly recommended."


# ── Individual Kuta scorers ──────────────────────────────────────────────────

def _kuta_varna(na: int, nb: int, ra: int, rb: int, system: str = "both") -> Tuple[int, Optional[int]]:
    s_nak = 1 if _VARNA_BY_NAK[na] >= _VARNA_BY_NAK[nb] else 0
    s_ras = 1 if _VARNA_BY_RASHI[ra] >= _VARNA_BY_RASHI[rb] else 0
    if system == "nakshatra": return s_nak, None
    if system == "rashi":     return s_ras, None
    return s_nak, s_ras


def _kuta_vashya(ra: int, rb: int) -> int:
    if ra == rb: return 2
    if rb in _RASHI_VASHYA.get(ra, []): return 2
    if ra in _RASHI_VASHYA.get(rb, []): return 1
    return 0


def _kuta_tara(na: int, nb: int) -> int:
    GOOD = {0, 1, 2, 4, 6, 8}
    t_ab = ((nb - na) % 27 + 1) % 9
    t_ba = ((na - nb) % 27 + 1) % 9
    half_a = 1.5 if t_ab in GOOD else 0.0
    half_b = 1.5 if t_ba in GOOD else 0.0
    return int(half_a + half_b + 0.5)


def _kuta_yoni(na: int, nb: int) -> Tuple[int, str, str]:
    ya, yb = _YONI[na], _YONI[nb]
    pair = frozenset([ya, yb])
    if ya == yb:              score = 4
    elif pair in _YONI_ENEMIES: score = 0
    elif pair in _YONI_FRIENDS: score = 3
    else:                     score = 2
    return score, ya, yb


def _kuta_graha_maitri(ra: int, rb: int) -> int:
    la, lb = RASHI_LORDS[ra], RASHI_LORDS[rb]
    if la == lb: return 5
    pa, pb = _prel(la, lb), _prel(lb, la)
    total = pa + pb
    if total == 4: return 5
    if total == 3: return 4
    if total == 2:
        return 3 if (pa == 1 and pb == 1) else 2
    if total == 1: return 1
    return 0


def _kuta_gana(na: int, nb: int) -> Tuple[int, bool]:
    ga, gb = _GANA[na], _GANA[nb]
    score = _GANA_SCORE[ga][gb]
    return score, (score == 0)


def _kuta_bhakuta(ra: int, rb: int) -> Tuple[int, bool, str]:
    d_ab = (rb - ra) % 12 + 1
    d_ba = (ra - rb) % 12 + 1
    DOSHA_PAIRS = {(2,12),(12,2),(6,8),(8,6),(5,9),(9,5)}
    pair_str = f"{min(d_ab, d_ba)}-{max(d_ab, d_ba)}"
    if (d_ab, d_ba) not in DOSHA_PAIRS:
        return 7, False, ""
    return 0, True, pair_str


def _kuta_nadi(na: int, nb: int) -> Tuple[int, bool, str]:
    nadi_a, nadi_b = _NADI[na], _NADI[nb]
    if nadi_a != nadi_b: return 8, False, ""
    if na == nb:
        return 8, False, (
            f"Nadi Dosha cancelled — both have same nakshatra ({NAKSHATRAS[na]}), "
            "which nullifies the nadi conflict per Muhurta Chintamani rule"
        )
    return 0, True, ""


def _check_rajju(na: int, nb: int) -> RajjuDosha:
    ra_idx = _RAJJU[na]
    rb_idx = _RAJJU[nb]
    rname_a = _RAJJU_NAMES[ra_idx]
    rname_b = _RAJJU_NAMES[rb_idx]
    if ra_idx != rb_idx:
        return RajjuDosha(
            present=False, rajju_a=rname_a, rajju_b=rname_b,
            severity="None",
            note=f"No Rajju Dosha — {NAKSHATRAS[na]} ({rname_a}) and {NAKSHATRAS[nb]} ({rname_b}) belong to different Rajju groups. This is auspicious."
        )
    return RajjuDosha(
        present=True, rajju_a=rname_a, rajju_b=rname_b,
        severity=_RAJJU_SEVERITY[ra_idx],
        note=_RAJJU_NOTES[ra_idx] + f" ({NAKSHATRAS[na]} & {NAKSHATRAS[nb]} — both {rname_a})"
    )


def _check_vedha(na: int, nb: int) -> VedhaDosha:
    if frozenset([na, nb]) in _VEDHA_PAIRS:
        return VedhaDosha(
            present=True,
            note=(
                f"Vedha Dosha — {NAKSHATRAS[na]} and {NAKSHATRAS[nb]} are an obstructing nakshatra pair. "
                "This combination is said to obstruct the positive qualities of both nakshatras in marriage. "
                "Remedy: Joint recitation of Nakshatra Suktam, separate Nakshatra Shanti pujas for both partners."
            )
        )
    return VedhaDosha(
        present=False,
        note=f"No Vedha Dosha — {NAKSHATRAS[na]} and {NAKSHATRAS[nb]} do not form an obstructing pair. Auspicious."
    )


def _compute_life_areas(v1, v2, v3, v4, v5, v6, v7, v8) -> List[LifeAreaScore]:
    def p(val, mx): return int(val / mx * 100)

    love       = int(p(v4,4)*0.40 + p(v2,2)*0.30 + p(v5,5)*0.30)
    mental     = int(p(v5,5)*0.50 + p(v6,6)*0.30 + p(v1,1)*0.20)
    health     = int(p(v8,8)*0.60 + p(v3,3)*0.40)
    children   = int(p(v7,7)*0.50 + p(v8,8)*0.30 + p(v3,3)*0.20)
    prosperity = int(p(v3,3)*0.40 + p(v7,7)*0.35 + p(v2,2)*0.25)
    spiritual  = int(p(v1,1)*0.40 + p(v6,6)*0.35 + p(v5,5)*0.25)

    def lbl(s):
        if s >= 80: return "Excellent"
        if s >= 55: return "Good"
        if s >= 35: return "Average"
        return "Needs Work"

    return [
        LifeAreaScore(area="Love & Intimacy",    icon="❤️",  score=love,       label=lbl(love)),
        LifeAreaScore(area="Mental Harmony",     icon="🧠",  score=mental,     label=lbl(mental)),
        LifeAreaScore(area="Health & Longevity", icon="🌿",  score=health,     label=lbl(health)),
        LifeAreaScore(area="Children & Family",  icon="👶",  score=children,   label=lbl(children)),
        LifeAreaScore(area="Prosperity & Luck",  icon="✨",  score=prosperity, label=lbl(prosperity)),
        LifeAreaScore(area="Spiritual Bond",     icon="🕉️",  score=spiritual,  label=lbl(spiritual)),
    ]


def _compute_strengths_challenges(
    v1, v2, v3, v4, v5, v6, v7, v8,
    gana_a, gana_b, nadi_a, nadi_b,
    rashi_a, rashi_b, ya, yb, la, lb,
    na_name, nb_name,
    rajju_dosha: bool, vedha_dosha: bool,
):
    strengths, challenges = [], []
    yla = _YONI_LABEL.get(ya, ya)
    ylb = _YONI_LABEL.get(yb, yb)

    if v8 == 8:
        strengths.append(f"Different Nadi ({nadi_a.split()[0]} × {nadi_b.split()[0]}) — excellent genetic & health compatibility")
    else:
        challenges.append(f"Nadi Dosha (both {nadi_a.split()[0]}) — constitutional similarity may affect progeny & health")

    if v5 >= 4:
        strengths.append(f"Strong Graha Maitri ({la} × {lb}) — deep mental compatibility & natural understanding")
    elif v5 <= 1:
        challenges.append(f"Weak Graha Maitri ({la} × {lb}) — different psychological dispositions; requires patience")

    if v6 == 6:
        strengths.append(f"Same Gana ({gana_a}) — identical temperaments, effortless daily harmony")
    elif v6 == 0:
        challenges.append(f"Gana Dosha ({gana_a} × {gana_b}) — temperament clash needs spiritual grounding")

    if v3 == 3:
        strengths.append(f"Perfect Tara ({na_name} × {nb_name}) — luck & wellbeing flow naturally in both directions")
    elif v3 == 0:
        challenges.append(f"Inauspicious Tara ({na_name} × {nb_name}) — obstacles in luck & health; birth-star worship advised")

    if v7 == 7:
        strengths.append(f"Auspicious Bhakuta ({rashi_a} × {rashi_b}) — favorable for prosperity & family growth")
    else:
        challenges.append(f"Bhakuta Dosha ({rashi_a} × {rashi_b}) — potential obstacles in material & family matters")

    if v4 >= 3:
        strengths.append(f"Excellent Yoni ({yla} × {ylb}) — natural physical harmony & deep intimacy")
    elif v4 == 0:
        challenges.append(f"Yoni Dosha ({yla} × {ylb}) — instinctual incompatibility in intimate life")

    if v2 == 2 and rashi_a != rashi_b:
        strengths.append(f"Strong Vashya ({rashi_a} → {rashi_b}) — natural attraction & devoted partnership")
    elif v2 == 0:
        challenges.append(f"No Vashya ({rashi_a} × {rashi_b}) — independent energies; attraction needs cultivation")

    if v1 == 1:
        strengths.append("Compatible Varna — aligned spiritual temperaments & shared life values")

    if rajju_dosha:
        challenges.append(f"Rajju Dosha — both nakshatras in same Rajju group; classical texts advise remedies")

    if vedha_dosha:
        challenges.append(f"Vedha Dosha — {na_name} & {nb_name} form an obstructing pair; Nakshatra Shanti recommended")

    return strengths, challenges


# ── Main endpoint ────────────────────────────────────────────────────────────

@router.post("/compatibility", response_model=CompatibilityResponse)
def compatibility(req: CompatibilityRequest):
    charts = []
    for person in [req.person_a, req.person_b]:
        try:
            dt = datetime.strptime(f"{person.date} {person.time}", "%Y-%m-%d %H:%M")
        except ValueError as e:
            raise HTTPException(status_code=422, detail=str(e))
        data, err = calc_kundali(
            year=dt.year, month=dt.month, day=dt.day,
            hour=dt.hour, minute=dt.minute,
            lat=person.lat, lon=person.lon, tz=person.tz,
        )
        if err:
            raise HTTPException(status_code=500, detail=err)
        charts.append(data)

    def moon_info(chart):
        chandra    = chart["grahas"]["Chandra"]
        nak_name   = chandra["nakshatra"]
        rashi_name = chandra["rashi"]
        nak_idx    = NAKSHATRAS.index(nak_name)   if nak_name   in NAKSHATRAS   else 0
        rashi_idx  = RASHI_NAMES.index(rashi_name) if rashi_name in RASHI_NAMES else 0
        return nak_idx, rashi_idx, nak_name, rashi_name

    na, ra, nak_a, rashi_a = moon_info(charts[0])
    nb, rb, nak_b, rashi_b = moon_info(charts[1])

    # ── 8 Kutas ──────────────────────────────────────────────────────────────
    v1, v1_alt        = _kuta_varna(na, nb, ra, rb, req.varna_system)
    v2                = _kuta_vashya(ra, rb)
    v3                = _kuta_tara(na, nb)
    v4, ya, yb        = _kuta_yoni(na, nb)
    v5                = _kuta_graha_maitri(ra, rb)
    v6, gana_dosha    = _kuta_gana(na, nb)
    v7, bhakuta_dosha, bhakuta_info = _kuta_bhakuta(ra, rb)
    v8, nadi_dosha, nadi_cancel     = _kuta_nadi(na, nb)

    la, lb = RASHI_LORDS[ra], RASHI_LORDS[rb]

    # Nadi same-rashi cancellation
    if nadi_dosha and ra == rb:
        nadi_dosha = False
        v8 = 8
        nadi_cancel = f"Nadi Dosha cancelled — both Moon in {rashi_a} (same rashi rule)"

    # Gana cancellation: if Graha Maitri = 5
    if gana_dosha and v5 == 5:
        gana_dosha = False
        v6 = 6

    # ── Extra doshas ─────────────────────────────────────────────────────────
    rajju = _check_rajju(na, nb)
    vedha = _check_vedha(na, nb)

    # ── Per-kuta interpretations ──────────────────────────────────────────────
    va_name = _VARNA_NAMES[_VARNA_BY_NAK[na]]
    vb_name = _VARNA_NAMES[_VARNA_BY_NAK[nb]]

    gunas = [
        GunaScore(name="Varna",        score=v1, max=1,
                  desc="Spiritual compatibility — temperament & life values",
                  interpretation=_interp_varna(v1, va_name, vb_name),
                  alt_score=v1_alt),
        GunaScore(name="Vashya",       score=v2, max=2,
                  desc="Mutual attraction, control & devotion",
                  interpretation=_interp_vashya(v2, rashi_a, rashi_b)),
        GunaScore(name="Tara",         score=v3, max=3,
                  desc="Birth star luck, health & prosperity",
                  interpretation=_interp_tara(v3, nak_a, nak_b)),
        GunaScore(name="Yoni",         score=v4, max=4,
                  desc=f"Physical compatibility — {_YONI_LABEL.get(ya,ya)} × {_YONI_LABEL.get(yb,yb)}",
                  interpretation=_interp_yoni(v4, ya, yb)),
        GunaScore(name="Graha Maitri", score=v5, max=5,
                  desc="Planetary friendship — mental harmony & values",
                  interpretation=_interp_graha_maitri(v5, la, lb)),
        GunaScore(name="Gana",         score=v6, max=6,
                  desc="Temperament — Deva / Manushya / Rakshasa nature",
                  interpretation=_interp_gana(v6, _GANA_NAMES[_GANA[na]], _GANA_NAMES[_GANA[nb]])),
        GunaScore(name="Bhakuta",      score=v7, max=7,
                  desc="Moon sign distance — prosperity & family growth",
                  interpretation=_interp_bhakuta(v7, rashi_a, rashi_b, bhakuta_info)),
        GunaScore(name="Nadi",         score=v8, max=8,
                  desc="Constitutional compatibility — health & progeny",
                  interpretation=_interp_nadi(v8, _NADI_NAMES[_NADI[na]], _NADI_NAMES[_NADI[nb]], nak_a, nak_b)),
    ]

    total = sum(g.score for g in gunas)
    pct   = int(total / 36 * 100)

    if pct >= 72:
        verdict = "Excellent Match"
        verdict_detail = (
            f"Exceptional compatibility — {total}/36 gunas ({pct}%). "
            "Strong harmony across all dimensions of life. "
            "This union is highly favoured by the stars for a long, prosperous, and spiritually fulfilling life together."
        )
    elif pct >= 55:
        verdict = "Good Match"
        verdict_detail = (
            f"Good compatibility — {total}/36 gunas ({pct}%). "
            "A harmonious relationship with minor differences that can be navigated with mutual respect. "
            "With understanding and love, this becomes a strong and lasting bond."
        )
    elif pct >= 36:
        verdict = "Average Match"
        verdict_detail = (
            f"Moderate compatibility — {total}/36 gunas ({pct}%). "
            "The relationship can work well with effort and proper remedies for any doshas present. "
            "Consulting a Vedic astrologer for personalised puja and gemstone guidance is recommended."
        )
    else:
        verdict = "Needs Remedies"
        verdict_detail = (
            f"Low guna score — {total}/36 gunas ({pct}%). "
            "Significant doshas are present. This pairing requires careful consideration and spiritual remedies. "
            "Shanti pujas, Navagraha homam, and sustained mutual devotion can reduce the impact considerably. "
            "An experienced Vedic astrologer should be consulted before proceeding."
        )

    # ── Mangal Dosha ─────────────────────────────────────────────────────────
    def has_mangal(chart) -> bool:
        return chart["grahas"]["Mangal"]["house"] in [1, 2, 4, 7, 8, 12]

    mangal_a = has_mangal(charts[0])
    mangal_b = has_mangal(charts[1])
    both_mangalik = mangal_a and mangal_b

    # ── Life Areas ────────────────────────────────────────────────────────────
    life_areas = _compute_life_areas(v1, v2, v3, v4, v5, v6, v7, v8)

    # ── Strengths & Challenges ────────────────────────────────────────────────
    strengths, challenges = _compute_strengths_challenges(
        v1, v2, v3, v4, v5, v6, v7, v8,
        _GANA_NAMES[_GANA[na]], _GANA_NAMES[_GANA[nb]],
        _NADI_NAMES[_NADI[na]], _NADI_NAMES[_NADI[nb]],
        rashi_a, rashi_b, ya, yb, la, lb, nak_a, nak_b,
        rajju.present, vedha.present,
    )

    # ── Dosha Summary ─────────────────────────────────────────────────────────
    doshas: list = []

    if nadi_dosha:
        doshas.append(DoshaSummary(
            name="Nadi Dosha", present=True, severity="High", cancellation=None,
            note=(
                f"Both have {_NADI_NAMES[_NADI[na]]} nadi. "
                "Most critical of all doshas — linked to health issues, progeny concerns, "
                "and potentially short marriage life. "
                "Remedies: Nadi Nirvana puja, Maha Mrityunjaya Japa (125,000 times), "
                "gold and silver donation, Kashi Vishwanath darshan."
            )
        ))
    elif nadi_cancel:
        doshas.append(DoshaSummary(
            name="Nadi Dosha", present=False, severity="None",
            cancellation=nadi_cancel,
            note="Nadi Dosha present but cancelled by classical Nadi Nirvana exception rule."
        ))

    if gana_dosha:
        ga_name = _GANA_NAMES[_GANA[na]]
        gb_name = _GANA_NAMES[_GANA[nb]]
        ga_set = {_GANA[na], _GANA[nb]}
        sev = "High" if 2 in ga_set else "Medium"
        doshas.append(DoshaSummary(
            name="Gana Dosha", present=True, severity=sev, cancellation=None,
            note=(
                f"{req.person_a.name or 'Person A'} — {ga_name} Gana ({nak_a}); "
                f"{req.person_b.name or 'Person B'} — {gb_name} Gana ({nak_b}). "
                "Gana mismatch causes temperament conflict — "
                f"{'involves Rakshasa Gana: most severe.' if sev == 'High' else 'may cause friction and misunderstandings.'} "
                "Note: Gana Dosha is cancelled if Graha Maitri = 5. "
                "Remedies: Gana Dosha Nivarana puja, joint Shiva–Parvati worship, Uma-Maheshwara Vrat."
            )
        ))

    if bhakuta_dosha:
        doshas.append(DoshaSummary(
            name="Bhakuta Dosha", present=True, severity="Medium", cancellation=None,
            note=(
                f"Moon signs form a {bhakuta_info} position (dosha combination). "
                "May cause separation, financial problems, or health issues for one partner. "
                "Remedies: Graha Shanti puja, Rudrabhisheka, donation of white items on Monday."
            )
        ))

    if rajju.present:
        doshas.append(DoshaSummary(
            name="Rajju Dosha", present=True, severity=rajju.severity, cancellation=None,
            note=rajju.note
        ))
    else:
        doshas.append(DoshaSummary(
            name="Rajju Dosha", present=False, severity="None",
            cancellation="Different Rajju groups — no obstruction",
            note=rajju.note
        ))

    if vedha.present:
        doshas.append(DoshaSummary(
            name="Vedha Dosha", present=True, severity="Medium", cancellation=None,
            note=vedha.note
        ))

    if mangal_a or mangal_b:
        if both_mangalik:
            doshas.append(DoshaSummary(
                name="Mangal Dosha", present=False, severity="None",
                cancellation="Both partners are Mangalik — Mangal Dosha cancels (Anulom Pratilom rule)",
                note="No remedies required for Mangal Dosha when both partners have it."
            ))
        else:
            who = req.person_a.name or "Person A" if mangal_a else req.person_b.name or "Person B"
            doshas.append(DoshaSummary(
                name="Mangal Dosha", present=True, severity="Medium", cancellation=None,
                note=(
                    f"{who} has Mars (Mangal) in houses 1/2/4/7/8/12. "
                    "Associated with relationship tension, separation tendencies, or aggression. "
                    "Remedies: Mangal Shanti homa, Kuja Dosha Nivarana puja, "
                    "red coral gemstone (after chart analysis), "
                    "or matching with another Mangalik partner."
                )
            ))

    if v4 == 0:
        doshas.append(DoshaSummary(
            name="Yoni Dosha", present=True, severity="Medium", cancellation=None,
            note=(
                f"Enemy Yoni pair: {_YONI_LABEL.get(ya,ya)} and {_YONI_LABEL.get(yb,yb)}. "
                "Physical incompatibility and mutual friction in intimate life. "
                "Remedies: Kamadeva puja, Shringara puja, Vashya tantra vidhi (under guidance)."
            )
        ))

    return CompatibilityResponse(
        total_score=total,
        max_score=36,
        percentage=pct,
        verdict=verdict,
        verdict_detail=verdict_detail,
        gunas=gunas,
        mangal_dosha=MangalDosha(person_a=mangal_a, person_b=mangal_b),
        nakshatra_a=nak_a,
        nakshatra_b=nak_b,
        rashi_a=rashi_a,
        rashi_b=rashi_b,
        gana_a=_GANA_NAMES[_GANA[na]],
        gana_b=_GANA_NAMES[_GANA[nb]],
        nadi_a=_NADI_NAMES[_NADI[na]],
        nadi_b=_NADI_NAMES[_NADI[nb]],
        varna_a=_VARNA_NAMES[_VARNA_BY_NAK[na]],
        varna_b=_VARNA_NAMES[_VARNA_BY_NAK[nb]],
        yoni_a=f"{ya} ({_YONI_LABEL.get(ya,ya)})",
        yoni_b=f"{yb} ({_YONI_LABEL.get(yb,yb)})",
        rajju_dosha=rajju,
        vedha_dosha=vedha,
        life_areas=life_areas,
        strengths=strengths,
        challenges=challenges,
        dosha_summary=doshas,
    )
