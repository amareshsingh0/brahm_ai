"""
One-time script: Convert GeoNames cities5000.txt → SQLite DB
Run: python3 api/scripts/build_cities_db.py

GeoNames columns (tab-separated):
0:geonameid  1:name  2:asciiname  3:alternatenames  4:lat  5:lon
6:feature_class  7:feature_code  8:country_code  9:cc2
10:admin1_code  11:admin2_code  12:admin3_code  13:admin4_code
14:population  15:elevation  16:dem  17:timezone  18:modification_date
"""
import sqlite3
import os
import sys

INPUT = os.path.join(os.path.dirname(__file__), "../data/cities5000.txt")
OUTPUT = os.path.join(os.path.dirname(__file__), "../data/cities.db")

COUNTRY_NAMES = {
    "IN": "India", "US": "United States", "GB": "United Kingdom",
    "AU": "Australia", "CA": "Canada", "PK": "Pakistan", "BD": "Bangladesh",
    "NP": "Nepal", "LK": "Sri Lanka", "AE": "UAE", "SA": "Saudi Arabia",
    "SG": "Singapore", "MY": "Malaysia", "DE": "Germany", "FR": "France",
    "IT": "Italy", "ES": "Spain", "BR": "Brazil", "MX": "Mexico",
    "ZA": "South Africa", "NG": "Nigeria", "KE": "Kenya", "EG": "Egypt",
    "RU": "Russia", "CN": "China", "JP": "Japan", "KR": "South Korea",
    "ID": "Indonesia", "PH": "Philippines", "TH": "Thailand", "VN": "Vietnam",
}

def build():
    if not os.path.exists(INPUT):
        print(f"ERROR: {INPUT} not found.")
        print("Run: wget https://download.geonames.org/export/dump/cities5000.zip && unzip cities5000.zip")
        sys.exit(1)

    if os.path.exists(OUTPUT):
        os.remove(OUTPUT)

    conn = sqlite3.connect(OUTPUT)
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE cities (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            ascii_name TEXT NOT NULL,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            country_code TEXT,
            country_name TEXT,
            timezone TEXT,
            population INTEGER
        )
    """)

    cur.execute("CREATE INDEX idx_ascii ON cities(ascii_name COLLATE NOCASE)")
    cur.execute("CREATE INDEX idx_name ON cities(name COLLATE NOCASE)")
    cur.execute("CREATE INDEX idx_country ON cities(country_code)")
    cur.execute("CREATE INDEX idx_population ON cities(population DESC)")

    count = 0
    with open(INPUT, encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) < 19:
                continue
            try:
                gid = int(parts[0])
                name = parts[1]
                ascii_name = parts[2]
                lat = float(parts[4])
                lon = float(parts[5])
                country_code = parts[8]
                timezone = parts[17]
                population = int(parts[14]) if parts[14] else 0
                country_name = COUNTRY_NAMES.get(country_code, country_code)

                cur.execute(
                    "INSERT INTO cities VALUES (?,?,?,?,?,?,?,?,?)",
                    (gid, name, ascii_name, lat, lon, country_code, country_name, timezone, population)
                )
                count += 1
            except (ValueError, IndexError):
                continue

    conn.commit()
    conn.close()
    size_mb = os.path.getsize(OUTPUT) / 1024 / 1024
    print(f"Done! {count:,} cities → {OUTPUT} ({size_mb:.1f} MB)")

if __name__ == "__main__":
    build()
