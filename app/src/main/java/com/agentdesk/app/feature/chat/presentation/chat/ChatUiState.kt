package com.agentdesk.app.feature.chat.presentation.chat

import com.agentdesk.app.core.mvi.UiState
import com.agentdesk.app.feature.ai.domain.model.ModelInfo
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.Conversation

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingPartialText: String = "",
    val availableModels: List<ModelInfo> = emptyList(),
    val isModelPickerOpen: Boolean = false,
    val error: String? = null
) : UiState
