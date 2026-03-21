"""
Birth Time Rectification Service
Tests candidate birth times within an uncertainty window and scores them
against known life events using Vimshottari dasha alignment.
"""
from datetime import datetime, timedelta
from typing import List, Dict
import swisseph as swe
from api.config import EPHE_PATH, RASHI_NAMES

# ── Dasha constants ────────────────────────────────────────────────────────────
NAKSHATRA_LORD = [
    "Ketu","Shukra","Surya","Chandra","Mangal","Rahu","Guru","Shani","Budh",
    "Ketu","Shukra","Surya","Chandra","Mangal","Rahu","Guru","Shani","Budh",
    "Ketu","Shukra","Surya","Chandra","Mangal","Rahu","Guru","Shani","Budh",
]
DASHA_YEARS = {
    "Ketu":7,"Shukra":20,"Surya":6,"Chandra":10,"Mangal":7,
    "Rahu":18,"Guru":16,"Shani":19,"Budh":17
}
DASHA_ORDER = ["Ketu","Shukra","Surya","Chandra","Mangal","Rahu","Guru","Shani","Budh"]
TOTAL_DASHA_YEARS = sum(DASHA_YEARS.values())  # 120

RASHI_LORDS_IDX = {
    0:"Mangal",1:"Shukra",2:"Budh",3:"Chandra",4:"Surya",5:"Budh",
    6:"Shukra",7:"Mangal",8:"Guru",9:"Shani",10:"Shani",11:"Guru",
}

NAK_NAMES = [
    "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra",
    "Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni","Uttara Phalguni",
    "Hasta","Chitra","Swati","Vishakha","Anuradha","Jyeshtha",
    "Mula","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishtha",
    "Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati",
]

# ── Event type → karaka planets + relevant houses ─────────────────────────────
EVENT_KARAKAS: Dict[str, Dict] = {
    "marriage":      {"planets": ["Shukra", "Guru"],          "houses": [7]},
    "child":         {"planets": ["Guru"],                     "houses": [5]},
    "career_start":  {"planets": ["Surya", "Shani"],          "houses": [10]},
    "career_change": {"planets": ["Shani", "Budh"],           "houses": [6, 10]},
    "property":      {"planets": ["Mangal", "Chandra"],        "houses": [4]},
    "health":        {"planets": ["Shani"],                    "houses": [6, 8]},
    "accident":      {"planets": ["Mangal", "Rahu"],           "houses": [8]},
    "foreign":       {"planets": ["Rahu", "Guru"],             "houses": [9, 12]},
    "loss":          {"planets": ["Shani", "Ketu"],            "houses": [8, 12]},
    "success":       {"planets": ["Guru", "Surya"],            "houses": [1, 9, 10]},
    "education":     {"planets": ["Budh", "Guru"],             "houses": [4, 5]},
    "spiritual":     {"planets": ["Guru", "Ketu"],             "houses": [9, 12]},
    "separation":    {"planets": ["Shani", "Rahu"],            "houses": [7, 12]},
    "financial_loss":{"planets": ["Shani", "Ketu"],            "houses": [8, 12]},
    "financial_gain":{"planets": ["Guru", "Shukra"],           "houses": [2, 11]},
}


def _jd_utc(year, month, day, hour, minute, tz):
    utc_h = hour + minute / 60.0 - tz
    return swe.julday(year, month, day, utc_h)


def _moon_nak_info(jd: float, ayanamsha: float):
    """Return (nakshatra_index 0-26, fraction_elapsed_in_nak 0-1)."""
    fl, _ = swe.calc_ut(jd, swe.MOON, swe.FLG_SWIEPH)
    moon_sid = (fl[0] - ayanamsha) % 360
    nak_width = 360.0 / 27
    nak_i = int(moon_sid / nak_width)
    frac = (moon_sid % nak_width) / nak_width
    return nak_i, frac


def _lagna_rashi(jd: float, lat: float, lon: float, ayanamsha: float) -> int:
    try:
        cusps, ascmc = swe.houses(jd, lat, lon, b"P")
        asc_sid = (ascmc[0] - ayanamsha) % 360
        return int(asc_sid / 30)
    except Exception:
        return 0


