"""
Eclipse (Grahan) calendar — uses pyswisseph eclipse functions.
Returns Sparsha/Madhya/Moksha timings, Sutak period, magnitude,
festival conflict detection, and Grahan nakshatra/rashi.
"""
from fastapi import APIRouter, HTTPException, Query
from api.config import EPHE_PATH, NAKSHATRAS, RASHI_NAMES

router = APIRouter()

_EFFECTS = {
    "Total Solar":   "Powerful transformation. Surya Grahan intensifies spiritual practices. Mantra japa yields amplified results during eclipse.",
    "Annular Solar": "New cycle begins. Ring of fire visible. Avoid starting new ventures. Strong time for introspection and prayer.",
    "Partial Solar": "Subtle shifts in external circumstances. Chant Aditya Hridaya Stotra. Observe silence during peak.",
    "Total Lunar":   "Chandra Grahan — emotional and karmic cleansing. Deep introspection, Vishnu Sahasranama, and water offerings recommended.",
    "Partial Lunar": "Inner reflection and release. Light a ghee lamp, recite mantras. Release what no longer serves.",
    "Penumbral":     "Gentle cosmic shift. Heightened sensitivity. Good for meditation. Some traditions observe Sutak.",
}

# Sutak duration per eclipse type (hours before Sparsha)
_SUTAK_HOURS = {
    "Total Solar": 12, "Annular Solar": 12, "Partial Solar": 12,
    "Total Lunar": 9,  "Partial Lunar": 9,  "Penumbral": 0,   # Penumbral: no Sutak per most traditions
}


def _jd_to_ist_str(jd: float, tz: float = 5.5) -> str:
    """Convert JD to local time string HH:MM."""
    try:
        import swisseph as swe
        jd_loc = jd + tz / 24.0
        y, m, d, h = swe.revjul(jd_loc)
        hr = int(h)
        mn = int((h - hr) * 60)
        return f"{hr:02d}:{mn:02d}"
    except Exception:
        return "N/A"


def _jd_to_date_str(jd: float, tz: float = 5.5) -> str:
    try:
        import swisseph as swe
        jd_loc = jd + tz / 24.0
        y, m, d, h = swe.revjul(jd_loc)
        return f"{int(y):04d}-{int(m):02d}-{int(d):02d}"
    except Exception:
        return "N/A"


def _moon_nakshatra_rashi(jd: float) -> dict:
    """Get Moon nakshatra and rashi at given JD."""
    try:
        import swisseph as swe
        swe.set_sid_mode(swe.SIDM_LAHIRI)
        lon = swe.calc_ut(jd, swe.MOON, swe.FLG_SWIEPH | swe.FLG_SIDEREAL)[0][0]
        nak_i = int(lon / (360 / 27))
        rashi_i = int(lon / 30)
        return {
            "nakshatra": NAKSHATRAS[nak_i] if nak_i < len(NAKSHATRAS) else "N/A",
            "rashi": RASHI_NAMES[rashi_i] if rashi_i < len(RASHI_NAMES) else "N/A",
        }
    except Exception:
        return {"nakshatra": "N/A", "rashi": "N/A"}


