"""
Brahm AI - Panchang (Vedic Hindu Calendar) Module — v2 (Perfect)
Calculates all 5 Panchang elements: Tithi, Vara, Nakshatra, Yoga, Karana
+ Rahukaal, Yamagandam, Gulika Kaal
+ Brahma Muhurta, Abhijit Muhurta, Pradosh Kaal, Nishita Kaal
+ Choghadiya (day + night)
+ Vriddhi / Kshaya tithi detection

Key improvements over v1:
  - Hindu sunrise definition (disc center, no atmospheric refraction) per Dharmashastra
  - All 5 Angas evaluated at actual sunrise (Udaya Tithi rule), not noon
  - Binary search for exact Tithi / Nakshatra / Yoga end times (not speed extrapolation)
  - Vriddhi (extended) and Kshaya (skipped) Tithi detection
  - Yamagandam + Gulika Kaal added
  - Brahma Muhurta, Pradosh Kaal, Nishita Kaal added
  - Choghadiya (8 day + 8 night periods) added

Reference: "Computational Architecture and Astronomical Algorithms for High-Precision
           Hindu Panchang Generation" — Brahm AI Research Library (Swiss Ephemeris, Lahiri)

Usage:
    from special.panchang import Panchang
    p = Panchang(2026, 3, 15, lat=28.6139, lon=77.2090, tz=5.5)
    p.print_panchang()
    d = p.to_dict()
"""

import swisseph as swe
import math
from datetime import datetime, timedelta

# ============================================================
# CONSTANTS
# ============================================================

TITHI_NAMES = [
    "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
    "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
    "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima",
    "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
    "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
    "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Amavasya",
]

TITHI_HINDI = [
    "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
    "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
    "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा",
    "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
    "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
    "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "अमावस्या",
]

YOGA_NAMES = [
    "Vishkumbha", "Preeti", "Ayushman", "Saubhagya", "Shobhana",
    "Atiganda", "Sukarma", "Dhriti", "Shoola", "Ganda",
    "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
    "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
    "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
    "Indra", "Vaidhriti",
]

YOGA_HINDI = [
    "विष्कुम्भ", "प्रीति", "आयुष्मान", "सौभाग्य", "शोभन",
    "अतिगण्ड", "सुकर्म", "धृति", "शूल", "गण्ड",
    "वृद्धि", "ध्रुव", "व्याघात", "हर्षण", "वज्र",
    "सिद्धि", "व्यतीपात", "वरीयान", "परिघ", "शिव",
    "सिद्ध", "साध्य", "शुभ", "शुक्ल", "ब्रह्म",
    "इन्द्र", "वैधृति",
]

YOGA_AUSPICIOUS = [
    False, True, True, True, True,   # Vishkumbha, Preeti, Ayushman, Saubhagya, Shobhana
    False, True, True, False, False,  # Atiganda, Sukarma, Dhriti, Shoola, Ganda
    True, True, False, True, False,   # Vriddhi, Dhruva, Vyaghata, Harshana, Vajra
    True, False, False, False, True,  # Siddhi, Vyatipata, Variyan, Parigha, Shiva
    True, True, True, True, True,     # Siddha, Sadhya, Shubha, Shukla, Brahma
    True, False,                      # Indra, Vaidhriti
]

KARANA_FIXED    = ["Kimstughna", "Shakuni", "Chatushpada", "Naga"]
KARANA_FIXED_HINDI = ["किंस्तुघ्न", "शकुनि", "चतुष्पाद", "नाग"]
KARANA_RECURRING = ["Bava", "Balava", "Kaulava", "Taitila", "Garaja", "Vanija", "Vishti"]
KARANA_RECURRING_HINDI = ["बव", "बालव", "कौलव", "तैतिल", "गरज", "वणिज", "विष्टि"]

NAKSHATRA_NAMES = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Moola", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishtha",
    "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati",
]

NAKSHATRA_HINDI = [
    "अश्विनी", "भरणी", "कृत्तिका", "रोहिणी", "मृगशिरा", "आर्द्रा",
    "पुनर्वसु", "पुष्य", "आश्लेषा", "मघा", "पूर्वा फाल्गुनी", "उत्तरा फाल्गुनी",
    "हस्त", "चित्रा", "स्वाति", "विशाखा", "अनुराधा", "ज्येष्ठा",
    "मूल", "पूर्वाषाढ़ा", "उत्तराषाढ़ा", "श्रवण", "धनिष्ठा",
    "शतभिषा", "पूर्वभाद्रपद", "उत्तरभाद्रपद", "रेवती",
]