def _active_dasha(birth_jd: float, event_year: int, event_month: int, event_day: int,
                  tz: float, nak_i: int, frac: float):
    """Return (mahadasha_lord, antardasha_lord) active on the event date."""
    event_jd = swe.julday(event_year, event_month, event_day, 12.0 - tz)
    days_since_birth = event_jd - birth_jd

    start_lord = NAKSHATRA_LORD[nak_i]
    start_idx = DASHA_ORDER.index(start_lord)
    # Days elapsed & remaining in starting dasha at birth moment
    first_dur = DASHA_YEARS[start_lord] * 365.25
    first_elapsed = frac * first_dur
    first_remaining = first_dur - first_elapsed

    # Walk mahadashas
    maha_lord = start_lord
    maha_start = 0.0
    maha_end = first_remaining
    accumulated = first_remaining
    idx = (start_idx + 1) % 9

    if days_since_birth > first_remaining:
        while accumulated < days_since_birth:
            lord = DASHA_ORDER[idx]
            dur = DASHA_YEARS[lord] * 365.25
            if accumulated + dur > days_since_birth:
                maha_lord = lord
                maha_start = accumulated
                maha_end = accumulated + dur
                break
            accumulated += dur
            idx = (idx + 1) % 9

    # Antardasha within mahadasha
    maha_dur = maha_end - maha_start
    days_into_maha = days_since_birth - maha_start
    maha_idx = DASHA_ORDER.index(maha_lord)
    antar_lord = maha_lord
    antar_acc = 0.0
    for j in range(9):
        al = DASHA_ORDER[(maha_idx + j) % 9]
        al_dur = (DASHA_YEARS[al] / TOTAL_DASHA_YEARS) * maha_dur
        if antar_acc + al_dur > days_into_maha:
            antar_lord = al
            break
        antar_acc += al_dur

    return maha_lord, antar_lord


def _score(lagna_rashi: int, events: List[Dict],
           birth_jd: float, tz: float, nak_i: int, frac: float) -> float:
    if not events:
        return 50.0
    total = 0.0
    for ev in events:
        etype = ev.get("type", "success")
        date_str = ev.get("date", "")
        try:
            d = datetime.strptime(date_str, "%Y-%m-%d")
        except Exception:
            continue
        karakas = EVENT_KARAKAS.get(etype, EVENT_KARAKAS["success"])
        k_planets = karakas["planets"]
        k_houses = karakas["houses"]

        maha, antar = _active_dasha(birth_jd, d.year, d.month, d.day, tz, nak_i, frac)

        s = 0.0
        if maha in k_planets:
            s += 4.0
        if antar in k_planets:
            s += 3.0
        for ri, lord in RASHI_LORDS_IDX.items():
            if lord == maha and (ri - lagna_rashi) % 12 + 1 in k_houses:
                s += 2.0
            if lord == antar and (ri - lagna_rashi) % 12 + 1 in k_houses:
                s += 1.0
        total += s

    max_per_event = 10.0
    return round((total / (len(events) * max_per_event)) * 100, 1)


def rectify_birth_time(
    year: int, month: int, day: int,
    approx_hour: int, approx_minute: int,
    lat: float, lon: float, tz: float,
    uncertainty_minutes: int,
    events: List[Dict],
    ayanamsha_mode: str = "lahiri",
) -> List[Dict]:
    """
    Test birth times every 5 min within ±uncertainty_minutes.
    Returns top candidates sorted by score (desc).
    """
    swe.set_ephe_path(EPHE_PATH)
    sid_map = {"lahiri": swe.SIDM_LAHIRI, "raman": swe.SIDM_RAMAN,
               "kp": swe.SIDM_KRISHNAMURTI, "true_citra": swe.SIDM_TRUE_CITRA}
    swe.set_sid_mode(sid_map.get(ayanamsha_mode, swe.SIDM_LAHIRI))

    ref_jd = _jd_utc(year, month, day, approx_hour, approx_minute, tz)
    ayanamsha = swe.get_ayanamsa_ut(ref_jd)

    step = 5
    candidates = []
    seen_lagnas = set()

    for delta in range(-uncertainty_minutes, uncertainty_minutes + step, step):
        test_dt = datetime(year, month, day, approx_hour, approx_minute) + timedelta(minutes=delta)
        test_jd = _jd_utc(test_dt.year, test_dt.month, test_dt.day,
                           test_dt.hour, test_dt.minute, tz)
        lr = _lagna_rashi(test_jd, lat, lon, ayanamsha)
        nak_i, frac = _moon_nak_info(test_jd, ayanamsha)
        score = _score(lr, events, test_jd, tz, nak_i, frac)

        # Mark first candidate of each lagna change
        lagna_changed = lr not in seen_lagnas
        seen_lagnas.add(lr)

        maha = antar = ""
        if events:
            try:
                d = datetime.strptime(events[0]["date"], "%Y-%m-%d")
                maha, antar = _active_dasha(test_jd, d.year, d.month, d.day,
                                            tz, nak_i, frac)
            except Exception:
                pass

        candidates.append({
            "time": test_dt.strftime("%H:%M"),
            "delta_minutes": delta,
            "lagna_rashi": RASHI_NAMES[lr],
            "lagna_rashi_idx": lr,
            "lagna_changed": lagna_changed,
            "score": score,
            "moon_nakshatra": NAK_NAMES[nak_i],
            "sample_maha": maha,
            "sample_antar": antar,
        })

    candidates.sort(key=lambda x: x["score"], reverse=True)
    return candidates[:12]
