package com.bimoraai.brahm.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.data.chat.SseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BotMsg(val isUser: Boolean, val text: String)

@HiltViewModel
class PageBotViewModel @Inject constructor(
    private val sseManager: SseManager,
) : ViewModel() {

    private val _msgs = MutableStateFlow<List<BotMsg>>(emptyList())
    val msgs = _msgs.asStateFlow()

    private val _streaming = MutableStateFlow(false)
    val streaming = _streaming.asStateFlow()

    fun send(text: String, pageContext: String = "general") {
        if (text.isBlank() || _streaming.value) return
        val history = _msgs.value.map { m -> (if (m.isUser) "user" else "assistant") to m.text }
        _msgs.value = _msgs.value + BotMsg(true, text)
        _msgs.value = _msgs.value + BotMsg(false, "")
        _streaming.value = true
        viewModelScope.launch {
            try {
                sseManager.streamChat(
                    message = text,
                    history = history.takeLast(6),
                    pageContext = pageContext,
                ).collect { token ->
                    val list = _msgs.value.toMutableList()
                    list[list.lastIndex] = BotMsg(false, list.last().text + token)
                    _msgs.value = list
                }
            } finally {
                _streaming.value = false
            }
        }
    }

    fun clear() { _msgs.value = emptyList() }
}
