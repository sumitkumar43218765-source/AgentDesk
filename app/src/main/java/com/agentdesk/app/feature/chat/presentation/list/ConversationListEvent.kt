package com.agentdesk.app.feature.chat.presentation.list

import com.agentdesk.app.core.mvi.UiEvent

sealed interface ConversationListEvent : UiEvent {
    data class SearchQueryChanged(val query: String) : ConversationListEvent
    data class CreateNewChat(val providerId: String, val model: String) : ConversationListEvent
    data class DeleteConversation(val id: String) : ConversationListEvent
}
