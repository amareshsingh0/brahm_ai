#!/bin/bash
# Patch: bidirectional month correction in festival_service.py
# Fixes 2025/2028/2030/2031 Diwali-cluster and other month-overshoot bugs

cd ~/books

# Backup
cp api/services/festival_service.py /tmp/festival_service_bak_bidir.py
echo "Backup: /tmp/festival_service_bak_bidir.py"

# Apply patch using Python
python3 << 'PYEOF'
path = "api/services/festival_service.py"
with open(path, "r") as f:
    src = f.read()

OLD = """                # \u2500\u2500 MONTH CORRECTION \u2500\u2500
                # Always use amanta for internal check \u2014 festival["month"] fields are in Amanta convention.
                if festival.get("month"):
                    got = _get_lunar_month_name(found_jd, tz, "amanta")
                    if got and got != festival["month"]:
                        found_jd = _search_nirmala(found_jd + 29.5, target_idx)
                        start_jd = _find_tithi_start(target_idx, found_jd)
                        end_jd   = _find_tithi_end(target_idx, found_jd)"""

NEW = """                # \u2500\u2500 MONTH CORRECTION \u2500\u2500
                # Always use amanta for internal check \u2014 festival["month"] fields are in Amanta convention.
                if festival.get("month"):
                    got = _get_lunar_month_name(found_jd, tz, "amanta")
                    if got and got != festival["month"]:
                        # Try one month earlier first (handles late jd_center overshoot)
                        earlier = _search_nirmala(found_jd - 29.5, target_idx)
                        if _get_lunar_month_name(earlier, tz, "amanta") == festival["month"]:
                            found_jd = earlier
                        else:
                            found_jd = _search_nirmala(found_jd + 29.5, target_idx)
                        start_jd = _find_tithi_start(target_idx, found_jd)
                        end_jd   = _find_tithi_end(target_idx, found_jd)"""

if OLD in src:
    src = src.replace(OLD, NEW, 1)
    with open(path, "w") as f:
        f.write(src)
    print("PATCH APPLIED OK")
else:
    print("ERROR: pattern not found — already patched or file differs")
PYEOF

# Verify
grep -n "Try one month earlier" ~/books/api/services/festival_service.py && echo "Grep OK" || echo "GREP FAILED"

# Syntax check
python3 -c "import ast; ast.parse(open('api/services/festival_service.py').read()); print('Syntax OK')" 2>&1

# Restart API
sudo systemctl restart brahm-api
sleep 3
sudo systemctl status brahm-api --no-pager | tail -5

echo "DONE"