NAKSHATRA_LORDS = [
    "Ketu", "Shukra", "Surya", "Chandra", "Mangal", "Rahu",
    "Guru", "Shani", "Budh", "Ketu", "Shukra", "Surya",
    "Chandra", "Mangal", "Rahu", "Guru", "Shani", "Budh",
    "Ketu", "Shukra", "Surya", "Chandra", "Mangal", "Rahu",
    "Guru", "Shani", "Budh",
]

# Vara (weekday): 0=Monday … 6=Sunday
VARA_NAMES    = ["Somavara", "Mangalavara", "Budhavara", "Guruvara",
                 "Shukravara", "Shanivara", "Ravivara"]
VARA_HINDI    = ["सोमवार", "मंगलवार", "बुधवार", "गुरुवार",
                 "शुक्रवार", "शनिवार", "रविवार"]
VARA_LORDS    = ["Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Surya"]
VARA_LORDS_HINDI = ["चन्द्र", "मंगल", "बुध", "गुरु", "शुक्र", "शनि", "सूर्य"]

# Inauspicious period segment indices (1-indexed, out of 8 equal day divisions)
# Weekday: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
RAHUKAAL_SEG  = {0: 2, 1: 7, 2: 5, 3: 6, 4: 4, 5: 3, 6: 8}
YAMAGANDAM_SEG = {0: 4, 1: 3, 2: 2, 3: 1, 4: 7, 5: 6, 6: 5}
GULIKA_SEG    = {0: 6, 1: 5, 2: 4, 3: 3, 4: 2, 5: 1, 6: 7}

# Choghadiya order per weekday (0=Mon … 6=Sun), 8 periods each for day and night
# Quality: A=Amrit, S=Shubh, L=Labh, C=Char, U=Udveg, R=Rog, K=Kaal
CHOGHADIYA_DAY = {
    0: ["Amrit", "Kaal", "Shubh", "Rog", "Udveg", "Char", "Labh", "Amrit"],       # Mon
    1: ["Rog", "Udveg", "Char", "Labh", "Amrit", "Kaal", "Shubh", "Rog"],         # Tue
    2: ["Labh", "Amrit", "Kaal", "Shubh", "Rog", "Udveg", "Char", "Labh"],        # Wed
    3: ["Shubh", "Rog", "Udveg", "Char", "Labh", "Amrit", "Kaal", "Shubh"],       # Thu
    4: ["Char", "Labh", "Amrit", "Kaal", "Shubh", "Rog", "Udveg", "Char"],        # Fri
    5: ["Kaal", "Shubh", "Rog", "Udveg", "Char", "Labh", "Amrit", "Kaal"],        # Sat
    6: ["Udveg", "Char", "Labh", "Amrit", "Kaal", "Shubh", "Rog", "Udveg"],       # Sun
}
CHOGHADIYA_NIGHT = {
    0: ["Char", "Rog", "Kaal", "Labh", "Udveg", "Shubh", "Amrit", "Char"],        # Mon
    1: ["Kaal", "Labh", "Udveg", "Shubh", "Amrit", "Char", "Rog", "Kaal"],        # Tue
    2: ["Udveg", "Shubh", "Amrit", "Char", "Rog", "Kaal", "Labh", "Udveg"],       # Wed
    3: ["Amrit", "Char", "Rog", "Kaal", "Labh", "Udveg", "Shubh", "Amrit"],       # Thu
    4: ["Rog", "Kaal", "Labh", "Udveg", "Shubh", "Amrit", "Char", "Rog"],         # Fri
    5: ["Labh", "Udveg", "Shubh", "Amrit", "Char", "Rog", "Kaal", "Labh"],        # Sat
    6: ["Shubh", "Amrit", "Char", "Rog", "Kaal", "Labh", "Udveg", "Shubh"],       # Sun
}
CHOGHADIYA_QUALITY = {
    "Amrit": {"quality": "Excellent", "auspicious": True,  "hindi": "अमृत"},
    "Shubh": {"quality": "Good",      "auspicious": True,  "hindi": "शुभ"},
    "Labh":  {"quality": "Gain",      "auspicious": True,  "hindi": "लाभ"},
    "Char":  {"quality": "Movable",   "auspicious": True,  "hindi": "चर"},   # ok for travel
    "Udveg": {"quality": "Anxiety",   "auspicious": False, "hindi": "उद्वेग"},
    "Rog":   {"quality": "Sickness",  "auspicious": False, "hindi": "रोग"},
    "Kaal":  {"quality": "Death",     "auspicious": False, "hindi": "काल"},
}

