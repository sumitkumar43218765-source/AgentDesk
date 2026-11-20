package com.agentdesk.app.feature.chat.presentation.list

import com.agentdesk.app.core.mvi.UiState
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import com.agentdesk.app.feature.chat.domain.model.Conversation

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val providers: List<ProviderConfig> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
) : UiState
