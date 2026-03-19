"""
Panchang service — wraps scripts/panchang.py Panchang class (v2).
Single source of calculation truth (no duplicate logic here).
"""
import sys, os
from typing import Optional

# Import Panchang from the scripts directory on the VM
# Path: ~/books/scripts/panchang.py
_scripts_dir = os.path.join(os.path.expanduser("~/books"), "scripts")
if _scripts_dir not in sys.path:
    sys.path.insert(0, _scripts_dir)

try:
    from panchang import Panchang
    PANCHANG_AVAILABLE = True
except Exception as _e:
    PANCHANG_AVAILABLE = False
    print(f"WARNING: panchang.py import failed — {_e}")


def get_panchang(year: int, month: int, day: int, lat: float, lon: float, tz: float) -> dict:
    """
    Return PanchangResponse-shaped dict for the given date and location.
    Raises RuntimeError if pyswisseph unavailable.
    """
    if not PANCHANG_AVAILABLE:
        raise RuntimeError("Panchang module unavailable (pyswisseph not installed)")

    p   = Panchang(year, month, day, lat=lat, lon=lon, tz=tz)
    raw = p.to_dict()

    tithi     = raw["tithi"]
    vara      = raw["vara"]
    nakshatra = raw["nakshatra"]
    yoga      = raw["yoga"]
    karana    = raw["karana"]

    def _slot(key):
        s = raw.get(key, {})
        return {"start": s.get("start", ""), "end": s.get("end", "")} if s else None

    # Choghadiya: convert list of dicts directly
    chog = raw.get("choghadiya")
    if chog:
        def _chog_list(lst):
            return [
                {
                    "name":       c["name"],
                    "hindi":      c["hindi"],
                    "quality":    c["quality"],
                    "auspicious": c["auspicious"],
                    "start":      c["start"],
                    "end":        c["end"],
                }
                for c in lst
            ]
        choghadiya_out = {
            "day":   _chog_list(chog["day"]),
            "night": _chog_list(chog["night"]),
        }
    else:
        choghadiya_out = None

    # Nishita Kaal
    nishita = raw.get("nishita_kaal", {})
    nishita_out = {
        "start":    nishita.get("start", ""),
        "end":      nishita.get("end", ""),
        "midpoint": nishita.get("midpoint", ""),
    } if nishita else None

    return {
        "vara": {
            "name":  vara.get("name", ""),
            "hindi": vara.get("hindi", ""),
            "lord":  vara.get("lord", ""),
        },
        "tithi": {
            "name":        tithi.get("name", ""),
            "hindi":       tithi.get("hindi", ""),
            "paksha":      tithi.get("paksha", ""),
            "end_time":    tithi.get("end_time", ""),
            "tithi_type":  tithi.get("tithi_type", "normal"),
        },
        "nakshatra": {
            "name":     nakshatra.get("name", ""),
            "hindi":    nakshatra.get("hindi", ""),
            "pada":     nakshatra.get("pada", 1),
            "lord":     nakshatra.get("lord", ""),
            "end_time": nakshatra.get("end_time", ""),
        },
        "yoga": {
            "name":          yoga.get("name", ""),
            "hindi":         yoga.get("hindi", ""),
            "is_auspicious": bool(yoga.get("is_auspicious", True)),
            "end_time":      yoga.get("end_time", ""),
        },
        "karana": {
            "name":      karana.get("name", ""),
            "hindi":     karana.get("hindi", ""),
            "is_bhadra": bool(karana.get("is_bhadra", False)),
        },
        "sunrise":       raw.get("sunrise", ""),
        "sunset":        raw.get("sunset", ""),
        "abhijit_muhurta": {"start": raw["abhijit_muhurta"]["start"], "end": raw["abhijit_muhurta"]["end"]},
        "rahukaal":        {"start": raw["rahukaal"]["start"],        "end": raw["rahukaal"]["end"]},
        "yamagandam":      _slot("yamagandam"),
        "gulika_kaal":     _slot("gulika_kaal"),
        "brahma_muhurta":  _slot("brahma_muhurta"),
        "pradosh_kaal":    _slot("pradosh_kaal"),
        "nishita_kaal":    nishita_out,
        "choghadiya":      choghadiya_out,
        "panchaka":        raw.get("panchaka", False),
    }
