package com.bimoraai.brahm.data.chat

import com.bimoraai.brahm.BuildConfig
import com.bimoraai.brahm.core.datastore.TokenDataStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenDataStore: TokenDataStore,
) {
    fun streamChat(message: String, conversationId: String?): Flow<String> = callbackFlow {
        val token = tokenDataStore.accessToken.firstOrNull()
        val body = buildString {
            append("{\"message\":\"${message.replace("\"", "\\\"")}\",")
            append("\"stream\":true")
            if (conversationId != null) append(",\"conversation_id\":\"$conversationId\"")
            append("}")
        }

        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}chat/stream")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                } else {
                    trySend(data)
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

private fun String.toMediaTypeOrNull(): okhttp3.MediaType? =
    okhttp3.MediaType.parse(this)
