package com.bimoraai.brahm.core.data

import android.content.Context
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscription data — plan info, daily usage, and feature flags.
 *
 * Free plan (no subscription): only "panchang" feature is unlocked.
 * Paid plans: features list is controlled entirely by admin.
 */
data class SubscriptionInfo(
    val planId:              String         = "free",
    val planName:            String         = "Free",
    val priceInr:            Int            = 0,
    val dailyMessageLimit:   Int?           = 0,   // null = unlimited
    val dailyTokenLimit:     Int?           = 0,
    val badgeText:           String?        = null,
    val status:              String         = "none", // "active" | "expired" | "none"
    val expiresAt:           String?        = null,
    val daysRemaining:       Int?           = null,
    val messagesUsedToday:   Int            = 0,
    val tokensUsedToday:     Int            = 0,
    val features:            List<String>   = listOf("panchang"),
    val isFree:              Boolean        = true,
)

@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs      = context.getSharedPreferences("brahm_sub_cache", Context.MODE_PRIVATE)
    private val jsonCodec  = Json { ignoreUnknownKeys = true }

    private val _info = MutableStateFlow(loadCached())
    val info: StateFlow<SubscriptionInfo> = _info.asStateFlow()

    // ── Public API ──────────────────────────────────────────────────────────

    /** Fetch subscription from backend and update cache. Call on login + app resume. */
    fun refresh() {
        scope.launch {
            try {
                val res = api.getSubscription()
                if (res.isSuccessful) {
                    val body = res.body() ?: return@launch
                    val parsed = parse(body)
                    _info.value = parsed
                    saveCache(parsed)
                }
            } catch (_: Exception) { /* keep cached value */ }
        }
    }

    /** Check if the user has access to a specific feature. Fail-open (true) if unknown. */
    fun hasFeature(featureKey: String): Boolean =
        _info.value.features.contains(featureKey)

    /** True if user has an active paid subscription. */
    val isPaid: Boolean get() = !_info.value.isFree && _info.value.status == "active"

    /** True if subscription has expired. */
    val isExpired: Boolean get() = _info.value.status == "expired"

    /** Messages remaining today (null = unlimited). */
    val messagesRemaining: Int?
        get() {
            val limit = _info.value.dailyMessageLimit ?: return null
            if (limit == 0) return null
            return maxOf(0, limit - _info.value.messagesUsedToday)
        }

    // ── Parsing ─────────────────────────────────────────────────────────────

    private fun parse(obj: JsonObject): SubscriptionInfo {
        fun str(key: String)  = obj[key]?.jsonPrimitive?.contentOrNull
        fun int_(key: String) = obj[key]?.jsonPrimitive?.intOrNull
        fun bool(key: String) = obj[key]?.jsonPrimitive?.booleanOrNull ?: false

        val features = try {
            obj["features"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: listOf("panchang")
        } catch (_: Exception) { listOf("panchang") }

        return SubscriptionInfo(
            planId            = str("plan_id")            ?: "free",
            planName          = str("plan_name")          ?: "Free",
            priceInr          = int_("price_inr")         ?: 0,
            dailyMessageLimit = int_("daily_message_limit"),
            dailyTokenLimit   = int_("daily_token_limit"),
            badgeText         = str("badge_text"),
            status            = str("status")             ?: "none",
            expiresAt         = str("expires_at"),
            daysRemaining     = int_("days_remaining"),
            messagesUsedToday = int_("messages_used_today") ?: 0,
            tokensUsedToday   = int_("tokens_used_today")   ?: 0,
            features          = features,
            isFree            = bool("is_free"),
        )
    }

    // ── Cache (SharedPreferences) ────────────────────────────────────────────

    private fun loadCached(): SubscriptionInfo {
        val json = prefs.getString("sub_info", null) ?: return SubscriptionInfo()
        return try {
            val obj = jsonCodec.parseToJsonElement(json) as JsonObject
            parse(obj)
        } catch (_: Exception) { SubscriptionInfo() }
    }

    private fun saveCache(info: SubscriptionInfo) {
        val obj = buildString {
            append("""{"plan_id":"${info.planId}","plan_name":"${info.planName}",""")
            append(""""price_inr":${info.priceInr},"status":"${info.status}",""")
            append(""""messages_used_today":${info.messagesUsedToday},""")
            append(""""tokens_used_today":${info.tokensUsedToday},""")
            append(""""is_free":${info.isFree},""")
            append(""""daily_message_limit":${info.dailyMessageLimit ?: "null"},""")
            append(""""daily_token_limit":${info.dailyTokenLimit ?: "null"},""")
            append(""""features":${info.features.joinToString(",", "[", "]") { "\"$it\"" }}""")
            append("}")
        }
        prefs.edit().putString("sub_info", obj).apply()
    }
}
