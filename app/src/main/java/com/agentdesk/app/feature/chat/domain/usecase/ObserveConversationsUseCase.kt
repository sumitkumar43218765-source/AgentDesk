package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.feature.chat.domain.model.Conversation
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return repository.observeConversations()
    }
}