@router.get("/grahan")
def grahan(year: int = Query(default=2026), tz: float = Query(default=5.5)):
    try:
        import swisseph as swe
        swe.set_ephe_path(EPHE_PATH)

        eclipses = []
        jd_start = swe.julday(year, 1, 1, 0.0)
        jd_end   = swe.julday(year + 1, 1, 1, 0.0)

        # ── Solar eclipses ────────────────────────────────────────────────
        jd = jd_start
        while jd < jd_end:
            ret = swe.sol_eclipse_when_glob(jd, swe.FLG_SWIEPH)
            if ret[0] < 0: break
            madhya_jd = ret[1][0]   # maximum eclipse
            if madhya_jd >= jd_end: break

            etype = ret[0]
            if etype & swe.ECL_TOTAL:     label = "Total Solar"
            elif etype & swe.ECL_ANNULAR: label = "Annular Solar"
            else:                          label = "Partial Solar"

            # ret[1] indices: 0=max, 1=C1/first external, 2=C2/first internal,
            #                  3=C3/second internal, 4=C4/second external, 5+=sunrise/sunset
            sparsha_jd = ret[1][1] if ret[1][1] > 0 else madhya_jd - 1.5/24
            _moksha    = ret[1][4] if ret[1][4] > 0 else 0
            # Safety: moksha must be after madhya; if not, try index 5 then symmetric fallback
            if _moksha > madhya_jd:
                moksha_jd = _moksha
            elif len(ret[1]) > 5 and ret[1][5] > madhya_jd:
                moksha_jd = ret[1][5]
            else:
                moksha_jd = madhya_jd + (madhya_jd - sparsha_jd)
            duration   = round((moksha_jd - sparsha_jd) * 24 * 60)

            sutak_hrs  = _SUTAK_HOURS.get(label, 12)
            sutak_start_jd = sparsha_jd - sutak_hrs / 24.0

            moon_info  = _moon_nakshatra_rashi(madhya_jd)
            date_str   = _jd_to_date_str(madhya_jd, tz)

            eclipses.append({
                "date":            date_str,
                "type":            label,
                "sparsha":         _jd_to_ist_str(sparsha_jd, tz),
                "madhya":          _jd_to_ist_str(madhya_jd, tz),
                "moksha":          _jd_to_ist_str(moksha_jd, tz),
                "duration_minutes": max(1, duration),
                "sutak_start":     _jd_to_ist_str(sutak_start_jd, tz) if sutak_hrs > 0 else None,
                "sutak_hours":     sutak_hrs,
                "nakshatra":       moon_info["nakshatra"],
                "rashi":           moon_info["rashi"],
                "spiritual_effect": _EFFECTS.get(label, ""),
                "festival_conflict": None,  # filled below
            })
            jd = madhya_jd + 20

        # ── Lunar eclipses ────────────────────────────────────────────────
        jd = jd_start
        while jd < jd_end:
            ret = swe.lun_eclipse_when(jd, swe.FLG_SWIEPH)
            if ret[0] < 0: break
            madhya_jd = ret[1][0]
            if madhya_jd >= jd_end: break

            etype = ret[0]
            if etype & swe.ECL_TOTAL:     label = "Total Lunar"
            elif etype & swe.ECL_PARTIAL: label = "Partial Lunar"
            else:                          label = "Penumbral"

            # ret[1] indices for lunar: 0=max, 1=partial begin (Sparsha),
            #                            2=total begin, 3=total end, 4=partial end (Moksha),
            #                            6=penumbral begin, 7=penumbral end
            if label == "Penumbral":
                # Penumbral: indices 5=penumbral begin, 6=penumbral end
                sparsha_jd = ret[1][5] if ret[1][5] > 0 else madhya_jd - 2/24
                moksha_jd  = ret[1][6] if ret[1][6] > 0 else madhya_jd + 2/24
            else:
                # Partial/Total: 1=partial begin (Sparsha), 4=partial end (Moksha)
                sparsha_jd = ret[1][1] if ret[1][1] > 0 else madhya_jd - 2/24
                _moksha    = ret[1][4] if ret[1][4] > 0 else 0
                # Safety: moksha must be after madhya; if not, try index 5 then symmetric fallback
                if _moksha > madhya_jd:
                    moksha_jd = _moksha
                elif ret[1][5] > madhya_jd:
                    moksha_jd = ret[1][5]
                else:
                    moksha_jd = madhya_jd + (madhya_jd - sparsha_jd)

            duration  = round((moksha_jd - sparsha_jd) * 24 * 60)
            sutak_hrs = _SUTAK_HOURS.get(label, 9)
            sutak_start_jd = sparsha_jd - sutak_hrs / 24.0

            moon_info = _moon_nakshatra_rashi(madhya_jd)
            date_str  = _jd_to_date_str(madhya_jd, tz)

            eclipses.append({
                "date":            date_str,
                "type":            label,
                "sparsha":         _jd_to_ist_str(sparsha_jd, tz),
                "madhya":          _jd_to_ist_str(madhya_jd, tz),
                "moksha":          _jd_to_ist_str(moksha_jd, tz),
                "duration_minutes": max(1, duration),
                "sutak_start":     _jd_to_ist_str(sutak_start_jd, tz) if sutak_hrs > 0 else None,
                "sutak_hours":     sutak_hrs,
                "nakshatra":       moon_info["nakshatra"],
                "rashi":           moon_info["rashi"],
                "spiritual_effect": _EFFECTS.get(label, ""),
                "festival_conflict": None,
            })
            jd = madhya_jd + 20

        eclipses.sort(key=lambda x: x["date"])

        # ── Festival conflict detection ───────────────────────────────────
        # Check if any eclipse falls on a major festival tithi
        _FESTIVAL_CONFLICTS = {
            # Purnima eclipses affect: Holika Dahan, Guru Purnima, Buddha Purnima, Raksha Bandhan
            "Total Lunar":   ["Purnima festivals (Holika Dahan, Guru Purnima, Raksha Bandhan) are directly affected. "
                              "Holika Dahan: must be performed before Sutak starts OR per tradition guidance. "
                              "Consult local Panchangam for exact observance time."],
            "Partial Lunar": ["Purnima festivals may be affected. Check if Sutak overlaps festival Pradosh window."],
            "Penumbral":     ["Minor effect. Some traditions observe Sutak; most proceed normally."],
            "Total Solar":   ["Amavasya/Chaturdashi festivals affected. Avoid new ventures during eclipse window."],
            "Annular Solar": ["Solar festival timings affected. Check Sutak window."],
            "Partial Solar": ["Minor festival impact. Observe Sutak per local tradition."],
        }

        for ec in eclipses:
            ec_date = ec["date"]
            conflict_msgs = []

            # Holika Dahan 2026 special case — Purnima (March 3, 2026) has Penumbral Lunar Eclipse
            # Any Purnima lunar eclipse near Phalguna Purnima = Holika Dahan affected
            try:
                y, m, d = map(int, ec_date.split("-"))
                # Phalguna Purnima is approx Feb-March
                if ec["type"] in ("Total Lunar", "Partial Lunar", "Penumbral") and m in (2, 3):
                    conflict_msgs.append(
                        "⚠️ Grahan on Phalguna Purnima: Holika Dahan timing affected. "
                        "Per Dharmasindhu: Holika Dahan must be performed outside Sutak and Bhadra windows. "
                        "If full night is under eclipse, perform Holika Dahan in next sunrise Pradosh per local guidance."
                    )
                # Ashadha/Shravana Purnima — Guru Purnima
                if ec["type"] in ("Total Lunar", "Partial Lunar") and m in (6, 7):
                    conflict_msgs.append(
                        "⚠️ Grahan on Ashadha/Shravana Purnima: Guru Purnima observance affected. "
                        "Pada Puja and Guru Vandana should be performed before Sutak begins."
                    )
                # Shravana Purnima — Raksha Bandhan
                if ec["type"] in ("Total Lunar", "Partial Lunar", "Penumbral") and m in (7, 8):
                    conflict_msgs.append(
                        "⚠️ Possible Raksha Bandhan conflict: Rakhi must not be tied during Sutak. "
                        "Tie Rakhi before Sutak starts or after Moksha + bath."
                    )
                # Kartik Amavasya — Diwali
                if ec["type"] in ("Total Solar", "Annular Solar", "Partial Solar") and m in (10, 11):
                    conflict_msgs.append(
                        "⚠️ Solar Grahan near Diwali: Lakshmi Puja timing affected. "
                        "Avoid Puja during Sutak window. Perform after Moksha + purification."
                    )
            except Exception:
                pass

            if conflict_msgs:
                ec["festival_conflict"] = conflict_msgs
            else:
                general = _FESTIVAL_CONFLICTS.get(ec["type"])
                if general:
                    ec["festival_conflict"] = general

        return {"year": year, "eclipses": eclipses, "tz": tz}

    except ImportError:
        raise HTTPException(status_code=503, detail="pyswisseph not installed")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
