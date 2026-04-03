"""
One-time script: Build cities.db from GeoNames data.
Sources:
  - IN.txt         : All Indian places (villages, towns, cities) ~750K entries
  - cities5000.txt : Worldwide cities with pop > 5000 ~80K entries

Together: ~800K places, final DB ~30-35MB on disk.

Download commands (run from ~/books/api/data/):
  wget https://download.geonames.org/export/dump/IN.zip && unzip IN.zip
  wget https://download.geonames.org/export/dump/cities5000.zip && unzip cities5000.zip

Run:
  python3 api/scripts/build_cities_db.py

GeoNames columns (tab-separated):
0:geonameid  1:name  2:asciiname  3:alternatenames  4:lat  5:lon
6:feature_class  7:feature_code  8:country_code  9:cc2
10:admin1_code  11:admin2_code  12:admin3_code  13:admin4_code
14:population  15:elevation  16:dem  17:timezone  18:modification_date
"""
import sqlite3
import os
import sys

DATA_DIR = os.path.join(os.path.dirname(__file__), "../data")
OUTPUT   = os.path.join(DATA_DIR, "cities.db")

# Source files: (path, country_filter, feature_class_filter)
# feature_class 'P' = populated places (cities, towns, villages, hamlets)
# feature_class 'A' = administrative divisions (keep for search fallback)
SOURCES = [
    # All Indian populated places — every village included
    (os.path.join(DATA_DIR, "IN.txt"),          "IN",  {"P", "A"}),
    # Worldwide cities with pop > 5000 (excludes India to avoid dupes)
    (os.path.join(DATA_DIR, "cities5000.txt"),  None,  {"P"}),
]

COUNTRY_NAMES = {
    "IN": "India", "US": "United States", "GB": "United Kingdom",
    "AU": "Australia", "CA": "Canada", "PK": "Pakistan", "BD": "Bangladesh",
    "NP": "Nepal", "LK": "Sri Lanka", "AE": "UAE", "SA": "Saudi Arabia",
    "SG": "Singapore", "MY": "Malaysia", "DE": "Germany", "FR": "France",
    "IT": "Italy", "ES": "Spain", "BR": "Brazil", "MX": "Mexico",
    "ZA": "South Africa", "NG": "Nigeria", "KE": "Kenya", "EG": "Egypt",
    "RU": "Russia", "CN": "China", "JP": "Japan", "KR": "South Korea",
    "ID": "Indonesia", "PH": "Philippines", "TH": "Thailand", "VN": "Vietnam",
    "NL": "Netherlands", "BE": "Belgium", "SE": "Sweden", "NO": "Norway",
    "DK": "Denmark", "FI": "Finland", "PL": "Poland", "CZ": "Czech Republic",
    "AT": "Austria", "CH": "Switzerland", "PT": "Portugal", "TR": "Turkey",
    "IR": "Iran", "IQ": "Iraq", "AF": "Afghanistan", "MM": "Myanmar",
    "KH": "Cambodia", "LA": "Laos", "TW": "Taiwan", "HK": "Hong Kong",
    "NZ": "New Zealand", "ZW": "Zimbabwe", "ET": "Ethiopia", "GH": "Ghana",
    "TZ": "Tanzania", "UG": "Uganda", "CM": "Cameroon", "CI": "Ivory Coast",
    "MV": "Maldives", "BT": "Bhutan", "MU": "Mauritius",
}


def build():
    # Validate at least one source exists
    found = [s for s in SOURCES if os.path.exists(s[0])]
    if not found:
        print("ERROR: No source files found in api/data/")
        print("Download commands:")
        print("  cd ~/books/api/data")
        print("  wget https://download.geonames.org/export/dump/IN.zip && unzip IN.zip")
        print("  wget https://download.geonames.org/export/dump/cities5000.zip && unzip cities5000.zip")
        sys.exit(1)

    if os.path.exists(OUTPUT):
        os.remove(OUTPUT)

    conn = sqlite3.connect(OUTPUT)
    cur  = conn.cursor()

    cur.execute("""
        CREATE TABLE cities (
            id           INTEGER PRIMARY KEY,
            name         TEXT NOT NULL,
            ascii_name   TEXT NOT NULL,
            lat          REAL NOT NULL,
            lon          REAL NOT NULL,
            country_code TEXT,
            country_name TEXT,
            timezone     TEXT,
            population   INTEGER,
            feature_code TEXT
        )
    """)
    cur.execute("CREATE INDEX idx_ascii      ON cities(ascii_name COLLATE NOCASE)")
    cur.execute("CREATE INDEX idx_name       ON cities(name COLLATE NOCASE)")
    cur.execute("CREATE INDEX idx_country    ON cities(country_code)")
    cur.execute("CREATE INDEX idx_population ON cities(population DESC)")

    seen_ids = set()
    total = 0

    for filepath, country_only, feature_classes in SOURCES:
        if not os.path.exists(filepath):
            print(f"  SKIP (not found): {filepath}")
            continue

        count = 0
        with open(filepath, encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) < 19:
                    continue
                try:
                    gid          = int(parts[0])
                    country_code = parts[8]

                    # Skip duplicates (IN.txt entries already added)
                    if gid in seen_ids:
                        continue
                    # If this source is non-India worldwide, skip India rows (IN.txt has them)
                    if country_only is None and country_code == "IN":
                        continue
                    # Feature class filter
                    if parts[6] not in feature_classes:
                        continue

                    name         = parts[1]
                    ascii_name   = parts[2]
                    lat          = float(parts[4])
                    lon          = float(parts[5])
                    timezone     = parts[17]
                    population   = int(parts[14]) if parts[14].strip() else 0
                    feature_code = parts[7]
                    country_name = COUNTRY_NAMES.get(country_code, country_code)

                    cur.execute(
                        "INSERT INTO cities VALUES (?,?,?,?,?,?,?,?,?,?)",
                        (gid, name, ascii_name, lat, lon,
                         country_code, country_name, timezone, population, feature_code)
                    )
                    seen_ids.add(gid)
                    count += 1
                except (ValueError, IndexError):
                    continue

        conn.commit()
        total += count
        print(f"  {os.path.basename(filepath)}: {count:,} rows inserted")

    conn.close()
    size_mb = os.path.getsize(OUTPUT) / 1024 / 1024
    print(f"\nDone! {total:,} total places → {OUTPUT} ({size_mb:.1f} MB)")
    print("Indian villages + worldwide cities ready.")


if __name__ == "__main__":
    build()
