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
) {
    fun streamChat(message: String, history: List<Pair<String, String>>): Flow<String> = callbackFlow {
        val token = tokenDataStore.accessToken.firstOrNull()

        // Build history JSON array
        val historyJson = history.joinToString(",", "[", "]") { (role, content) ->
            "{\"role\":\"${role}\",\"content\":\"${content.replace("\\", "\\\\").replace("\"", "\\\"")}\"}"
        }
        val escapedMsg = message.replace("\\", "\\\\").replace("\"", "\\\"")
        val body = "{\"message\":\"$escapedMsg\",\"history\":$historyJson,\"language\":\"hi\",\"page_context\":\"general\",\"page_data\":{}}"

        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}chat")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val json = JSONObject(data)
                    if (json.optString("type") == "token") {
                        val content = json.optString("content", "")
                        if (content.isNotEmpty()) trySend(content)
                    }
                    // Ignore other event types (birth_form, save_kundali_prompt, etc.)
                } catch (_: Exception) {
                    // Not JSON — skip
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t)
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}

