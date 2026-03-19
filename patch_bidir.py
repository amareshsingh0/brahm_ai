import shutil, subprocess, sys

path = "/root/books/api/services/festival_service.py"
shutil.copy(path, "/tmp/festival_service_bak_bidir.py")
print("Backup: /tmp/festival_service_bak_bidir.py")

with open(path, "r") as f:
    src = f.read()

OLD = (
    "                # \u2500\u2500 MONTH CORRECTION \u2500\u2500\n"
    "                # Always use amanta for internal check \u2014 festival[\"month\"] fields are in Amanta convention.\n"
    "                if festival.get(\"month\"):\n"
    "                    got = _get_lunar_month_name(found_jd, tz, \"amanta\")\n"
    "                    if got and got != festival[\"month\"]:\n"
    "                        found_jd = _search_nirmala(found_jd + 29.5, target_idx)\n"
    "                        start_jd = _find_tithi_start(target_idx, found_jd)\n"
    "                        end_jd   = _find_tithi_end(target_idx, found_jd)"
)

NEW = (
    "                # \u2500\u2500 MONTH CORRECTION \u2500\u2500\n"
    "                # Always use amanta for internal check \u2014 festival[\"month\"] fields are in Amanta convention.\n"
    "                if festival.get(\"month\"):\n"
    "                    got = _get_lunar_month_name(found_jd, tz, \"amanta\")\n"
    "                    if got and got != festival[\"month\"]:\n"
    "                        # Try one month earlier first (handles late jd_center overshoot)\n"
    "                        earlier = _search_nirmala(found_jd - 29.5, target_idx)\n"
    "                        if _get_lunar_month_name(earlier, tz, \"amanta\") == festival[\"month\"]:\n"
    "                            found_jd = earlier\n"
    "                        else:\n"
    "                            found_jd = _search_nirmala(found_jd + 29.5, target_idx)\n"
    "                        start_jd = _find_tithi_start(target_idx, found_jd)\n"
    "                        end_jd   = _find_tithi_end(target_idx, found_jd)"
)

if OLD in src:
    src = src.replace(OLD, NEW, 1)
    with open(path, "w") as f:
        f.write(src)
    print("PATCH APPLIED OK")
else:
    print("ERROR: pattern not found - already patched or file differs")
    sys.exit(1)

r = subprocess.run(["python3", "-c",
    "import ast; ast.parse(open('/root/books/api/services/festival_service.py').read()); print('Syntax OK')"],
    capture_output=True, text=True)
print(r.stdout.strip() or r.stderr.strip())

r2 = subprocess.run(["grep", "-n", "Try one month earlier", path], capture_output=True, text=True)
print(r2.stdout.strip())
print("DONE - restart API with: sudo systemctl restart brahm-api")
