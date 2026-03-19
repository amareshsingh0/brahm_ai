import re

path = '/root/books/api/services/festival_service.py'
with open(path) as f:
    src = f.read()

orig_len = len(src)

# 1) Add _sun_lon_sid after _sun_lon
if '_sun_lon_sid' not in src:
    src = src.replace(
        'def _sun_lon(jd: float) -> float:\n    return swe.calc_ut(jd, swe.SUN)[0][0]',
        'def _sun_lon(jd: float) -> float:\n    return swe.calc_ut(jd, swe.SUN)[0][0]\n\ndef _sun_lon_sid(jd: float) -> float:\n    swe.set_sid_mode(swe.SIDM_LAHIRI)\n    return swe.calc_ut(jd, swe.SUN, swe.FLG_SIDEREAL)[0][0]',
        1
    )

# 2) fix _find_solar_entry to use sidereal
src = src.replace('return (_sun_lon(jd) - target_lon', 'return (_sun_lon_sid(jd) - target_lon', 1)

# 3) approx_day per festival rule
fixes = [
    ('"paksha": "S", "num": 5, "approx_month": 2}',
     '"paksha": "S", "num": 5, "approx_month": 2, "approx_day": 3}'),
    ('"paksha": "K", "num": 14, "approx_month": 2}',
     '"paksha": "K", "num": 14, "approx_month": 2, "approx_day": 20}'),
    ('"paksha": "S", "num": 15, "approx_month": 3, "special": "bhadra_check"}',
     '"paksha": "S", "num": 15, "approx_month": 3, "approx_day": 13, "special": "bhadra_check"}'),
    ('"paksha": "S", "num": 1, "approx_month": 3}',
     '"paksha": "S", "num": 1, "approx_month": 3, "approx_day": 20}'),
    ('"paksha": "S", "num": 9, "approx_month": 3}',
     '"paksha": "S", "num": 9, "approx_month": 3, "approx_day": 28}'),
    ('"paksha": "S", "num": 15, "approx_month": 4}',
     '"paksha": "S", "num": 15, "approx_month": 4, "approx_day": 12}'),
    ('"paksha": "S", "num": 3, "approx_month": 4}',
     '"paksha": "S", "num": 3, "approx_month": 4, "approx_day": 22}'),
    ('"paksha": "S", "num": 15, "approx_month": 5}',
     '"paksha": "S", "num": 15, "approx_month": 5, "approx_day": 12}'),
    ('"paksha": "S", "num": 15, "approx_month": 7}',
     '"paksha": "S", "num": 15, "approx_month": 7, "approx_day": 10}'),
    ('"paksha": "S", "num": 5, "approx_month": 7}',
     '"paksha": "S", "num": 5, "approx_month": 7, "approx_day": 27}'),
    ('"paksha": "S", "num": 15, "approx_month": 8, "special": "bhadra_check"}',
     '"paksha": "S", "num": 15, "approx_month": 8, "approx_day": 9, "special": "bhadra_check"}'),
    ('"paksha": "K", "num": 8, "approx_month": 8, "special": "nishita_note"}',
     '"paksha": "K", "num": 8, "approx_month": 8, "approx_day": 17, "special": "nishita_note"}'),
    ('"paksha": "S", "num": 4, "approx_month": 8}',
     '"paksha": "S", "num": 4, "approx_month": 8, "approx_day": 22}'),
    ('"paksha": "S", "start": 1, "end": 9, "approx_month": 10}',
     '"paksha": "S", "start": 1, "end": 9, "approx_month": 10, "approx_day": 5}'),
    ('"paksha": "S", "num": 10, "approx_month": 10}',
     '"paksha": "S", "num": 10, "approx_month": 10, "approx_day": 13}'),
    ('"paksha": "K", "num": 15, "approx_month": 10}',
     '"paksha": "K", "num": 15, "approx_month": 10, "approx_day": 26}'),
    ('"paksha": "S", "num": 6, "approx_month": 10}',
     '"paksha": "S", "num": 6, "approx_month": 10, "approx_day": 29}'),
    ('"paksha": "S", "num": 11, "approx_month": 11}',
     '"paksha": "S", "num": 11, "approx_month": 11, "approx_day": 6}'),
]
for old, new in fixes:
    src = src.replace(old, new, 1)

# 4) fix tithi scan: use approx_day, sorted range +-20
# Replace the approx_month block in tithi section
src = re.sub(
    r"(elif rule\[\"type\"\] == \"tithi\":\n"
    r"                target_idx   = _rule_to_idx\(rule\[\"paksha\"\], rule\[\"num\"\]\)\n"
    r"                approx_month = rule\[\"approx_month\"\])\n"
    r"                jd_center    = swe\.julday\(year, approx_month, 15, 6\.0 - tz\)\n"
    r"\n"
    r"                found_jd = None\n"
    r"                for delta in range\(-42, 43\):",
    r"\1\n"
    r"                approx_day   = rule.get(\"approx_day\", 15)\n"
    r"                jd_center    = swe.julday(year, approx_month, approx_day, 6.0 - tz)\n"
    r"\n"
    r"                found_jd = None\n"
    r"                for delta in sorted(range(-20, 21), key=abs):",
    src, count=1
)

# 5) fix tithi_range scan: use approx_day, range +-20
src = re.sub(
    r"(elif rule\[\"type\"\] == \"tithi_range\":\n"
    r"                start_idx    = _rule_to_idx\(rule\[\"paksha\"\], rule\[\"start\"\]\)\n"
    r"                end_idx      = _rule_to_idx\(rule\[\"paksha\"\], rule\[\"end\"\]\)\n"
    r"                approx_month = rule\[\"approx_month\"\])\n"
    r"                jd_center    = swe\.julday\(year, approx_month, 15, 6\.0 - tz\)\n"
    r"\n"
    r"                start_found = end_found = None\n"
    r"                for delta in range\(-42, 43\):",
    r"\1\n"
    r"                approx_day   = rule.get(\"approx_day\", 15)\n"
    r"                jd_center    = swe.julday(year, approx_month, approx_day, 6.0 - tz)\n"
    r"\n"
    r"                start_found = end_found = None\n"
    r"                for delta in range(-20, 21):",
    src, count=1
)

# 6) fix end_found guard
src = src.replace(
    'if tidx == end_idx and start_found is not None:\n                        end_found = test_jd',
    'if tidx == end_idx and start_found is not None and test_jd > start_found:\n                        end_found = test_jd',
    1
)

with open(path, 'w') as f:
    f.write(src)

print(f"Patch done. Len before={orig_len} after={len(src)}")
print("_sun_lon_sid present:     ", '_sun_lon_sid' in src)
print("sidereal in solar entry:  ", '_sun_lon_sid(jd)' in src)
print("approx_day in Diwali:     ", '"approx_day": 26' in src)
print("sorted(range(-20 in tithi:", 'sorted(range(-20' in src)
print("range(-20, 21) tithi_range:", 'range(-20, 21)' in src)
print("end_found guard:           ", 'test_jd > start_found' in src)
