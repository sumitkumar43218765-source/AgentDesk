package com.agentdesk.app.feature.chat.presentation.chat

import com.agentdesk.app.core.mvi.UiEvent

sealed interface ChatEvent : UiEvent {
    data class LoadConversation(val id: String) : ChatEvent
    data class InputTextChanged(val text: String) : ChatEvent
    data object SendMessage : ChatEvent
    data object StopStreaming : ChatEvent
    data object RetryLast : ChatEvent
    data class ChangeModel(val modelId: String) : ChatEvent
    data class ToggleModelPicker(val open: Boolean) : ChatEvent
}
