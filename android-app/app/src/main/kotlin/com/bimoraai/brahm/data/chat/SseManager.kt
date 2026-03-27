package com.bimoraai.brahm.data.chat

import com.bimoraai.brahm.BuildConfig
import com.bimoraai.brahm.core.datastore.TokenDataStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenDataStore: TokenDataStore,
    private val userRepository: com.bimoraai.brahm.core.data.UserRepository,
) {
    fun streamChat(
        message: String,
        history: List<Pair<String, String>>,
        sessionId: String = "",
        userId: String = "",
        pageContext: String = "general",
    ): Flow<String> = callbackFlow {
        val token = tokenDataStore.accessToken.firstOrNull()
        val user = userRepository.user.value

        val historyJson = history.joinToString(",", "[", "]") { (role, content) ->
            "{\"role\":\"${role}\",\"content\":\"${content.replace("\\", "\\\\").replace("\"", "\\\"")}\"}"
        }
        val escapedMsg = message.replace("\\", "\\\\").replace("\"", "\\\"")

        // Build page_data with user birth info so backend can generate kundali without birth_form
        val pageDataJson = if (user != null && user.date.isNotBlank() && user.place.isNotBlank()) {
            val escapedName  = user.name.replace("\\", "\\\\").replace("\"", "\\\"")
            val escapedDate  = user.date.replace("\"", "\\\"")
            val escapedTime  = user.time.replace("\"", "\\\"")
            val escapedPlace = user.place.replace("\\", "\\\\").replace("\"", "\\\"")
            "{\"name\":\"$escapedName\",\"date\":\"$escapedDate\",\"time\":\"$escapedTime\",\"place\":\"$escapedPlace\",\"lat\":${user.lat},\"lon\":${user.lon},\"tz\":${user.tz}}"
        } else "{}"

        val body = buildString {
            append("{\"message\":\"$escapedMsg\"")
            append(",\"history\":$historyJson")
            append(",\"language\":\"hi\"")
            append(",\"page_context\":\"$pageContext\"")
            append(",\"page_data\":$pageDataJson")
            if (sessionId.isNotBlank()) append(",\"session_id\":\"$sessionId\"")
            if (userId.isNotBlank()) append(",\"user_id\":\"$userId\"")
            append("}")
        }

        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}chat")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") { close(); return }
                try {
                    val json = JSONObject(data)
                    when (json.optString("type")) {
                        "token" -> {
                            val content = json.optString("content", "")
                            if (content.isNotEmpty()) trySend(content)
                        }
                        "birth_form" -> {
                            // Backend needs birth details — prompt user to fill profile
                            trySend("Aapki kundali banana ke liye janam vivaran chahiye.\n\nKripya pehle **Profile** mein apna naam, janam tithi, samay aur sthan save karein, phir dobara poochein.")
                            close()
                        }
                        "error" -> {
                            val msg = json.optString("message", "Server error")
                            trySend("Error: $msg")
                            close()
                        }
                        // save_kundali_prompt and other events — ignore silently
                    }
                } catch (_: Exception) { /* Not JSON — skip */ }
            }

            override fun onClosed(eventSource: EventSource) { close() }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val msg = when (response?.code) {
                    503 -> "Server abhi busy hai, thodi der baad try karein."
                    401 -> "Session expire ho gayi, please login karein."
                    else -> t?.message
                }
                if (msg != null) trySend(msg)
                close(t)
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}
