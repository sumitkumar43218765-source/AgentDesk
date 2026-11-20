package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class RetryLastUseCase @Inject constructor(
    private val repository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase
) {
    suspend operator fun invoke(
        conversationId: String,
        messages: List<ChatMessage>
    ): Flow<LlmEvent> {
        val lastUserMsg = messages.lastOrNull { it.role == Role.USER } ?: return emptyFlow()
        val text = lastUserMsg.blocks.filterIsInstance<ContentBlock.Text>().joinToString("\n") { it.text }
        val historyBeforeLastUser = messages.takeWhile { it.id != lastUserMsg.id }

        // Remove the failed assistant message and last user message from DB before retrying
        val assistantMessagesToDelete = messages.filter { it.role == Role.ASSISTANT && it.createdAt >= lastUserMsg.createdAt }
        for (m in assistantMessagesToDelete) {
            repository.deleteMessage(m.id)
        }
        repository.deleteMessage(lastUserMsg.id)

        return sendMessageUseCase(conversationId, text, historyBeforeLastUser)
    }
}
