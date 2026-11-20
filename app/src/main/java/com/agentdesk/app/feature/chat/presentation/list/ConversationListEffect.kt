package com.agentdesk.app.feature.chat.presentation.list

import com.agentdesk.app.core.mvi.UiEffect

sealed interface ConversationListEffect : UiEffect {
    data class OpenChat(val conversationId: String) : ConversationListEffect
    data object OpenProvidersSetup : ConversationListEffect
}
