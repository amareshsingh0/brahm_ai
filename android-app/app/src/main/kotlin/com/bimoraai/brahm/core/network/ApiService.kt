package com.bimoraai.brahm.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.*

// ─── Request / Response models ───────────────────────────────────────────────

@Serializable data class SendOtpRequest(val phone: String, val purpose: String = "login")
@Serializable data class SendOtpResponse(
    val sent: Boolean,
    val message: String,
    val test_otp: String? = null,   // returned in TEST_MODE
)

@Serializable data class VerifyOtpRequest(val phone: String, val otp: String, val purpose: String = "login")
@Serializable data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val user_id: String,
    val name: String? = null,
    val plan: String = "free",
    val phone: String? = null,
    val phone_verified: Boolean = false,
)

@Serializable data class GoogleAuthRequest(val id_token: String, val device_type: String = "android")

// Used by GET /user — full profile
@Serializable data class UserDto(
    val session_id: String = "",
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val plan: String = "free",
    val date: String = "",        // birth_date YYYY-MM-DD
    val time: String = "",        // birth_time HH:MM
    val place: String = "",       // birth_place
    val gender: String = "",      // Male | Female | Other | Prefer not to say
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tz: Double = 5.5,
    val rashi: String = "",
    val nakshatra: String = "",
)

// Used by POST /user — update profile
@Serializable data class UpdateProfileRequest(
    val session_id: String = "",
    val name: String = "",
    val date: String = "",
    val time: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tz: Double = 5.5,
    val place: String = "",
    val gender: String = "",
    val rashi: String = "",
    val nakshatra: String = "",
    val language: String = "english",
    val plan: String = "free",
    val phone: String? = null,
    val email: String? = null,
)

// PanchangRequest removed — backend uses GET with query params, not POST body

@Serializable data class KundaliRequest(
    val name: String = "",
    val date: String,            // YYYY-MM-DD
    val time: String,            // HH:MM
    val place: String = "",
    val lat: Double,
    val lon: Double,
    val tz: Double = 5.5,
    val ayanamsha: String = "lahiri",
    val rahu_mode: String = "mean",
    val calc_options: List<String> = listOf(
        "antardasha", "pratyantar", "sukshma",
        "ashtakavarga", "shadbala", "upagraha",
        // Varga charts — D-1 and D-9 always included in main response
        "d2", "d3", "d4", "d5", "d6", "d7", "d8",
        "d10", "d11", "d12", "d16", "d20", "d24", "d27",
        "d30", "d40", "d45", "d60",
    ),
)

@Serializable data class SaveKundaliRequest(
    val name: String = "",
    val birth_date: String,
    val birth_time: String,
    val birth_lat: Double,
    val birth_lon: Double,
    val birth_tz: Double = 5.5,
    val birth_place: String = "",
    val kundali_json: String,
)

@Serializable data class ChatRequest(
    val message: String,
    val conversation_id: String? = null,
)

@Serializable data class RefreshRequest(val refresh_token: String)

// City search
@Serializable data class City(
    val name: String,
    val label: String = "",
    val country: String = "",
    val lat: Double,
    val lon: Double,
    val tz: Double = 5.5,
)
@Serializable data class CitiesResponse(val cities: List<City>)
@Serializable data class CitySearchResponse(val results: List<City>)

// ─── API endpoints ────────────────────────────────────────────────────────────

interface ApiService {

