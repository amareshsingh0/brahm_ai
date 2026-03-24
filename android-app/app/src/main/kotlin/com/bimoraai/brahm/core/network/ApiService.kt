package com.bimoraai.brahm.core.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

// ─── Request / Response models ───────────────────────────────────────────────

@Serializable data class SendOtpRequest(val phone: String)
@Serializable data class SendOtpResponse(val success: Boolean, val message: String)

@Serializable data class VerifyOtpRequest(val phone: String, val otp: String)
@Serializable data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val user: UserDto,
)

@Serializable data class GoogleAuthRequest(val id_token: String)

@Serializable data class UserDto(
    val id: String,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatar_url: String? = null,
    val plan: String = "free",
)

@Serializable data class PanchangRequest(
    val date: String,            // YYYY-MM-DD
    val lat: Double,
    val lon: Double,
    val tz: String,
)

@Serializable data class KundaliRequest(
    val name: String,
    val dob: String,             // YYYY-MM-DD
    val tob: String,             // HH:MM
    val pob: String,
    val lat: Double,
    val lon: Double,
    val tz: String,
)

@Serializable data class ChatRequest(
    val message: String,
    val conversation_id: String? = null,
)

@Serializable data class RefreshRequest(val refresh_token: String)

// ─── API endpoints ────────────────────────────────────────────────────────────

interface ApiService {

    // Auth
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): Response<SendOtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // User
    @GET("user/me")
    suspend fun getMe(): Response<UserDto>

    // Panchang
    @POST("panchang")
    suspend fun getPanchang(@Body body: PanchangRequest): Response<Map<String, @Serializable Any?>>

    // Kundali
    @POST("kundali/generate")
    suspend fun generateKundali(@Body body: KundaliRequest): Response<Map<String, @Serializable Any?>>

    // All other astrology endpoints — POST with dynamic bodies
    @POST("gochar/calculate")
    suspend fun getGochar(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("compatibility/calculate")
    suspend fun getCompatibility(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("muhurta/find")
    suspend fun getMuhurta(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("sade-sati/calculate")
    suspend fun getSadeSati(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("dosha/check")
    suspend fun getDosha(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("gemstone/recommend")
    suspend fun getGemstone(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("kp/generate")
    suspend fun getKP(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("prashna/generate")
    suspend fun getPrashna(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("varshphal/generate")
    suspend fun getVarshphal(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    @POST("rectification/analyze")
    suspend fun getRectification(@Body body: Map<String, @Serializable Any?>): Response<Map<String, @Serializable Any?>>

    // Palmistry (multipart — image upload)
    @Multipart
    @POST("palmistry/analyze")
    suspend fun analyzePalm(
        @Part image: okhttp3.MultipartBody.Part,
    ): Response<Map<String, @Serializable Any?>>
}
