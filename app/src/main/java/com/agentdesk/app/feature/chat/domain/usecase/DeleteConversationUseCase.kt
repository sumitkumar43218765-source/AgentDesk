package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteConversation(id)
    }
}
