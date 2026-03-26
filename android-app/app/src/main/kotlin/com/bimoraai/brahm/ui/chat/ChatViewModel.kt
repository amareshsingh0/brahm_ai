package com.bimoraai.brahm.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.data.chat.SseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val role: String,
    val content: String,
    val followUps: List<String> = emptyList(),
    val isComplete: Boolean = false,
)

// Strips [CONFIDENCE: HIGH/MEDIUM/LOW] and [FOLLOWUPS: "q1" | "q2"] from display text
private val CONFIDENCE_REGEX = Regex("""\[CONFIDENCE:\s*\w+\]""")
private val FOLLOWUPS_REGEX  = Regex("""\[FOLLOWUPS:\s*(.*?)\]""", RegexOption.DOT_MATCHES_ALL)

private fun stripTags(raw: String): String =
    raw.replace(CONFIDENCE_REGEX, "")
       .replace(FOLLOWUPS_REGEX, "")
       .trim()

private fun parseFollowUps(raw: String): List<String> {
    val match = FOLLOWUPS_REGEX.find(raw) ?: return emptyList()
    return match.groupValues[1]
        .split("|")
        .map { it.trim().trim('"') }
        .filter { it.isNotBlank() }
        .take(3)
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sseManager: SseManager,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isStreaming = MutableStateFlow(false)

    val messages = _messages.asStateFlow()
    val isStreaming = _isStreaming.asStateFlow()

    fun sendMessage(text: String) {
        val historySnapshot = _messages.value.map { Pair(it.role, it.content) }

        _messages.value = _messages.value + ChatMessage("user", text, isComplete = true)
        _isStreaming.value = true
        // Empty assistant bubble — typing indicator shows until first token
        _messages.value = _messages.value + ChatMessage("assistant", "")

        viewModelScope.launch {
            var rawBuffer = ""
            sseManager.streamChat(text, historySnapshot)
                .catch { e ->
                    updateLast("Error: ${e.message ?: "Network error"}", isComplete = true)
                    _isStreaming.value = false
                }
                .onCompletion {
                    // Final parse: extract follow-ups + fully clean text
                    val followUps = parseFollowUps(rawBuffer)
                    updateLast(stripTags(rawBuffer), followUps = followUps, isComplete = true)
                    _isStreaming.value = false
                }
                .collect { token ->
                    rawBuffer += token
                    // Show clean text during streaming (no tags flicker)
                    updateLast(stripTags(rawBuffer))
                }
        }
    }

    fun sendFollowUp(question: String) = sendMessage(question)

    private fun updateLast(
        content: String,
        followUps: List<String> = emptyList(),
        isComplete: Boolean = false,
    ) {
        val list = _messages.value.toMutableList()
        if (list.isNotEmpty()) {
            list[list.size - 1] = list.last().copy(
                content = content,
                followUps = followUps,
                isComplete = isComplete,
            )
            _messages.value = list
        }
    }
}
