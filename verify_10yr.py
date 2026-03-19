"""
10-Year Festival Verification Script
Run on VM: python3 ~/books/verify_10yr.py
Checks 2024-2033 for date anomalies.
"""
import sys, os
sys.path.insert(0, os.path.expanduser("~/books"))
os.environ.setdefault("EPHE_PATH", os.path.expanduser("~/books/data/swiss_ephe"))

from api.services.festival_service import get_festival_calendar

# ── Expected Gregorian month ranges (min_month, max_month) ───────────────────
# If a festival falls outside this range it's flagged as anomaly
EXPECTED = {
    "Makar Sankranti":       (1,  1),
    "Maha Shivaratri":       (2,  3),
    "Holika Dahan":          (2,  4),
    "Holi":                  (2,  4),
    "Gudi Padwa":            (3,  4),
    "Chaitra Navratri":      (3,  4),
    "Ram Navami":            (3,  5),
    "Akshaya Tritiya":       (4,  5),
    "Parshuram Jayanti":     (4,  5),
    "Buddha Purnima":        (4,  6),
    "Narasimha Jayanti":     (4,  6),
    "Nirjala Ekadashi":      (5,  7),
    "Vat Savitri":           (5,  6),
    "Guru Purnima":          (7,  8),
    "Hariyali Teej":         (7,  9),
    "Janmashtami":           (8,  9),   # Krishna Janmashtami
    "Krishna Janmashtami":   (8,  9),
    "Kajari Teej":           (8,  9),
    "Ganesh Chaturthi":      (8,  9),
    "Hartalika Teej":        (8,  9),
    "Onam":                  (8,  9),
    "Anant Chaturdashi":     (9, 10),
    "Mahalaya":              (9, 10),
    "Sharad Navratri":       (9, 11),
    "Dussehra":              (9, 11),
    "Karva Chauth":          (10, 11),
    "Ahoi Ashtami":          (10, 11),
    "Dhanteras":             (10, 11),
    "Naraka Chaturdashi":    (10, 11),
    "Diwali":                (10, 11),
    "Govardhan Puja":        (10, 11),
    "Bhai Dooj":             (10, 11),
    "Skanda Sashti":         (10, 12),
    "Tulsi Vivah":           (10, 12),
    "Kartik Purnima":        (11, 12),
    "Geeta Jayanti":         (11, 12),
    "Dattatreya Jayanti":    (11, 12),
}

LAT, LON, TZ = 28.61, 77.21, 5.5  # New Delhi

print(f"{'Year':<6} {'Date':<12} {'Festival':<30} {'Tithi':<20} {'Month':<14} {'Status'}")
print("-" * 105)

anomalies = []
prev_dates = {}  # festival → previous year's date (for consecutive year jump check)

for year in range(2024, 2034):
    try:
        festivals = get_festival_calendar(year, LAT, LON, TZ,
                                          tradition="smarta", lunar_system="amanta")
    except Exception as e:
        print(f"{year}  ERROR: {e}")
        continue

    # Sort by date
    festivals.sort(key=lambda f: f.get("date", ""))

    # Check for Adhik Maas note
    adhik = next((f for f in festivals if "adhik" in f.get("name","").lower()), None)

    year_anomalies = 0
    for f in festivals:
        name    = f.get("name", "")
        date_s  = f.get("date", "")
        tithi   = f"{f.get('paksha','')[:1]}{f.get('tithi_name','')}"
        month   = f.get("month", "")
        dosh    = f.get("dosh_notes", [])

        if not date_s:
            continue

        try:
            greg_month = int(date_s[5:7])
        except:
            greg_month = 0

        # Check expected range
        status = "✅"
        if name in EXPECTED:
            lo, hi = EXPECTED[name]
            if not (lo <= greg_month <= hi):
                status = f"❌ WRONG MONTH (got {greg_month}, expect {lo}-{hi})"
                year_anomalies += 1
                anomalies.append((year, date_s, name, status))

        # Check consecutive year jump (> 40 days = likely 29-day off error)
        key = name
        if key in prev_dates:
            from datetime import date as dt
            try:
                d1 = dt.fromisoformat(prev_dates[key])
                d2 = dt.fromisoformat(date_s)
                diff = abs((d2 - d1).days)
                # Inter-year diff is ~354 days (lunar yr) to ~384 days (Adhik yr).
                # Flag only if outside 300-430 range — means festival landed in wrong month.
                if diff < 300 or diff > 430:
                    status += f" ⚠️  JUMP {diff}d from {prev_dates[key]}"
                    if "WRONG" not in status:
                        year_anomalies += 1
                        anomalies.append((year, date_s, name, f"jump {diff}d"))
            except:
                pass

        prev_dates[key] = date_s

        flag = "  " if status == "✅" else ">>>"
        print(f"{flag}{year:<4} {date_s:<12} {name:<30} {tithi:<20} {month:<14} {status}")

    tag = ""
    # Check for duplicate festivals in same year (same name twice)
    names = [f.get("name") for f in festivals]
    dups = [n for n in set(names) if names.count(n) > 1]
    if dups:
        print(f"  *** DUPLICATE in {year}: {dups}")
        year_anomalies += len(dups)

    summary = f"✅ {len(festivals)} festivals"
    if year_anomalies:
        summary = f"❌ {year_anomalies} ANOMALIES — {len(festivals)} festivals total"
    print(f"  {'─'*60} {year}: {summary}\n")

# ── Summary ───────────────────────────────────────────────────────────────────
print("\n" + "="*80)
print("ANOMALY SUMMARY")
print("="*80)
if not anomalies:
    print("No anomalies found across 2024-2033!")
else:
    for year, date_s, name, issue in anomalies:
        print(f"  {year}  {date_s}  {name:<30}  {issue}")
