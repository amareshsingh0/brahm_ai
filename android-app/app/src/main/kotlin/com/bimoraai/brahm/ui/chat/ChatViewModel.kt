package com.bimoraai.brahm.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.data.chat.SseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val role: String,
    val content: String,
    val followUps: List<String> = emptyList(),
    val isComplete: Boolean = false,
)

data class ChatSession(
    val sessionId: String,
    val preview: String,     // first user message or custom_name
    val lastAt: String,      // ISO timestamp
    val messageCount: Int,
    val messages: List<ChatMessage>,
    val isPinned: Boolean    = false,
    val isArchived: Boolean  = false,
    val customName: String?  = null,
)

private val CONFIDENCE_REGEX = Regex("""\[CONFIDENCE:\s*\w+\]""")
private val FOLLOWUPS_REGEX  = Regex("""\[FOLLOWUPS:\s*(.*?)\]""", RegexOption.DOT_MATCHES_ALL)

private fun stripTags(raw: String): String =
    raw.replace(CONFIDENCE_REGEX, "").replace(FOLLOWUPS_REGEX, "").trim()

private fun parseFollowUps(raw: String): List<String> {
    val match = FOLLOWUPS_REGEX.find(raw) ?: return emptyList()
    return match.groupValues[1].split("|").map { it.trim().trim('"') }.filter { it.isNotBlank() }.take(3)
}

