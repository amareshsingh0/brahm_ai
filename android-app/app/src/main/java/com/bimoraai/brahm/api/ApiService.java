package com.bimoraai.brahm.api;

import com.bimoraai.brahm.api.models.KundaliRequest;
import com.bimoraai.brahm.api.models.SendOtpRequest;
import com.bimoraai.brahm.api.models.SendOtpResponse;
import com.bimoraai.brahm.api.models.VerifyOtpRequest;
import com.bimoraai.brahm.api.models.VerifyOtpResponse;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for all Brahm AI backend endpoints.
 *
 * Base URL: https://brahmasmi.bimoraai.com/
 *
 * All endpoints that return variable / schema-free JSON use {@link JsonObject}
 * so callers can navigate the response freely without needing a separate POJO
 * for every endpoint.
 *
 * Auth / OTP endpoints use typed request/response POJOs for compile-time safety.
 */
public interface ApiService {

    // ─────────────────────────────────────────────────────────────────────────
    // Auth
    // ─────────────────────────────────────────────────────────────────────────

    /** Step 1 — send a one-time password to the given phone number. */
    @POST("api/auth/send-otp")
    Call<SendOtpResponse> sendOtp(@Body SendOtpRequest req);

    /** Step 2 — verify OTP and receive a JWT access token. */
    @POST("api/auth/verify-otp")
    Call<VerifyOtpResponse> verifyOtp(@Body VerifyOtpRequest req);

    // ─────────────────────────────────────────────────────────────────────────
    // Kundali
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate a full Drik-level Kundali for the given birth data.
     * Response contains: grahas, lagna, bhav_chalit, shadbala,
     * ashtakavarga, upagraha, yogas, dashas, alerts.
     */
    @POST("api/kundali")
    Call<JsonObject> getKundali(@Body KundaliRequest req);

    // ─────────────────────────────────────────────────────────────────────────
    // Panchang
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Daily Panchang for the given coordinates and date.
     *
     * @param lat  Latitude  (decimal degrees)
     * @param lon  Longitude (decimal degrees)
     * @param tz   UTC offset in hours (e.g. 5.5 for IST)
     * @param date ISO date string "YYYY-MM-DD"
     */
    @GET("api/panchang")
    Call<JsonObject> getPanchang(
        @Query("lat")  double lat,
        @Query("lon")  double lon,
        @Query("tz")   double tz,
        @Query("date") String date
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Gochar (Transit)
    // ─────────────────────────────────────────────────────────────────────────

    /** Current planetary positions (transit sky chart). */
    @GET("api/gochar")
    Call<JsonObject> getGochar(
        @Query("lat") double lat,
        @Query("lon") double lon,
        @Query("tz")  double tz
    );

    /**
     * Analyse transits against the user's natal chart.
     * Body should contain birth_date, birth_time, lat, lon, tz.
     */
    @POST("api/gochar/analyze")
    Call<JsonObject> analyzeGochar(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // Horoscope
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Daily horoscope for a rashi.
     *
     * @param rashi Rashi slug e.g. "mesh", "vrishabha" (lowercase, no spaces).
     */
    @GET("api/horoscope/{rashi}")
    Call<JsonObject> getHoroscope(@Path("rashi") String rashi);

    // ─────────────────────────────────────────────────────────────────────────
    // Compatibility (Kundali Milan)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Guna Milan / Nakshatra compatibility between two people.
     * Body: { person1: {date,time,lat,lon,tz}, person2: {date,time,lat,lon,tz} }
     */
    @POST("api/compatibility")
    Call<JsonObject> getCompatibility(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // Muhurta
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Find auspicious timings for a given activity.
     * Body: { activity, lat, lon, tz, date_from, date_to }
     */
    @POST("api/muhurta/activity")
    Call<JsonObject> getMuhurta(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // Sade Sati
    // ─────────────────────────────────────────────────────────────────────────

    @GET("api/sade-sati")
    Call<JsonObject> getSadeSati(
        @Query("lat")  double lat,
        @Query("lon")  double lon,
        @Query("tz")   double tz,
        @Query("date") String date,
        @Query("time") String time
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Dosha
    // ─────────────────────────────────────────────────────────────────────────

    @GET("api/dosha")
    Call<JsonObject> getDosha(
        @Query("lat")  double lat,
        @Query("lon")  double lon,
        @Query("tz")   double tz,
        @Query("date") String date,
        @Query("time") String time
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Gemstones
    // ─────────────────────────────────────────────────────────────────────────

    @GET("api/gemstones")
    Call<JsonObject> getGemstones(
        @Query("lat")  double lat,
        @Query("lon")  double lon,
        @Query("tz")   double tz,
        @Query("date") String date,
        @Query("time") String time
    );

    // ─────────────────────────────────────────────────────────────────────────
    // KP System
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * KP sub-lord analysis.
     * Body: { date, time, lat, lon, tz, name }
     */
    @POST("api/kp")
    Call<JsonObject> getKP(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // Prashna (Horary)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Prashna Kundali for the current moment.
     * Body: { question, date, time, lat, lon, tz }
     */
    @POST("api/prashna")
    Call<JsonObject> getPrashna(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // Varshphal (Solar Return)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Solar Return chart for a given target year.
     * Body: { date, time, lat, lon, tz, year }
     */
    @POST("api/varshphal")
    Call<JsonObject> getVarshphal(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // Birth Time Rectification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rectify birth time using life events.
     * Body: { date, approx_time_from, approx_time_to, lat, lon, tz, life_events }
     */
    @POST("api/rectification")
    Call<JsonObject> getRectification(@Body JsonObject body);

    // ─────────────────────────────────────────────────────────────────────────
    // City search
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * City autocomplete.
     *
     * @param query Partial city name (at least 2 characters).
     * Response: { cities: [{name, lat, lon, tz, country}] }
     */
    @GET("api/cities")
    Call<JsonObject> searchCities(@Query("q") String query);

    // ─────────────────────────────────────────────────────────────────────────
    // Sky / Planets now
    // ─────────────────────────────────────────────────────────────────────────

    /** Current positions of all planets in the sky. */
    @GET("api/planets/now")
    Call<JsonObject> getPlanetsNow(
        @Query("lat") double lat,
        @Query("lon") double lon,
        @Query("tz")  double tz
    );
}
