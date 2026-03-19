"""
Brahm AI - Kundali (Birth Chart) Module
NASA DE441 + pyswisseph for extreme accuracy

Usage:
    from special.kundali import Kundali
    k = Kundali(year=1990, month=5, day=15, hour=10, minute=30, lat=28.6139, lon=77.2090, tz=5.5)
    k.print_chart()
"""

import swisseph as swe
import math
from datetime import datetime

# Swiss Ephemeris path (update on VM)
EPHE_PATH = "/home/amareshsingh2005/books/data/swiss_ephe"

# Ayanamsha constants
AYANAMSHA_LAHIRI = swe.SIDM_LAHIRI
AYANAMSHA_KP = swe.SIDM_KRISHNAMURTI
AYANAMSHA_RAMAN = swe.SIDM_RAMAN

# 9 Grahas (Vedic)
GRAHAS = {
    swe.SUN: "Surya",
    swe.MOON: "Chandra",
    swe.MARS: "Mangal",
    swe.MERCURY: "Budh",
    swe.JUPITER: "Guru",
    swe.VENUS: "Shukra",
    swe.SATURN: "Shani",
    swe.MEAN_NODE: "Rahu",      # Mean North Node
    # Ketu = Rahu + 180 (calculated)
}

# 12 Rashis
RASHIS = [
    "Mesha (Aries)", "Vrishabha (Taurus)", "Mithuna (Gemini)",
    "Karka (Cancer)", "Simha (Leo)", "Kanya (Virgo)",
    "Tula (Libra)", "Vrischika (Scorpio)", "Dhanu (Sagittarius)",
    "Makara (Capricorn)", "Kumbha (Aquarius)", "Meena (Pisces)"
]

RASHI_SHORT = [
    "Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya",
    "Tula", "Vrischika", "Dhanu", "Makara", "Kumbha", "Meena"
]

# 27 Nakshatras
NAKSHATRAS = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Moola", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishtha",
    "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
]

# Nakshatra lords (for Vimshottari Dasha)
NAKSHATRA_LORDS = [
    "Ketu", "Shukra", "Surya", "Chandra", "Mangal", "Rahu",
    "Guru", "Shani", "Budh", "Ketu", "Shukra", "Surya",
    "Chandra", "Mangal", "Rahu", "Guru", "Shani", "Budh",
    "Ketu", "Shukra", "Surya", "Chandra", "Mangal", "Rahu",
    "Guru", "Shani", "Budh"
]

# Dasha years (Vimshottari - total 120 years)
DASHA_YEARS = {
    "Ketu": 7, "Shukra": 20, "Surya": 6, "Chandra": 10,
    "Mangal": 7, "Rahu": 18, "Guru": 16, "Shani": 19, "Budh": 17
}

# Dasha sequence
DASHA_SEQUENCE = ["Ketu", "Shukra", "Surya", "Chandra", "Mangal",
                  "Rahu", "Guru", "Shani", "Budh"]

# Rashi lords
RASHI_LORDS = {
    0: "Mangal", 1: "Shukra", 2: "Budh", 3: "Chandra",
    4: "Surya", 5: "Budh", 6: "Shukra", 7: "Mangal",
    8: "Guru", 9: "Shani", 10: "Shani", 11: "Guru"
}

# Graha exaltation rashis
EXALTATION = {
    "Surya": 0, "Chandra": 1, "Mangal": 9, "Budh": 5,
    "Guru": 3, "Shukra": 11, "Shani": 6, "Rahu": 1, "Ketu": 7
}

# Graha debilitation rashis
DEBILITATION = {
    "Surya": 6, "Chandra": 7, "Mangal": 3, "Budh": 11,
    "Guru": 9, "Shukra": 5, "Shani": 0, "Rahu": 7, "Ketu": 1
}