fun formatSessionDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val local = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now(ZoneId.systemDefault())
        when {
            local == today            -> "Today"
            local == today.minusDays(1) -> "Yesterday"
            local >= today.minusDays(7) -> local.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
            else                      -> local.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }
    } catch (_: Exception) { "" }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sseManager: SseManager,
    private val tokenDataStore: TokenDataStore,
    private val api: ApiService,
) : ViewModel() {

    private val _messages          = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isStreaming       = MutableStateFlow(false)
    private val _sessions          = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _archivedSessions  = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _sessionsLoading   = MutableStateFlow(false)

    val messages          = _messages.asStateFlow()
    val isStreaming       = _isStreaming.asStateFlow()
    val sessions          = _sessions.asStateFlow()
    val archivedSessions  = _archivedSessions.asStateFlow()
    val sessionsLoading   = _sessionsLoading.asStateFlow()

    private var sessionId = ""
    private var userId    = ""

    init {
        viewModelScope.launch {
            val persisted = tokenDataStore.chatSessionId.firstOrNull()
            sessionId = if (persisted.isNullOrBlank()) {
                val newId = UUID.randomUUID().toString()
                tokenDataStore.saveChatSessionId(newId)
                newId
            } else persisted

            userId = tokenDataStore.userId.firstOrNull() ?: ""
            loadAllSessions()
        }
    }

    // ── Load all sessions from server ─────────────────────────────────────────

    fun loadAllSessions() {
        viewModelScope.launch {
            _sessionsLoading.value = true
            try {
                val respNormal   = api.getChatSessions(includeArchived = false)
                val respArchived = api.getChatSessions(includeArchived = true)

                fun parseArr(resp: retrofit2.Response<JsonObject>): List<ChatSession> {
                    if (!resp.isSuccessful) return emptyList()
                    val arr = resp.body()?.get("sessions")?.jsonArray ?: return emptyList()
                    return arr.mapNotNull { el ->
                        val obj        = el.jsonObject
                        val sid        = obj["session_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val ctx        = obj["page_context"]?.jsonPrimitive?.content ?: "general"
                        if (ctx != "general") return@mapNotNull null
                        val lastAt     = obj["last_at"]?.jsonPrimitive?.content ?: ""
                        val isPinned   = obj["is_pinned"]?.jsonPrimitive?.booleanOrNull ?: false
                        val isArchived = obj["is_archived"]?.jsonPrimitive?.booleanOrNull ?: false
                        val customName = obj["custom_name"]?.jsonPrimitive?.contentOrNull

                        val msgs = obj["messages"]?.jsonArray?.mapNotNull { m ->
                            val o       = m.jsonObject
                            val role    = o["role"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            val content = o["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            if (content.isBlank()) null
                            else ChatMessage(role = role, content = stripTags(content), isComplete = true)
                        } ?: emptyList()

                        val rawPreview = msgs.firstOrNull { it.role == "user" }?.content?.take(60) ?: "Chat session"
                        ChatSession(
                            sessionId    = sid,
                            preview      = customName?.takeIf { it.isNotBlank() } ?: rawPreview,
                            lastAt       = lastAt,
                            messageCount = msgs.size,
                            messages     = msgs,
                            isPinned     = isPinned,
                            isArchived   = isArchived,
                            customName   = customName,
                        )
                    }
                }

                val parsed   = parseArr(respNormal)
                val archived = parseArr(respArchived)
                _sessions.value         = parsed
                _archivedSessions.value = archived

                if (_messages.value.isEmpty()) {
                    val current = parsed.firstOrNull { it.sessionId == sessionId }
                    if (current != null && current.messages.isNotEmpty()) {
                        _messages.value = current.messages
                    }
                }
            } catch (_: Exception) {
            } finally {
                _sessionsLoading.value = false
            }
        }
    }

    // ── Session actions ───────────────────────────────────────────────────────

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            try {
                api.deleteChatSession(session.sessionId)
                _sessions.value         = _sessions.value.filter { it.sessionId != session.sessionId }
                _archivedSessions.value = _archivedSessions.value.filter { it.sessionId != session.sessionId }
                if (sessionId == session.sessionId) startNewChat()
            } catch (_: Exception) {}
        }
    }

    private fun patchMeta(session: ChatSession, body: JsonObject, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val resp = api.patchSessionMeta(session.sessionId, body)
                if (resp.isSuccessful) onSuccess()
            } catch (_: Exception) {}
        }
    }

    fun pinSession(session: ChatSession) {
        val pinned = !session.isPinned
        val body   = buildJsonObject { put("action", if (pinned) "pin" else "unpin") }
        patchMeta(session, body) {
            _sessions.value = _sessions.value.map {
                if (it.sessionId == session.sessionId) it.copy(isPinned = pinned) else it
            }.sortedWith(compareBy({ if (it.isPinned) 0 else 1 }, { it.lastAt })).reversed()
                .let { list -> list.filter { it.isPinned } + list.filter { !it.isPinned } }
        }
    }

    fun archiveSession(session: ChatSession) {
        val body = buildJsonObject { put("action", "archive") }
        patchMeta(session, body) {
            val updated = session.copy(isPinned = false, isArchived = true)
            _sessions.value         = _sessions.value.filter { it.sessionId != session.sessionId }
            _archivedSessions.value = listOf(updated) + _archivedSessions.value
        }
    }

    fun unarchiveSession(session: ChatSession) {
        val body = buildJsonObject { put("action", "unarchive") }
        patchMeta(session, body) {
            val updated = session.copy(isArchived = false)
            _archivedSessions.value = _archivedSessions.value.filter { it.sessionId != session.sessionId }
            _sessions.value = listOf(updated) + _sessions.value
        }
    }

    fun renameSession(session: ChatSession, newName: String) {
        val body = buildJsonObject { put("action", "rename"); put("name", newName) }
        patchMeta(session, body) {
            val update = { list: List<ChatSession> ->
                list.map { if (it.sessionId == session.sessionId) it.copy(customName = newName, preview = newName) else it }
            }
            _sessions.value         = update(_sessions.value)
            _archivedSessions.value = update(_archivedSessions.value)
        }
    }

    // ── Load a specific session ───────────────────────────────────────────────

    fun loadSession(session: ChatSession) {
        sessionId = session.sessionId
        _messages.value = session.messages
        viewModelScope.launch { tokenDataStore.saveChatSessionId(sessionId) }
    }

    // ── Start a completely new chat ───────────────────────────────────────────

    fun startNewChat() {
        val newId = UUID.randomUUID().toString()
        sessionId = newId
        _messages.value = emptyList()
        viewModelScope.launch { tokenDataStore.saveChatSessionId(newId) }
    }

    // ── Send message ─────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        val historySnapshot = _messages.value.map { Pair(it.role, it.content) }

        _messages.value = _messages.value + ChatMessage("user", text, isComplete = true)
        _isStreaming.value = true
        _messages.value = _messages.value + ChatMessage("assistant", "")

        viewModelScope.launch {
            var rawBuffer = ""
            sseManager.streamChat(text, historySnapshot, sessionId, userId)
                .catch { e ->
                    updateLast("Error: ${e.message ?: "Network error"}", isComplete = true)
                    _isStreaming.value = false
                }
                .onCompletion {
                    val followUps = parseFollowUps(rawBuffer)
                    val finalText = stripTags(rawBuffer)
                    if (finalText.isBlank()) {
                        _messages.value = _messages.value.dropLast(1)
                    } else {
                        updateLast(finalText, followUps = followUps, isComplete = true)
                    }
                    _isStreaming.value = false
                }
                .collect { token ->
                    rawBuffer += token
                    updateLast(stripTags(rawBuffer))
                }
        }
    }

    fun sendFollowUp(question: String) = sendMessage(question)

    private fun updateLast(content: String, followUps: List<String> = emptyList(), isComplete: Boolean = false) {
        val list = _messages.value.toMutableList()
        if (list.isNotEmpty()) {
            list[list.size - 1] = list.last().copy(content = content, followUps = followUps, isComplete = isComplete)
            _messages.value = list
        }
    }
}