# Swiss Ephemeris bitflags for Hindu sunrise
# SE_BIT_DISC_CENTER = 256: geometric center of solar disc (not upper limb)
# SE_BIT_NO_REFRACTION = 512: no atmospheric refraction (zenith angle = 90.000°)
# Combined: 768 = SE_BIT_HINDU_RISING per Swiss Ephemeris docs
_SE_DISC_CENTER    = 256
_SE_NO_REFRACTION  = 512
_SE_HINDU_RISING   = _SE_DISC_CENTER | _SE_NO_REFRACTION  # 768


# ============================================================
# HELPER UTILITIES
# ============================================================

def _jd_to_ist_str(jd, tz=5.5):
    """Convert Julian Day Number to local time string HH:MM."""
    unix_epoch_jd = 2440587.5
    unix_seconds  = (jd - unix_epoch_jd) * 86400.0
    dt_utc   = datetime(1970, 1, 1) + timedelta(seconds=unix_seconds)
    dt_local = dt_utc + timedelta(hours=tz)
    return dt_local.strftime("%H:%M")


def _jd_to_datetime(jd, tz=5.5):
    """Convert Julian Day to local datetime."""
    unix_epoch_jd = 2440587.5
    unix_seconds  = (jd - unix_epoch_jd) * 86400.0
    dt_utc = datetime(1970, 1, 1) + timedelta(seconds=unix_seconds)
    return dt_utc + timedelta(hours=tz)


def _get_sid_lon(planet_id, jd):
    """Return Lahiri sidereal longitude of planet at JD."""
    flags  = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
    result = swe.calc_ut(jd, planet_id, flags)
    return result[0][0] % 360.0


def _tithi_diff(jd):
    """Moon - Sun sidereal elongation in degrees [0, 360)."""
    moon = _get_sid_lon(swe.MOON, jd)
    sun  = _get_sid_lon(swe.SUN,  jd)
    return (moon - sun) % 360.0


def _yoga_sum(jd):
    """(Moon + Sun) sidereal sum mod 360."""
    moon = _get_sid_lon(swe.MOON, jd)
    sun  = _get_sid_lon(swe.SUN,  jd)
    return (moon + sun) % 360.0


def _find_next_crossing(fn, jd_start, target_val, span, max_days=2.0):
    """
    Binary-search for the next JD where fn(jd) mod span crosses target_val.
    fn should be monotonically increasing (e.g., tithi elongation, yoga sum).
    Returns JD to ~1-second accuracy.
    """
    # Current floor index
    cur_floor = int(fn(jd_start) / span)
    target_boundary = (cur_floor + 1) * span  # next crossing

    # Search window: current to current + max_days
    t_lo = jd_start
    t_hi = jd_start + max_days

    # Validate hi actually crosses the boundary
    hi_val = fn(t_hi)
    # Handle 360° wrap: if hi_val < starting val, adjust
    if hi_val < fn(jd_start) and target_boundary > 350:
        target_boundary = target_boundary % 360.0

    # Binary search
    for _ in range(60):  # 60 iterations → ~1 second accuracy
        t_mid = (t_lo + t_hi) * 0.5
        mid_val = fn(t_mid) % 360.0
        mid_idx = int(mid_val / span)
        if mid_idx < cur_floor + 1:
            t_lo = t_mid
        else:
            t_hi = t_mid
        if (t_hi - t_lo) * 86400 < 1.0:  # 1-second precision
            break

    return (t_lo + t_hi) * 0.5


def _find_next_nak_crossing(jd_start, max_days=2.0):
    """Binary-search for next Nakshatra boundary crossing by Moon."""
    nak_span = 360.0 / 27.0  # 13.3333°
    moon0 = _get_sid_lon(swe.MOON, jd_start)
    cur_nak = int(moon0 / nak_span)
    target_lon = (cur_nak + 1) * nak_span  # could exceed 360

    t_lo = jd_start
    t_hi = jd_start + max_days

    for _ in range(60):
        t_mid = (t_lo + t_hi) * 0.5
        moon_mid = _get_sid_lon(swe.MOON, t_mid)
        # Handle wrap
        wrapped_lon = moon_mid if moon_mid >= moon0 - 5 else moon_mid + 360
        if wrapped_lon < target_lon:
            t_lo = t_mid
        else:
            t_hi = t_mid
        if (t_hi - t_lo) * 86400 < 1.0:
            break

    return (t_lo + t_hi) * 0.5


# ============================================================
# PANCHANG CLASS
# ============================================================