class Kundali:
    def __init__(self, year, month, day, hour=0, minute=0, second=0,
                 lat=28.6139, lon=77.2090, tz=5.5, ayanamsha=AYANAMSHA_LAHIRI,
                 name="", place=""):
        """
        Generate Vedic Birth Chart (Kundali)

        Args:
            year, month, day: Birth date
            hour, minute, second: Birth time (24hr format, LOCAL time)
            lat, lon: Birth place coordinates
            tz: Timezone offset from UTC (e.g., India = 5.5)
            ayanamsha: Ayanamsha system (default: Lahiri)
            name: Person's name (optional)
            place: Birth place name (optional)
        """
        self.name = name
        self.place = place
        self.birth_date = datetime(year, month, day, hour, minute, second)
        self.lat = lat
        self.lon = lon
        self.tz = tz

        # Setup Swiss Ephemeris
        swe.set_ephe_path(EPHE_PATH)
        swe.set_sid_mode(ayanamsha)

        # Calculate Julian Day (UTC)
        ut_hour = hour + minute / 60.0 + second / 3600.0 - tz
        self.jd = swe.julday(year, month, day, ut_hour)

        # Calculate Ayanamsha value
        self.ayanamsha_value = swe.get_ayanamsa(self.jd)

        # Calculate all graha positions
        self.grahas = self._calculate_grahas()

        # Calculate Lagna (Ascendant) and houses
        self.lagna, self.houses = self._calculate_houses()

        # Calculate Ketu (opposite of Rahu)
        rahu_lon = self.grahas["Rahu"]["longitude"]
        ketu_lon = (rahu_lon + 180) % 360
        ketu_rashi = int(ketu_lon / 30)
        ketu_degree = ketu_lon % 30
        ketu_nak = int(ketu_lon / (360 / 27))
        ketu_pada = int((ketu_lon % (360 / 27)) / (360 / 108)) + 1

        self.grahas["Ketu"] = {
            "longitude": ketu_lon,
            "latitude": 0,
            "rashi": ketu_rashi,
            "rashi_name": RASHI_SHORT[ketu_rashi],
            "degree": ketu_degree,
            "nakshatra": NAKSHATRAS[ketu_nak],
            "nakshatra_idx": ketu_nak,
            "pada": ketu_pada,
            "retro": self.grahas["Rahu"].get("retro", False),
            "house": self._get_house(ketu_lon),
        }

        # Assign houses to all grahas
        for name in self.grahas:
            self.grahas[name]["house"] = self._get_house(self.grahas[name]["longitude"])

    def _calculate_grahas(self):
        """Calculate sidereal positions of all 8 grahas (Ketu added separately)"""
        grahas = {}
        for planet_id, name in GRAHAS.items():
            flags = swe.FLG_SWIEPH | swe.FLG_SIDEREAL
            result = swe.calc_ut(self.jd, planet_id, flags)

            lon = result[0][0]  # Sidereal longitude
            lat = result[0][1]
            speed = result[0][3]  # Daily motion

            rashi = int(lon / 30)
            degree = lon % 30
            nak_idx = int(lon / (360 / 27))
            pada = int((lon % (360 / 27)) / (360 / 108)) + 1

            grahas[name] = {
                "longitude": lon,
                "latitude": lat,
                "speed": speed,
                "rashi": rashi,
                "rashi_name": RASHI_SHORT[rashi],
                "degree": degree,
                "nakshatra": NAKSHATRAS[nak_idx],
                "nakshatra_idx": nak_idx,
                "pada": pada,
                "retro": speed < 0,
                "house": 0,  # Set later
            }
        return grahas

    def _calculate_houses(self):
        """Calculate Lagna (Ascendant) and 12 house cusps"""
        # Placidus house system (most common), sidereal
        cusps, ascmc = swe.houses_ex(self.jd, self.lat, self.lon,
                                      b'P',  # Placidus
                                      swe.FLG_SIDEREAL)
        lagna = ascmc[0]  # Ascendant degree (sidereal)

        houses = []
        for i in range(12):
            houses.append({
                "house": i + 1,
                "cusp": cusps[i],
                "rashi": int(cusps[i] / 30),
                "rashi_name": RASHI_SHORT[int(cusps[i] / 30)],
                "degree": cusps[i] % 30,
                "lord": RASHI_LORDS[int(cusps[i] / 30)],
            })

        return lagna, houses

    def _get_house(self, longitude):
        """Determine which house a planet falls in"""
        cusps = [h["cusp"] for h in self.houses]
        for i in range(12):
            next_i = (i + 1) % 12
            start = cusps[i]
            end = cusps[next_i]

            if end < start:  # Wraps around 360
                if longitude >= start or longitude < end:
                    return i + 1
            else:
                if start <= longitude < end:
                    return i + 1
        return 1  # Default

    def get_graha_status(self, graha_name):
        """Check if graha is exalted, debilitated, own sign, etc."""
        if graha_name not in self.grahas:
            return "unknown"

        rashi = self.grahas[graha_name]["rashi"]

        if graha_name in EXALTATION and EXALTATION[graha_name] == rashi:
            return "Uchcha (Exalted)"
        if graha_name in DEBILITATION and DEBILITATION[graha_name] == rashi:
            return "Neecha (Debilitated)"
        if RASHI_LORDS.get(rashi) == graha_name:
            return "Svakshetra (Own Sign)"
        return "Normal"

    def get_vimshottari_dasha(self):
        """Calculate Vimshottari Mahadasha periods from birth"""
        moon = self.grahas["Chandra"]
        nak_idx = moon["nakshatra_idx"]
        nak_lord = NAKSHATRA_LORDS[nak_idx]

        # How much of the nakshatra is remaining at birth
        nak_span = 360 / 27  # 13.333 degrees per nakshatra
        nak_start = nak_idx * nak_span
        elapsed_in_nak = moon["longitude"] - nak_start
        remaining_fraction = 1 - (elapsed_in_nak / nak_span)

        # Find starting dasha lord
        start_idx = DASHA_SEQUENCE.index(nak_lord)

        dashas = []
        current_date = self.birth_date

        # First dasha (partial)
        lord = DASHA_SEQUENCE[start_idx]
        years = DASHA_YEARS[lord] * remaining_fraction
        days = int(years * 365.25)
        end_date = datetime.fromordinal(current_date.toordinal() + days)
        dashas.append({
            "lord": lord,
            "years": round(years, 2),
            "start": current_date.strftime("%Y-%m-%d"),
            "end": end_date.strftime("%Y-%m-%d"),
        })
        current_date = end_date

        # Remaining dashas (full cycles)
        for i in range(1, 10):
            idx = (start_idx + i) % 9
            lord = DASHA_SEQUENCE[idx]
            years = DASHA_YEARS[lord]
            days = int(years * 365.25)
            end_date = datetime.fromordinal(current_date.toordinal() + days)
            dashas.append({
                "lord": lord,
                "years": years,
                "start": current_date.strftime("%Y-%m-%d"),
                "end": end_date.strftime("%Y-%m-%d"),
            })
            current_date = end_date

        return dashas

    def get_yogas(self):
        """Detect major Vedic yogas in the chart"""
        yogas = []

        # Helper: get rashi of a graha
        def rashi(name):
            return self.grahas[name]["rashi"]

        def house(name):
            return self.grahas[name]["house"]

        lagna_rashi = int(self.lagna / 30)

        # 1. Gajakesari Yoga: Jupiter in kendra from Moon
        moon_rashi = rashi("Chandra")
        guru_rashi = rashi("Guru")
        diff = (guru_rashi - moon_rashi) % 12
        if diff in [0, 3, 6, 9]:
            yogas.append({
                "name": "Gajakesari Yoga",
                "description": "Guru in kendra from Chandra - wisdom, fame, prosperity",
                "grahas": ["Guru", "Chandra"],
                "strength": "Strong" if not self.grahas["Guru"]["retro"] else "Moderate"
            })

        # 2. Budhaditya Yoga: Sun + Mercury in same rashi
        if rashi("Surya") == rashi("Budh"):
            yogas.append({
                "name": "Budhaditya Yoga",
                "description": "Surya + Budh in same rashi - intelligence, communication skills",
                "grahas": ["Surya", "Budh"],
                "strength": "Strong"
            })

        # 3. Chandra-Mangal Yoga: Moon + Mars in same rashi
        if rashi("Chandra") == rashi("Mangal"):
            yogas.append({
                "name": "Chandra-Mangal Yoga",
                "description": "Chandra + Mangal together - wealth through courage",
                "grahas": ["Chandra", "Mangal"],
                "strength": "Strong"
            })

        # 4. Panch Mahapurusha Yogas (Mars, Mercury, Jupiter, Venus, Saturn in kendra in own/exalted sign)
        mahapurusha = {
            "Mangal": ("Ruchaka Yoga", "Courage, leadership, military success"),
            "Budh": ("Bhadra Yoga", "Intelligence, business acumen, eloquence"),
            "Guru": ("Hamsa Yoga", "Spirituality, wisdom, righteous living"),
            "Shukra": ("Malavya Yoga", "Beauty, luxury, artistic talents"),
            "Shani": ("Sasa Yoga", "Power, authority, discipline"),
        }
        for graha, (yoga_name, desc) in mahapurusha.items():
            h = house(graha)
            r = rashi(graha)
            is_kendra = h in [1, 4, 7, 10]
            is_own = RASHI_LORDS.get(r) == graha
            is_exalted = EXALTATION.get(graha) == r
            if is_kendra and (is_own or is_exalted):
                yogas.append({
                    "name": yoga_name,
                    "description": desc,
                    "grahas": [graha],
                    "strength": "Very Strong"
                })

        # 5. Mangal Dosha (Kuja Dosha): Mars in 1, 2, 4, 7, 8, 12
        mars_house = house("Mangal")
        if mars_house in [1, 2, 4, 7, 8, 12]:
            yogas.append({
                "name": "Mangal Dosha (Kuja Dosha)",
                "description": f"Mangal in house {mars_house} - may affect marriage. Check cancellation conditions.",
                "grahas": ["Mangal"],
                "strength": "Applicable"
            })

        # 6. Raj Yoga: Lords of kendra (1,4,7,10) and trikona (1,5,9) connected
        kendra_lords = set()
        trikona_lords = set()
        for h in self.houses:
            if h["house"] in [1, 4, 7, 10]:
                kendra_lords.add(h["lord"])
            if h["house"] in [1, 5, 9]:
                trikona_lords.add(h["lord"])

        raj_yoga_lords = kendra_lords & trikona_lords
        if raj_yoga_lords:
            yogas.append({
                "name": "Raj Yoga",
                "description": f"Kendra-Trikona lords connected ({', '.join(raj_yoga_lords)}) - success, authority, fame",
                "grahas": list(raj_yoga_lords),
                "strength": "Strong"
            })

        # 7. Vipareeta Raj Yoga: Lords of 6, 8, 12 in 6, 8, or 12
        dusthana_houses = [6, 8, 12]
        dusthana_lords = []
        for h in self.houses:
            if h["house"] in dusthana_houses:
                dusthana_lords.append((h["house"], h["lord"]))
        for dh, dl in dusthana_lords:
            if dl in self.grahas:
                dl_house = house(dl)
                if dl_house in dusthana_houses and dl_house != dh:
                    yogas.append({
                        "name": "Vipareeta Raj Yoga",
                        "description": f"{dl} (lord of {dh}) in house {dl_house} - success through adversity",
                        "grahas": [dl],
                        "strength": "Moderate"
                    })
                    break

        return yogas

    def to_dict(self):
        """Export full Kundali as dictionary"""
        return {
            "name": self.name,
            "place": self.place,
            "birth_date": self.birth_date.strftime("%Y-%m-%d %H:%M:%S"),
            "latitude": self.lat,
            "longitude": self.lon,
            "timezone": self.tz,
            "ayanamsha": round(self.ayanamsha_value, 4),
            "lagna": {
                "degree": round(self.lagna, 4),
                "rashi": RASHI_SHORT[int(self.lagna / 30)],
                "rashi_idx": int(self.lagna / 30),
                "nakshatra": NAKSHATRAS[int(self.lagna / (360 / 27))],
            },
            "grahas": self.grahas,
            "houses": self.houses,
            "yogas": self.get_yogas(),
            "dashas": self.get_vimshottari_dasha(),
        }

    def print_chart(self):
        """Pretty print the Kundali with Hindi explanations"""

        # Hindi names for grahas
        GRAHA_HINDI = {
            "Surya": "सूर्य (Sun)", "Chandra": "चन्द्र (Moon)", "Mangal": "मंगल (Mars)",
            "Budh": "बुध (Mercury)", "Guru": "गुरु (Jupiter)", "Shukra": "शुक्र (Venus)",
            "Shani": "शनि (Saturn)", "Rahu": "राहु (N.Node)", "Ketu": "केतु (S.Node)"
        }

        # Hindi names for rashis
        RASHI_HINDI = {
            "Mesha": "मेष (Aries)", "Vrishabha": "वृषभ (Taurus)", "Mithuna": "मिथुन (Gemini)",
            "Karka": "कर्क (Cancer)", "Simha": "सिंह (Leo)", "Kanya": "कन्या (Virgo)",
            "Tula": "तुला (Libra)", "Vrischika": "वृश्चिक (Scorpio)", "Dhanu": "धनु (Sagittarius)",
            "Makara": "मकर (Capricorn)", "Kumbha": "कुम्भ (Aquarius)", "Meena": "मीन (Pisces)"
        }

        # Hindi names for nakshatras
        NAKSHATRA_HINDI = {
            "Ashwini": "अश्विनी", "Bharani": "भरणी", "Krittika": "कृत्तिका",
            "Rohini": "रोहिणी", "Mrigashira": "मृगशिरा", "Ardra": "आर्द्रा",
            "Punarvasu": "पुनर्वसु", "Pushya": "पुष्य", "Ashlesha": "आश्लेषा",
            "Magha": "मघा", "Purva Phalguni": "पूर्वा फाल्गुनी", "Uttara Phalguni": "उत्तरा फाल्गुनी",
            "Hasta": "हस्त", "Chitra": "चित्रा", "Swati": "स्वाति",
            "Vishakha": "विशाखा", "Anuradha": "अनुराधा", "Jyeshtha": "ज्येष्ठा",
            "Moola": "मूल", "Purva Ashadha": "पूर्वाषाढ़ा", "Uttara Ashadha": "उत्तराषाढ़ा",
            "Shravana": "श्रवण", "Dhanishtha": "धनिष्ठा", "Shatabhisha": "शतभिषा",
            "Purva Bhadrapada": "पूर्वभाद्रपद", "Uttara Bhadrapada": "उत्तरभाद्रपद", "Revati": "रेवती"
        }

        # Hindi house meanings
        HOUSE_HINDI = {
            1: "तनु भाव (Self/शरीर)", 2: "धन भाव (Wealth/धन)",
            3: "सहज भाव (Siblings/भाई-बहन)", 4: "सुख भाव (Happiness/माता)",
            5: "सन्तान भाव (Children/संतान)", 6: "रोग भाव (Enemies/रोग)",
            7: "दारा भाव (Marriage/विवाह)", 8: "आयु भाव (Longevity/आयु)",
            9: "भाग्य भाव (Fortune/भाग्य)", 10: "कर्म भाव (Career/कर्म)",
            11: "लाभ भाव (Gains/लाभ)", 12: "व्यय भाव (Loss/मोक्ष)"
        }

        # Status Hindi
        STATUS_HINDI = {
            "Uchcha (Exalted)": "उच्च (बहुत बलवान - शुभ)",
            "Neecha (Debilitated)": "नीच (दुर्बल - अशुभ प्रभाव)",
            "Svakshetra (Own Sign)": "स्वक्षेत्री (अपनी राशि में - बलवान)",
            "Normal": "सामान्य"
        }

        print("\n" + "=" * 75)
        print("  जन्म कुण्डली (JANAM KUNDALI / BIRTH CHART)")
        print("=" * 75)
        if self.name:
            print(f"  नाम (Name): {self.name}")
        print(f"  जन्म तिथि (Birth): {self.birth_date.strftime('%d %B %Y, %I:%M %p')}")
        if self.place:
            print(f"  जन्म स्थान (Place): {self.place} ({self.lat}°N, {self.lon}°E)")
        print(f"  अयनांश (Ayanamsha - Lahiri): {self.ayanamsha_value:.4f}°")
        print(f"  जूलियन दिवस (Julian Day): {self.jd:.6f}")
        print("=" * 75)

        # Lagna
        lagna_rashi = int(self.lagna / 30)
        lagna_nak = int(self.lagna / (360 / 27))
        lagna_rashi_hi = RASHI_HINDI.get(RASHI_SHORT[lagna_rashi], RASHI_SHORT[lagna_rashi])
        lagna_nak_hi = NAKSHATRA_HINDI.get(NAKSHATRAS[lagna_nak], NAKSHATRAS[lagna_nak])
        print(f"\n  लग्न (LAGNA/ASCENDANT):")
        print(f"  राशि: {lagna_rashi_hi}")
        print(f"  अंश: {self.lagna % 30:.2f}°")
        print(f"  नक्षत्र: {lagna_nak_hi} ({NAKSHATRAS[lagna_nak]})")
        print(f"  --- लग्न का अर्थ: यह आपके व्यक्तित्व और शारीरिक गठन को दर्शाता है ---")

        # Graha positions
        print(f"\n  ग्रह स्थिति (GRAHA POSITIONS / PLANETARY POSITIONS)")
        print(f"  {'─' * 70}")
        print(f"  {'ग्रह':<16} {'राशि':<20} {'अंश':>6}  {'नक्षत्र':<16} {'पाद':>4} {'भाव':>4}  {'स्थिति'}")
        print(f"  {'─' * 70}")

        graha_order = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu"]
        for gname in graha_order:
            g = self.grahas[gname]
            retro = " (वक्री)" if g["retro"] else ""
            status = self.get_graha_status(gname)
            g_hindi = GRAHA_HINDI.get(gname, gname)
            r_hindi = RASHI_HINDI.get(g['rashi_name'], g['rashi_name'])
            n_hindi = NAKSHATRA_HINDI.get(g['nakshatra'], g['nakshatra'])
            s_hindi = STATUS_HINDI.get(status, status)
            print(f"  {g_hindi:<16} {r_hindi:<20} {g['degree']:>5.1f}°  {n_hindi:<16} {g['pada']:>4} {g['house']:>4}   {s_hindi}{retro}")

        # Hindi explanation of key placements
        print(f"\n  मुख्य ग्रह विश्लेषण (KEY PLANETARY ANALYSIS):")
        print(f"  {'─' * 70}")

        # Surya analysis
        surya = self.grahas["Surya"]
        print(f"  सूर्य {RASHI_HINDI.get(surya['rashi_name'], '')} में भाव {surya['house']} में:")
        surya_status = self.get_graha_status("Surya")
        if surya_status == "Uchcha (Exalted)":
            print(f"    -> सूर्य उच्च का है! आत्मविश्वास, नेतृत्व और पिता का सुख मिलेगा।")
        elif surya_status == "Neecha (Debilitated)":
            print(f"    -> सूर्य नीच का है। आत्मविश्वास में कमी हो सकती है, पिता से कठिनाई।")
        else:
            print(f"    -> आत्मा, पिता, सरकारी कार्य और नेतृत्व का कारक।")

        # Chandra analysis
        chandra = self.grahas["Chandra"]
        chandra_nak = NAKSHATRA_HINDI.get(chandra['nakshatra'], chandra['nakshatra'])
        print(f"  चन्द्र {RASHI_HINDI.get(chandra['rashi_name'], '')} में, नक्षत्र: {chandra_nak}:")
        print(f"    -> चन्द्र राशि ही आपकी 'राशि' (Moon Sign) है।")
        print(f"    -> मन, माता, भावनाएं और मानसिक स्थिति का कारक।")

        # Mangal analysis
        mangal = self.grahas["Mangal"]
        if mangal['house'] in [1, 2, 4, 7, 8, 12]:
            print(f"  मंगल भाव {mangal['house']} में - मांगलिक दोष सम्भव।")
            print(f"    -> विवाह में देरी या कठिनाई हो सकती है। उपाय से शान्ति हो सकती है।")

        # Houses
        print(f"\n  भाव चक्र (HOUSE CHART / BHAVA CHAKRA)")
        print(f"  {'─' * 65}")
        print(f"  {'भाव':<6} {'राशि':<20} {'अंश':>6}  {'स्वामी':<16} {'अर्थ'}")
        print(f"  {'─' * 65}")
        for h in self.houses:
            r_hindi = RASHI_HINDI.get(h['rashi_name'], h['rashi_name'])
            l_hindi = GRAHA_HINDI.get(h['lord'], h['lord'])
            h_meaning = HOUSE_HINDI.get(h['house'], "")
            print(f"  {h['house']:<6} {r_hindi:<20} {h['degree']:>5.1f}°  {l_hindi:<16} {h_meaning}")

        # Yogas
        yogas = self.get_yogas()
        if yogas:
            print(f"\n  योग (YOGAS DETECTED / विशेष ग्रह संयोग)")
            print(f"  {'─' * 65}")

            YOGA_HINDI = {
                "Gajakesari Yoga": "गजकेसरी योग - गुरु चन्द्र से केन्द्र में\n    फल: बुद्धि, यश, समृद्धि और समाज में सम्मान मिलता है।",
                "Budhaditya Yoga": "बुधादित्य योग - सूर्य और बुध एक राशि में\n    फल: तीव्र बुद्धि, वाक्पटुता और शिक्षा में सफलता।",
                "Chandra-Mangal Yoga": "चन्द्र-मंगल योग - चन्द्र और मंगल एक राशि में\n    फल: साहस से धन प्राप्ति, व्यापार में लाभ।",
                "Ruchaka Yoga": "रुचक योग (पंचमहापुरुष) - मंगल केन्द्र में स्वराशि/उच्च\n    फल: शौर्य, नेतृत्व, सैन्य/पुलिस में सफलता।",
                "Bhadra Yoga": "भद्र योग (पंचमहापुरुष) - बुध केन्द्र में स्वराशि/उच्च\n    फल: व्यापार कुशलता, वाक्पटुता, बुद्धिमत्ता।",
                "Hamsa Yoga": "हंस योग (पंचमहापुरुष) - गुरु केन्द्र में स्वराशि/उच्च\n    फल: आध्यात्मिकता, ज्ञान, धार्मिक जीवन।",
                "Malavya Yoga": "मालव्य योग (पंचमहापुरुष) - शुक्र केन्द्र में स्वराशि/उच्च\n    फल: सौन्दर्य, विलासिता, कला में प्रतिभा।",
                "Sasa Yoga": "शश योग (पंचमहापुरुष) - शनि केन्द्र में स्वराशि/उच्च\n    फल: शक्ति, अधिकार, अनुशासन, राजनीति में सफलता।",
                "Mangal Dosha (Kuja Dosha)": "मांगलिक दोष - मंगल 1/2/4/7/8/12 भाव में\n    प्रभाव: विवाह में विलम्ब या कठिनाई। उपाय आवश्यक।",
                "Raj Yoga": "राज योग - केन्द्र और त्रिकोण के स्वामी सम्बन्धित\n    फल: सफलता, अधिकार, यश और प्रतिष्ठा।",
                "Vipareeta Raj Yoga": "विपरीत राज योग - दुःस्थान स्वामी दुःस्थान में\n    फल: कठिनाइयों से सफलता, अप्रत्याशित लाभ।",
            }

            for y in yogas:
                print(f"\n  * {y['name']} [{y['strength']}]")
                hindi_desc = YOGA_HINDI.get(y['name'], f"    {y['description']}")
                print(f"    {hindi_desc}")
        else:
            print(f"\n  कोई प्रमुख योग नहीं पाया गया।")

        # Vimshottari Dasha
        dashas = self.get_vimshottari_dasha()
        print(f"\n  विंशोत्तरी महादशा (VIMSHOTTARI MAHADASHA)")
        print(f"  कुल चक्र: 120 वर्ष | चन्द्र नक्षत्र आधारित")
        print(f"  {'─' * 55}")
        print(f"  {'दशा स्वामी':<16} {'अवधि':>8}   {'आरम्भ':<12} {'समाप्ति':<12}")
        print(f"  {'─' * 55}")

        from datetime import datetime as dt
        now = dt.now()
        current_dasha = None

        for d in dashas:
            lord_hi = GRAHA_HINDI.get(d['lord'], d['lord'])
            start_dt = dt.strptime(d['start'], "%Y-%m-%d")
            end_dt = dt.strptime(d['end'], "%Y-%m-%d")
            marker = ""
            if start_dt <= now <= end_dt:
                marker = " <-- वर्तमान (CURRENT)"
                current_dasha = d
            print(f"  {lord_hi:<16} {d['years']:>6.2f} वर्ष  {d['start']}  {d['end']}{marker}")

        if current_dasha:
            print(f"\n  अभी चल रही महादशा: {GRAHA_HINDI.get(current_dasha['lord'], current_dasha['lord'])}")
            print(f"  ({current_dasha['start']} से {current_dasha['end']} तक)")

        print(f"\n{'=' * 75}")
        print(f"  गणना: pyswisseph + Lahiri Ayanamsha | Brahm AI Jyotish Module")
        print(f"{'=' * 75}\n")


# ============================================
# STANDALONE TEST
# ============================================
if __name__ == "__main__":
    print("Test 1: Amaresh Singh - Robertsganj, 14 April 2003, 8:30 PM")
    k = Kundali(
        year=2003, month=4, day=14,
        hour=20, minute=30, second=0,
        lat=24.6917104, lon=83.0817920, tz=5.5,
        name="Amaresh Singh",
        place="Robertsganj (Sonbhadra, UP)"
    )
    k.print_chart()

    print("\n\nTest 2: New Delhi, 15 May 1990, 10:30 AM")
    k2 = Kundali(
        year=1990, month=5, day=15,
        hour=10, minute=30, second=0,
        lat=28.6139, lon=77.2090, tz=5.5,
        name="Test Person 2",
        place="New Delhi"
    )
    k2.print_chart()
