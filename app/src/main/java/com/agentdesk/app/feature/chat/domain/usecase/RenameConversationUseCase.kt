package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class RenameConversationUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(id: String, newTitle: String) {
        val conversation = repository.getConversation(id) ?: return
        repository.updateConversation(conversation.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }
}