    // Auth
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body body: SendOtpRequest): Response<SendOtpResponse>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // Cities — use search endpoint (GeoNames 200K+ DB) instead of legacy 730-city JSON
    @GET("cities/search")
    suspend fun searchCities(@Query("q") query: String, @Query("limit") limit: Int = 8): Response<CitySearchResponse>

    @GET("geocode")
    suspend fun geocode(@Query("q") query: String): Response<City>

    // User profile
    @GET("user")
    suspend fun getMe(): Response<UserDto>

    // Chat history sessions
    @GET("user/chats/sessions")
    suspend fun getChatSessions(
        @Query("include_archived") includeArchived: Boolean = false,
    ): Response<JsonObject>

    @DELETE("user/chats/session/{sessId}")
    suspend fun deleteChatSession(@Path("sessId") sessId: String): Response<JsonObject>

    @PATCH("user/chats/session/{sessId}/meta")
    suspend fun patchSessionMeta(
        @Path("sessId") sessId: String,
        @Body body: JsonObject,
    ): Response<JsonObject>

    @POST("user")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UserDto>

    // Panchang — GET with query params (backend: GET /api/panchang?date=&lat=&lon=&tz=)
    @GET("panchang")
    suspend fun getPanchang(
        @Query("date") date: String,
        @Query("lat")  lat: Double,
        @Query("lon")  lon: Double,
        @Query("tz")   tz: Double = 5.5,   // IST offset in hours (NOT "Asia/Kolkata")
    ): Response<JsonObject>

    // Kundali
    @POST("kundali")
    suspend fun generateKundali(@Body body: KundaliRequest): Response<JsonObject>

    @GET("user/kundali")
    suspend fun getSavedKundali(): Response<JsonObject>

    @POST("user/kundali")
    suspend fun saveKundali(@Body body: SaveKundaliRequest): Response<JsonObject>

    // Gochar — current sky (GET, no body)
    @GET("gochar")
    suspend fun getGocharNow(): Response<JsonObject>

    // All other astrology endpoints — POST with dynamic bodies
    @POST("compatibility")
    suspend fun getCompatibility(@Body body: JsonObject): Response<JsonObject>

    @POST("muhurta/activity")
    suspend fun getMuhurta(@Body body: JsonObject): Response<JsonObject>

    @POST("kp")
    suspend fun getKP(@Body body: JsonObject): Response<JsonObject>

    @POST("prashna")
    suspend fun getPrashna(@Body body: JsonObject): Response<JsonObject>

    @POST("varshphal")
    suspend fun getVarshphal(@Body body: JsonObject): Response<JsonObject>

    @POST("rectification")
    suspend fun getRectification(@Body body: JsonObject): Response<JsonObject>

    @GET("horoscope/{rashi}")
    suspend fun getHoroscope(
        @Path("rashi") rashi: String,
        @Query("period") period: String = "daily",
    ): Response<JsonObject>

    // Palmistry (multipart — image upload)
    @Multipart
    @POST("palmistry/analyze")
    suspend fun analyzePalm(
        @Part image: okhttp3.MultipartBody.Part,
        @Query("hand_role") handRole: String = "dominant",
    ): Response<JsonObject>

    @Multipart
    @POST("palmistry/analyze-both")
    suspend fun analyzeBothPalms(
        @Part dominant: okhttp3.MultipartBody.Part,
        @Part non_dominant: okhttp3.MultipartBody.Part,
        @Query("dominant_hand") dominantHand: String = "right",
    ): Response<JsonObject>

    // Sky — current planetary positions
    @GET("planets/now")
    suspend fun getPlanetsNow(): Response<JsonObject>

    // Vedic Library — RAG search
    @GET("search")
    suspend fun searchVedicLibrary(@Query("q") query: String): Response<JsonObject>

    // Festivals — full year (for PanchangScreen Festivals tab)
    @GET("festivals")
    suspend fun getFestivalsYear(
        @Query("year") year: Int,
        @Query("lat")  lat: Double,
        @Query("lon")  lon: Double,
        @Query("tz")   tz: Double = 5.5,
    ): Response<JsonObject>

    // Festivals — per month (legacy, kept for compatibility)
    @GET("festivals")
    suspend fun getFestivals(
        @Query("month") month: Int,
        @Query("year")  year: Int,
    ): Response<JsonObject>

    // Grahan / Eclipse calendar (for PanchangScreen)
    @GET("grahan")
    suspend fun getGrahan(): Response<JsonObject>

    // Gochar personal analysis
    @POST("gochar/analyze")
    suspend fun analyzeGochar(@Body body: JsonObject): Response<JsonObject>

    // Calendar — monthly grid with per-day tithi, festivals, special day flags
    @GET("calendar/month")
    suspend fun getCalendar(
        @Query("year")         year: Int,
        @Query("month")        month: Int,
        @Query("lat")          lat: Double,
        @Query("lon")          lon: Double,
        @Query("tz")           tz: Double = 5.5,
        @Query("tradition")    tradition: String = "smarta",
        @Query("lunar_system") lunarSystem: String = "amanta",
    ): Response<JsonObject>
}