class Panchang:
    def __init__(self, year, month, day, lat=28.6139, lon=77.2090,
                 tz=5.5, ephe_path="/home/amareshsingh2005/books/data/swiss_ephe",
                 hour=0, minute=0):
        self.year  = year
        self.month = month
        self.day   = day
        self.lat   = lat
        self.lon   = lon
        self.tz    = tz
        self.ephe_path = ephe_path

        swe.set_ephe_path(ephe_path)
        swe.set_sid_mode(swe.SIDM_LAHIRI)

        # JD at local noon (reference for fallback calcs)
        ut_noon = 12.0 - tz
        self.jd_noon = swe.julday(year, month, day, ut_noon)

        # Ayanamsha at noon
        self.ayanamsha = swe.get_ayanamsa(self.jd_noon)

        # Cached values (lazy)
        self._sunrise_sunset = None
        self._next_sunrise   = None   # JD of next day's sunrise (for Vriddhi/Kshaya)
        self._vara_num       = None

    # ----------------------------------------------------------
    # SUNRISE / SUNSET  (Hindu definition — disc center, no refraction)
    # ----------------------------------------------------------

    def get_sunrise_sunset(self):
        if self._sunrise_sunset is not None:
            return self._sunrise_sunset

        geopos = [self.lon, self.lat, 0.0]

        # Hindu sunrise: disc center, no refraction (SE_BIT_HINDU_RISING = 768)
        rsmi_rise = swe.CALC_RISE | _SE_HINDU_RISING
        rsmi_set  = swe.CALC_SET  | _SE_HINDU_RISING

        # Search from previous midnight (UTC) — safe starting point
        jd_search_start = self.jd_noon - 0.75

        try:
            ret_rise = swe.rise_trans(
                jd_search_start, swe.SUN, rsmi_rise, geopos, 0.0, 0.0, swe.FLG_SWIEPH
            )
            sunrise_jd = ret_rise[1][0]

            ret_set = swe.rise_trans(
                sunrise_jd, swe.SUN, rsmi_set, geopos, 0.0, 0.0, swe.FLG_SWIEPH
            )
            sunset_jd = ret_set[1][0]
        except Exception:
            # Fallback: standard rise_trans without Hindu flags
            ret_rise = swe.rise_trans(
                jd_search_start, swe.SUN, swe.CALC_RISE, geopos, 0.0, 0.0, swe.FLG_SWIEPH
            )
            sunrise_jd = ret_rise[1][0]
            ret_set = swe.rise_trans(
                sunrise_jd, swe.SUN, swe.CALC_SET, geopos, 0.0, 0.0, swe.FLG_SWIEPH
            )
            sunset_jd = ret_set[1][0]

        self._sunrise_sunset = {
            "sunrise_jd":  sunrise_jd,
            "sunset_jd":   sunset_jd,
            "sunrise_str": _jd_to_ist_str(sunrise_jd, self.tz),
            "sunset_str":  _jd_to_ist_str(sunset_jd,  self.tz),
        }
        return self._sunrise_sunset

    def _get_next_sunrise_jd(self):
        """JD of tomorrow's sunrise — used for Vriddhi/Kshaya detection."""
        if self._next_sunrise is not None:
            return self._next_sunrise
        sr = self.get_sunrise_sunset()
        geopos = [self.lon, self.lat, 0.0]
        rsmi_rise = swe.CALC_RISE | _SE_HINDU_RISING
        try:
            ret = swe.rise_trans(
                sr["sunrise_jd"] + 0.9, swe.SUN, rsmi_rise, geopos, 0.0, 0.0, swe.FLG_SWIEPH
            )
            self._next_sunrise = ret[1][0]
        except Exception:
            self._next_sunrise = sr["sunrise_jd"] + 1.0
        return self._next_sunrise

    # ----------------------------------------------------------
    # VARA (Weekday — from sunrise JD)
    # ----------------------------------------------------------

    def get_vara(self):
        sr = self.get_sunrise_sunset()
        dt_local = _jd_to_datetime(sr["sunrise_jd"], tz=self.tz)
        weekday  = dt_local.weekday()  # 0=Mon, 6=Sun
        self._vara_num = weekday
        return {
            "num":        weekday,
            "name":       VARA_NAMES[weekday],
            "hindi":      VARA_HINDI[weekday],
            "lord":       VARA_LORDS[weekday],
            "lord_hindi": VARA_LORDS_HINDI[weekday],
        }

    # ----------------------------------------------------------
    # TITHI (Lunar Day) — evaluated at sunrise (Udaya Tithi rule)
    # ----------------------------------------------------------

    def get_tithi(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd = sr["sunrise_jd"]

        diff     = _tithi_diff(sunrise_jd)
        tithi_idx = int(diff / 12.0)           # 0-based index 0-29
        tithi_idx = max(0, min(29, tithi_idx))
        tithi_num = tithi_idx + 1              # 1-30

        paksha = "Shukla" if tithi_num <= 15 else "Krishna"
        paksha_hindi = "शुक्ल पक्ष" if paksha == "Shukla" else "कृष्ण पक्ष"

        # Exact end time via binary search
        try:
            end_jd = _find_next_crossing(_tithi_diff, sunrise_jd, None, 12.0)
            end_time_str = _jd_to_ist_str(end_jd, self.tz)
        except Exception:
            # Fallback: linear estimate using mean angular speed
            fraction = (diff % 12.0) / 12.0
            hours_rem = (1.0 - fraction) * 12.0 / 0.508
            end_jd   = sunrise_jd + hours_rem / 24.0
            end_time_str = _jd_to_ist_str(end_jd, self.tz)

        # Vriddhi / Kshaya detection
        next_sunrise_jd = self._get_next_sunrise_jd()
        diff_next     = _tithi_diff(next_sunrise_jd)
        tithi_idx_next = int(diff_next / 12.0)

        if tithi_idx_next == tithi_idx:
            tithi_type = "vridhi"    # same tithi at both sunrises = extended
        elif tithi_idx_next == tithi_idx + 2 or (tithi_idx == 29 and tithi_idx_next == 1):
            tithi_type = "ksheya"   # jumped an index = skipped tithi
        else:
            tithi_type = "normal"

        return {
            "num":         tithi_num,
            "idx":         tithi_idx,
            "name":        TITHI_NAMES[tithi_idx],
            "hindi":       TITHI_HINDI[tithi_idx],
            "paksha":      paksha,
            "paksha_hindi": paksha_hindi,
            "end_time":    end_time_str,
            "tithi_type":  tithi_type,
            "diff_deg":    round(diff, 4),
        }

    # ----------------------------------------------------------
    # NAKSHATRA — evaluated at sunrise
    # ----------------------------------------------------------

    def get_nakshatra(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd = sr["sunrise_jd"]

        moon_lon  = _get_sid_lon(swe.MOON, sunrise_jd)
        nak_span  = 360.0 / 27.0
        nak_idx   = int(moon_lon / nak_span)
        nak_idx   = max(0, min(26, nak_idx))

        pos_in_nak = moon_lon - (nak_idx * nak_span)
        fraction   = pos_in_nak / nak_span
        pada_span  = nak_span / 4.0
        pada       = int(pos_in_nak / pada_span) + 1
        pada       = max(1, min(4, pada))

        # Exact end time via binary search on Moon longitude
        try:
            end_jd = _find_next_nak_crossing(sunrise_jd)
            end_time_str = _jd_to_ist_str(end_jd, self.tz)
        except Exception:
            # Fallback: Moon speed ~13.17°/day
            hours_rem    = (1.0 - fraction) * nak_span / (13.17 / 24.0)
            end_jd       = sunrise_jd + hours_rem / 24.0
            end_time_str = _jd_to_ist_str(end_jd, self.tz)

        return {
            "idx":      nak_idx,
            "name":     NAKSHATRA_NAMES[nak_idx],
            "hindi":    NAKSHATRA_HINDI[nak_idx],
            "pada":     pada,
            "lord":     NAKSHATRA_LORDS[nak_idx],
            "fraction": round(fraction, 4),
            "moon_lon": round(moon_lon, 4),
            "end_time": end_time_str,
        }

    # ----------------------------------------------------------
    # YOGA — evaluated at sunrise
    # ----------------------------------------------------------

    def get_yoga(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd = sr["sunrise_jd"]

        total     = _yoga_sum(sunrise_jd)
        yoga_span = 360.0 / 27.0
        yoga_idx  = int(total / yoga_span)
        yoga_idx  = max(0, min(26, yoga_idx))
        fraction  = (total % yoga_span) / yoga_span

        # Exact end time via binary search on yoga sum
        try:
            end_jd = _find_next_crossing(_yoga_sum, sunrise_jd, None, yoga_span)
            end_time_str = _jd_to_ist_str(end_jd, self.tz)
        except Exception:
            # Fallback: yoga changes ~slightly faster than tithi
            hours_rem = (1.0 - fraction) * yoga_span / (14.17 / 24.0)
            end_jd    = sunrise_jd + hours_rem / 24.0
            end_time_str = _jd_to_ist_str(end_jd, self.tz)

        return {
            "idx":          yoga_idx,
            "name":         YOGA_NAMES[yoga_idx],
            "hindi":        YOGA_HINDI[yoga_idx],
            "is_auspicious": YOGA_AUSPICIOUS[yoga_idx],
            "fraction":     round(fraction, 4),
            "total_deg":    round(total, 4),
            "end_time":     end_time_str,
        }

    # ----------------------------------------------------------
    # KARANA — evaluated at sunrise
    # ----------------------------------------------------------

    def get_karana(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd = sr["sunrise_jd"]

        diff        = _tithi_diff(sunrise_jd)
        karana_half = int(diff / 6.0)  # 0–59
        karana_half = max(0, min(59, karana_half))

        if karana_half == 0:
            name = KARANA_FIXED[0]; hindi = KARANA_FIXED_HINDI[0]; is_fixed = True; idx = 0
        elif karana_half >= 57:
            fixed_idx = min(karana_half - 57 + 1, 3)
            name = KARANA_FIXED[fixed_idx]; hindi = KARANA_FIXED_HINDI[fixed_idx]
            is_fixed = True; idx = fixed_idx
        else:
            recurring_pos = (karana_half - 1) % 7
            name = KARANA_RECURRING[recurring_pos]
            hindi = KARANA_RECURRING_HINDI[recurring_pos]
            is_fixed = False; idx = recurring_pos

        is_bhadra = (name == "Vishti")
        half_in_tithi = (karana_half % 2) + 1

        return {
            "idx":          idx,
            "name":         name,
            "hindi":        hindi,
            "is_fixed":     is_fixed,
            "half_in_tithi": half_in_tithi,
            "karana_seq":   karana_half,
            "is_bhadra":    is_bhadra,
        }

    # ----------------------------------------------------------
    # HELPER: build inauspicious period from segment table
    # ----------------------------------------------------------

    def _seg_period(self, seg_table):
        sr = self.get_sunrise_sunset()
        sunrise_jd = sr["sunrise_jd"]
        sunset_jd  = sr["sunset_jd"]
        day_dur    = sunset_jd - sunrise_jd
        seg_dur    = day_dur / 8.0

        vara    = self.get_vara()
        weekday = vara["num"]
        seg_n   = seg_table[weekday]  # 1-indexed

        start_jd = sunrise_jd + (seg_n - 1) * seg_dur
        end_jd   = start_jd + seg_dur
        return {
            "start":   _jd_to_ist_str(start_jd, self.tz),
            "end":     _jd_to_ist_str(end_jd,   self.tz),
            "segment": seg_n,
        }

    # ----------------------------------------------------------
    # RAHUKAAL / YAMAGANDAM / GULIKA
    # ----------------------------------------------------------

    def get_rahukaal(self):
        return self._seg_period(RAHUKAAL_SEG)

    def get_yamagandam(self):
        return self._seg_period(YAMAGANDAM_SEG)

    def get_gulika_kaal(self):
        return self._seg_period(GULIKA_SEG)

    # ----------------------------------------------------------
    # ABHIJIT MUHURTA (8th of 15 equal daylight Muhurtas)
    # ----------------------------------------------------------

    def get_abhijit_muhurta(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd  = sr["sunrise_jd"]
        sunset_jd   = sr["sunset_jd"]
        muhurta_dur = (sunset_jd - sunrise_jd) / 15.0

        start_jd  = sunrise_jd + 7 * muhurta_dur
        end_jd    = start_jd + muhurta_dur
        midday_jd = (sunrise_jd + sunset_jd) / 2.0

        return {
            "start":   _jd_to_ist_str(start_jd,  self.tz),
            "end":     _jd_to_ist_str(end_jd,     self.tz),
            "midday":  _jd_to_ist_str(midday_jd,  self.tz),
        }

    # ----------------------------------------------------------
    # BRAHMA MUHURTA (2 muhurtas = 96 min before sunrise)
    # ----------------------------------------------------------

    def get_brahma_muhurta(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd = sr["sunrise_jd"]

        # Night length from previous sunset: approximate as 24h - daylight
        # Simpler: 2 Muhurtas = 2 × 48min = 96 min before sunrise
        # Per Shastra: night divided into 15 Muhurtas; Brahma = 14th = 96 min before sunrise
        end_jd   = sunrise_jd
        start_jd = sunrise_jd - (96.0 / 1440.0)  # 96 minutes

        return {
            "start": _jd_to_ist_str(start_jd, self.tz),
            "end":   _jd_to_ist_str(end_jd,   self.tz),
        }

    # ----------------------------------------------------------
    # PRADOSH KAAL (3 Muhurtas = 144 min starting at sunset)
    # ----------------------------------------------------------

    def get_pradosh_kaal(self):
        sr = self.get_sunrise_sunset()
        sunset_jd = sr["sunset_jd"]

        start_jd = sunset_jd
        end_jd   = sunset_jd + (144.0 / 1440.0)  # 144 minutes

        return {
            "start": _jd_to_ist_str(start_jd, self.tz),
            "end":   _jd_to_ist_str(end_jd,   self.tz),
        }

    # ----------------------------------------------------------
    # NISHITA KAAL (8th of 15 night Muhurtas — centered on midnight)
    # ----------------------------------------------------------

    def get_nishita_kaal(self):
        sr = self.get_sunrise_sunset()
        sunset_jd  = sr["sunset_jd"]
        next_sunrise_jd = self._get_next_sunrise_jd()

        night_dur   = next_sunrise_jd - sunset_jd
        muhurta_dur = night_dur / 15.0

        # 8th muhurta (0-indexed: 7th) is the Nishita = midnight
        start_jd = sunset_jd + 7 * muhurta_dur
        end_jd   = start_jd + muhurta_dur

        return {
            "start":   _jd_to_ist_str(start_jd, self.tz),
            "end":     _jd_to_ist_str(end_jd,   self.tz),
            "midpoint": _jd_to_ist_str((start_jd + end_jd) / 2, self.tz),
        }

    # ----------------------------------------------------------
    # CHOGHADIYA (8 day + 8 night Vedic time periods)
    # ----------------------------------------------------------

    def get_choghadiya(self):
        sr = self.get_sunrise_sunset()
        sunrise_jd      = sr["sunrise_jd"]
        sunset_jd       = sr["sunset_jd"]
        next_sunrise_jd = self._get_next_sunrise_jd()

        vara    = self.get_vara()
        weekday = vara["num"]

        day_dur    = sunset_jd - sunrise_jd
        night_dur  = next_sunrise_jd - sunset_jd
        day_seg    = day_dur / 8.0
        night_seg  = night_dur / 8.0

        day_names   = CHOGHADIYA_DAY[weekday]
        night_names = CHOGHADIYA_NIGHT[weekday]

        def _build(names, base_jd, seg_dur):
            periods = []
            for i, name in enumerate(names):
                s = base_jd + i * seg_dur
                e = s + seg_dur
                q = CHOGHADIYA_QUALITY[name]
                periods.append({
                    "name":        name,
                    "hindi":       q["hindi"],
                    "quality":     q["quality"],
                    "auspicious":  q["auspicious"],
                    "start":       _jd_to_ist_str(s, self.tz),
                    "end":         _jd_to_ist_str(e, self.tz),
                })
            return periods

        return {
            "day":   _build(day_names,   sunrise_jd, day_seg),
            "night": _build(night_names, sunset_jd,  night_seg),
        }

    # ----------------------------------------------------------
    # PANCHAKA PERIOD (Moon in last 5 Nakshatras: Dhanishtha 2nd half
    #                   through Revati — indices 22-26)
    # ----------------------------------------------------------

    def is_panchaka(self):
        """Returns True if Moon is in Panchaka (Nakshatras 23-27, sidereal)."""
        nakshatra = self.get_nakshatra()
        nak_idx   = nakshatra["idx"]
        # Dhanishtha 2nd half (nak 22, pada 3/4) through Revati (nak 26)
        if nak_idx > 22:
            return True
        if nak_idx == 22 and nakshatra["pada"] >= 3:
            return True
        return False

    # ----------------------------------------------------------
    # TO_DICT — complete Panchang output
    # ----------------------------------------------------------

    def to_dict(self):
        sr        = self.get_sunrise_sunset()
        tithi     = self.get_tithi()
        vara      = self.get_vara()
        nakshatra = self.get_nakshatra()
        yoga      = self.get_yoga()
        karana    = self.get_karana()
        rahukaal  = self.get_rahukaal()
        yamagan   = self.get_yamagandam()
        gulika    = self.get_gulika_kaal()
        abhijit   = self.get_abhijit_muhurta()
        brahma    = self.get_brahma_muhurta()
        pradosh   = self.get_pradosh_kaal()
        nishita   = self.get_nishita_kaal()
        choghadiya = self.get_choghadiya()

        return {
            "date":             f"{self.day:02d}/{self.month:02d}/{self.year}",
            "location":         {"lat": self.lat, "lon": self.lon, "tz": self.tz},
            "ayanamsha_lahiri": round(self.ayanamsha, 4),
            "sunrise":          sr["sunrise_str"],
            "sunset":           sr["sunset_str"],
            # Five Angas
            "vara":      vara,
            "tithi":     tithi,
            "nakshatra": nakshatra,
            "yoga":      yoga,
            "karana":    karana,
            # Inauspicious periods
            "rahukaal":    {"start": rahukaal["start"], "end": rahukaal["end"]},
            "yamagandam":  {"start": yamagan["start"],  "end": yamagan["end"]},
            "gulika_kaal": {"start": gulika["start"],   "end": gulika["end"]},
            # Auspicious windows
            "abhijit_muhurta": {"start": abhijit["start"], "end": abhijit["end"]},
            "brahma_muhurta":  {"start": brahma["start"],  "end": brahma["end"]},
            "pradosh_kaal":    {"start": pradosh["start"], "end": pradosh["end"]},
            "nishita_kaal":    {
                "start":    nishita["start"],
                "end":      nishita["end"],
                "midpoint": nishita["midpoint"],
            },
            # Choghadiya
            "choghadiya": choghadiya,
            # Panchaka
            "panchaka": self.is_panchaka(),
        }

    # ----------------------------------------------------------
    # PRINT PANCHANG
    # ----------------------------------------------------------

    def print_panchang(self):
        d         = self.to_dict()
        tithi     = d["tithi"]
        vara      = d["vara"]
        nakshatra = d["nakshatra"]
        yoga      = d["yoga"]
        karana    = d["karana"]

        print()
        print("=" * 68)
        print("          पञ्चाङ्ग  (PANCHANG / VEDIC ALMANAC)  v2")
        print("=" * 68)
        print(f"  तिथि / Date  : {self.day:02d} / {self.month:02d} / {self.year}")
        print(f"  स्थान        : {self.lat}°N, {self.lon}°E  (TZ: UTC+{self.tz})")
        print(f"  अयनांश       : {d['ayanamsha_lahiri']:.4f}° (Lahiri)")
        print("=" * 68)
        print(f"  सूर्योदय  : {d['sunrise']}   |   सूर्यास्त  : {d['sunset']}")
        print()

        def _sec(title):
            print(f"  {'─'*30}  {title}")

        _sec("वार (VARA / Weekday)")
        print(f"  {vara['hindi']}  ({vara['name']})  —  स्वामी: {vara['lord']}")

        _sec("तिथि (TITHI / Lunar Day)")
        type_str = {"vridhi": " [वृद्धि — extended]", "ksheya": " [क्षय — skipped]", "normal": ""}.get(tithi["tithi_type"], "")
        print(f"  {tithi['paksha_hindi']}  {tithi['hindi']}  ({tithi['paksha']} {tithi['name']}){type_str}")
        print(f"  समाप्ति     : {tithi['end_time']}")

        _sec("नक्षत्र (NAKSHATRA / Lunar Mansion)")
        print(f"  {nakshatra['hindi']}  ({nakshatra['name']})  पाद {nakshatra['pada']}/4  —  स्वामी: {nakshatra['lord']}")
        print(f"  समाप्ति     : {nakshatra['end_time']}")

        _sec("योग (YOGA)")
        ausp = "शुभ" if yoga["is_auspicious"] else "अशुभ"
        print(f"  {yoga['hindi']}  ({yoga['name']})  —  {ausp}")
        print(f"  समाप्ति     : {yoga['end_time']}")

        _sec("करण (KARANA / Half Lunar Day)")
        bhadra_str = "  ⚠️ भद्रा!" if karana["is_bhadra"] else ""
        print(f"  {karana['hindi']}  ({karana['name']}){bhadra_str}")

        _sec("राहुकाल  /  यमगण्ड  /  गुलिक")
        print(f"  राहुकाल   : {d['rahukaal']['start']} – {d['rahukaal']['end']}")
        print(f"  यमगण्ड    : {d['yamagandam']['start']} – {d['yamagandam']['end']}")
        print(f"  गुलिक काल : {d['gulika_kaal']['start']} – {d['gulika_kaal']['end']}")

        _sec("शुभ मुहूर्त (Auspicious Windows)")
        print(f"  ब्रह्म मुहूर्त : {d['brahma_muhurta']['start']} – {d['brahma_muhurta']['end']}")
        print(f"  अभिजित    : {d['abhijit_muhurta']['start']} – {d['abhijit_muhurta']['end']}")
        print(f"  प्रदोष काल : {d['pradosh_kaal']['start']} – {d['pradosh_kaal']['end']}")
        print(f"  निशीथ काल : {d['nishita_kaal']['start']} – {d['nishita_kaal']['end']}")

        if d["panchaka"]:
            print()
            print("  ⚠️  पञ्चक (Panchaka): Moon in last 5 Nakshatras — avoid new constructions")

        _sec("चौघड़िया (CHOGHADIYA)")
        print("  Day:")
        for c in d["choghadiya"]["day"]:
            mark = "✓" if c["auspicious"] else "✗"
            print(f"    {mark} {c['start']}–{c['end']}  {c['hindi']} ({c['name']}) [{c['quality']}]")
        print("  Night:")
        for c in d["choghadiya"]["night"]:
            mark = "✓" if c["auspicious"] else "✗"
            print(f"    {mark} {c['start']}–{c['end']}  {c['hindi']} ({c['name']}) [{c['quality']}]")

        print()
        print("=" * 68)
