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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sseManager: SseManager,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isStreaming = MutableStateFlow(false)

    val messages = _messages.asStateFlow()
    val isStreaming = _isStreaming.asStateFlow()

    fun sendMessage(text: String) {
        // Snapshot history before adding new user message
        val historySnapshot = _messages.value.map { Pair(it.role, it.content) }

        // Add user message immediately
        _messages.value = _messages.value + ChatMessage("user", text)
        _isStreaming.value = true

        // Add empty assistant bubble
        _messages.value = _messages.value + ChatMessage("assistant", "")

        viewModelScope.launch {
            var buffer = ""
            sseManager.streamChat(text, historySnapshot)
                .catch { e ->
                    updateLast("Error: ${e.message ?: "Network error"}")
                    _isStreaming.value = false
                }
                .onCompletion { _isStreaming.value = false }
                .collect { token ->
                    buffer += token
                    updateLast(buffer)
                }
        }
    }

    private fun updateLast(content: String) {
        val list = _messages.value.toMutableList()
        if (list.isNotEmpty()) {
            list[list.size - 1] = list.last().copy(content = content)
            _messages.value = list
        }
    }
}
