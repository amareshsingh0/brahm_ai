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

    private var conversationId: String? = null

    fun sendMessage(text: String) {
        // Add user message immediately
        _messages.value = _messages.value + ChatMessage("user", text)
        _isStreaming.value = true

        // Start streaming assistant response
        val assistantIndex = _messages.value.size
        _messages.value = _messages.value + ChatMessage("assistant", "")

        viewModelScope.launch {
            var buffer = ""
            sseManager.streamChat(text, conversationId)
                .catch { e ->
                    // Replace last message with error
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
