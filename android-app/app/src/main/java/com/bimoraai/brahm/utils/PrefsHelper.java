package com.bimoraai.brahm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Wraps EncryptedSharedPreferences for secure, type-safe storage of user
 * credentials and birth data. All string defaults are "" and numeric defaults
 * are 0.0 so callers can use isEmpty() / == 0 checks without null guards.
 *
 * Usage:
 *   PrefsHelper prefs = new PrefsHelper(context);
 *   prefs.saveName("Arjun");
 *   String name = prefs.getName();
 */
public class PrefsHelper {

    // ── Preference keys ────────────────────────────────────────────────────
    private static final String PREFS_NAME    = "brahm_secure_prefs";
    private static final String KEY_TOKEN      = "auth_token";
    private static final String KEY_NAME       = "user_name";
    private static final String KEY_PHONE      = "user_phone";
    private static final String KEY_PLAN       = "user_plan";
    private static final String KEY_BIRTH_DATE = "birth_date";
    private static final String KEY_BIRTH_TIME = "birth_time";
    private static final String KEY_BIRTH_PLACE= "birth_place";
    private static final String KEY_LAT        = "birth_lat";
    private static final String KEY_LON        = "birth_lon";
    private static final String KEY_TZ         = "birth_tz";
    private static final String KEY_KUNDALI_JSON = "kundali_json";

    private final SharedPreferences prefs;

    /**
     * Instantiates the helper. Falls back to regular (unencrypted)
     * SharedPreferences if EncryptedSharedPreferences cannot be initialised
     * (e.g. in emulators without a hardware keystore) so the app never crashes.
     */
    public PrefsHelper(Context context) {
        SharedPreferences sp;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sp = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context.getApplicationContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback — should not happen on a real device
            sp = context.getApplicationContext()
                        .getSharedPreferences(PREFS_NAME + "_plain", Context.MODE_PRIVATE);
        }
        this.prefs = sp;
    }

    // ── Auth token ──────────────────────────────────────────────────────────

    public void   saveToken(String token) { put(KEY_TOKEN, token); }
    public String getToken()              { return getString(KEY_TOKEN); }
    /** Returns true when a non-empty JWT token has been saved. */
    public boolean isLoggedIn()           { return !getToken().isEmpty(); }

    // ── User identity ───────────────────────────────────────────────────────

    public void   saveName(String name)   { put(KEY_NAME, name); }
    public String getName()               { return getString(KEY_NAME); }

    public void   savePhone(String phone) { put(KEY_PHONE, phone); }
    public String getPhone()              { return getString(KEY_PHONE); }

    /**
     * Plan tier: "free", "jyotishi", or "acharya".
     * Default is "free" so free-tier users don't need an explicit save.
     */
    public void   savePlan(String plan)   { put(KEY_PLAN, plan); }
    public String getPlan()               { return prefs.getString(KEY_PLAN, "free"); }

    // ── Birth data ──────────────────────────────────────────────────────────

    /** ISO date string, e.g. "1990-08-15". */
    public void   saveBirthDate(String date)  { put(KEY_BIRTH_DATE, date); }
    public String getBirthDate()              { return getString(KEY_BIRTH_DATE); }

    /** 24-hour time string, e.g. "14:30". */
    public void   saveBirthTime(String time)  { put(KEY_BIRTH_TIME, time); }
    public String getBirthTime()              { return getString(KEY_BIRTH_TIME); }

    /** Place name as typed by the user, e.g. "New Delhi". */
    public void   saveBirthPlace(String place){ put(KEY_BIRTH_PLACE, place); }
    public String getBirthPlace()             { return getString(KEY_BIRTH_PLACE); }

    public void   saveLat(double lat) { prefs.edit().putFloat(KEY_LAT, (float) lat).apply(); }
    public double getLat()            { return prefs.getFloat(KEY_LAT, 0f); }

    public void   saveLon(double lon) { prefs.edit().putFloat(KEY_LON, (float) lon).apply(); }
    public double getLon()            { return prefs.getFloat(KEY_LON, 0f); }

    /** UTC offset in hours, e.g. 5.5 for IST. */
    public void   saveTz(double tz)   { prefs.edit().putFloat(KEY_TZ, (float) tz).apply(); }
    public double getTz()             { return prefs.getFloat(KEY_TZ, 5.5f); }

    // ── Kundali JSON cache ───────────────────────────────────────────────────

    /**
     * Caches the full kundali JSON string returned by POST /api/kundali.
     * This avoids a network call every time the Kundali tabs are viewed.
     */
    public void   saveKundaliJson(String json) { put(KEY_KUNDALI_JSON, json); }

    /**
     * Returns the cached kundali JSON string, or "" if not yet fetched.
     * Check with {@code isEmpty()} before parsing.
     */
    public String getKundaliJson()             { return getString(KEY_KUNDALI_JSON); }

    // ── Nuke everything ─────────────────────────────────────────────────────

    /** Removes all stored preferences. Call on logout. */
    public void clear() { prefs.edit().clear().apply(); }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void   put(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    private String getString(String key) {
        return prefs.getString(key, "");
    }
}
