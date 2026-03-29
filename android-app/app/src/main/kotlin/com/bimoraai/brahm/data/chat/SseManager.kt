package com.bimoraai.brahm.data.chat

import com.bimoraai.brahm.BuildConfig
import com.bimoraai.brahm.core.datastore.TokenDataStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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
        pageData: String = "{}",          // JSON from the screen (API result)
    ): Flow<String> = callbackFlow {
        val token = tokenDataStore.accessToken.firstOrNull()
        val user = userRepository.user.value

        // Use JSONObject/JSONArray for safe serialization — avoids 422 from unescaped \n in history
        val historyArr = org.json.JSONArray().apply {
            history.forEach { (role, content) ->
                put(org.json.JSONObject().put("role", role).put("content", content))
            }
        }

        val pageDataObj = try {
            org.json.JSONObject(if (pageData.isBlank()) "{}" else pageData)
        } catch (_: Exception) { org.json.JSONObject() }

        if (user != null && user.date.isNotBlank() && user.place.isNotBlank()) {
            pageDataObj.put("user_birth_data", org.json.JSONObject().apply {
                put("name", user.name)
                put("date", user.date)
                put("time", user.time)
                put("place", user.place)
                put("lat", user.lat)
                put("lon", user.lon)
                put("tz", user.tz)
            })
        }

        val bodyObj = org.json.JSONObject().apply {
            put("message", message)
            put("history", historyArr)
            put("language", "hi")
            put("page_context", pageContext)
            put("page_data", pageDataObj)
            if (sessionId.isNotBlank()) put("session_id", sessionId)
            if (userId.isNotBlank()) put("user_id", userId)
        }
        val body = bodyObj.toString()

        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}chat")
            .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
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
