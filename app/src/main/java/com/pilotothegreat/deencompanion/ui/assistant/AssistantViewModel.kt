package com.pilotothegreat.deencompanion.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.services.AssistantProxyService
import com.pilotothegreat.deencompanion.services.SpeechManager
import com.pilotothegreat.deencompanion.services.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(
    private val speechManager: SpeechManager,
    private val assistantProxyService: AssistantProxyService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val messages: StateFlow<List<AssistantMessage>> = _messages

    val speechState: StateFlow<SpeechState> = speechManager.state

    init {
        // Observe speech state changes to handle incoming speech inputs
        viewModelScope.launch {
            speechManager.state.collectLatest { state ->
                when (state) {
                    is SpeechState.ConfidenceCheck -> {
                        // User speech has been transcribed
                        addMessage(state.text, isUser = true)
                    }
                    is SpeechState.PlayingTajweed -> {
                        // Assistant responds with Arabic tajweed text
                        addMessage(state.text, isUser = false)
                    }
                    is SpeechState.PlayingTTS -> {
                        // Assistant responds with translation text
                        addMessage(state.text, isUser = false)
                    }
                    else -> {}
                }
            }
        }
    }

    fun startListening() {
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun sendMessage(query: String) {
        if (query.trim().isEmpty()) return
        addMessage(query, isUser = true)
        
        viewModelScope.launch {
            val responsePair = assistantProxyService.query(query)
            val combinedResponse = if (responsePair.first.isNotEmpty()) {
                "${responsePair.first}\n\n${responsePair.second}"
            } else {
                responsePair.second
            }
            addMessage(combinedResponse, isUser = false)
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        // Prevent duplicate messages if any
        val currentList = _messages.value
        if (currentList.isNotEmpty() && currentList.last().text == text && currentList.last().isUser == isUser) {
            return
        }
        _messages.value = currentList + AssistantMessage(text = text, isUser = isUser)
    }

    fun purgeHistory() {
        _messages.value = emptyList()
        speechManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
