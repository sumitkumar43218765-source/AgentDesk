package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.chat.domain.model.Conversation
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val repository: ChatRepository,
    private val idGenerator: IdGenerator
) {
    suspend operator fun invoke(
        title: String,
        providerId: String,
        model: String,
        projectId: String? = null
    ): Conversation {
        val conversation = Conversation(
            id = idGenerator.newId(),
            projectId = projectId,
            title = title.take(40).ifBlank { "New Chat" },
            providerId = providerId,
            model = model
        )
        repository.saveConversation(conversation)
        return conversation
    }
}
