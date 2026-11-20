package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.ToolRegistry
import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.LlmMessage
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.ai.domain.model.StopReason
import com.agentdesk.app.feature.ai.domain.model.TokenUsage
import com.agentdesk.app.feature.chat.domain.BasePrompts
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.MessageStatus
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
    private val providerRegistry: ProviderRegistry,
    private val toolRegistry: ToolRegistry,
    private val basePrompts: BasePrompts,
    private val idGenerator: IdGenerator
) {

    suspend operator fun invoke(
        conversationId: String,
        userText: String,
        history: List<ChatMessage>
    ): Flow<LlmEvent> = flow {
        if (userText.length > 100_000) {
            emit(LlmEvent.Error(AppError.Unknown("Message too long (>100KB)")))
            return@flow
        }

        val conversation = repository.getConversation(conversationId)
        if (conversation == null) {
            emit(LlmEvent.Error(AppError.Storage("Conversation not found")))
            return@flow
        }

        val provider = providerRegistry.getProvider(conversation.providerId)
        if (provider == null) {
            emit(LlmEvent.Error(AppError.Auth("Provider not configured. Please add an API key.")))
            return@flow
        }

        // Save user message
        val userMsgId = idGenerator.newId()
        val userMessage = ChatMessage(
            id = userMsgId,
            conversationId = conversationId,
            role = Role.USER,
            blocks = listOf(ContentBlock.Text(userText)),
            status = MessageStatus.COMPLETE
        )
        repository.saveMessage(userMessage)

        // Create pending assistant message
        val assistantMsgId = idGenerator.newId()
        val assistantMessage = ChatMessage(
            id = assistantMsgId,
            conversationId = conversationId,
            role = Role.ASSISTANT,
            blocks = emptyList(),
            status = MessageStatus.STREAMING
        )
        repository.saveMessage(assistantMessage)

        // Build messages list for LLM
        val llmMessages = mutableListOf<LlmMessage>()
        for (m in history) {
            llmMessages.add(LlmMessage(m.role, m.blocks))
        }
        llmMessages.add(LlmMessage(Role.USER, listOf(ContentBlock.Text(userText))))

        val request = LlmRequest(
            model = conversation.model,
            system = basePrompts.getChatBasePrompt(),
            messages = llmMessages,
            tools = toolRegistry.getTools()
        )

        val fullTextBuilder = StringBuilder()
        val toolUseBlocks = mutableListOf<ContentBlock.ToolUse>()
        var finalUsage = TokenUsage()

        try {
            provider.stream(request).collect { event ->
                when (event) {
                    is LlmEvent.TextDelta -> {
                        fullTextBuilder.append(event.text)
                        emit(event)
                    }
                    is LlmEvent.ToolCallCompleted -> {
                        toolUseBlocks.add(ContentBlock.ToolUse(event.id, event.name, event.argsJson))
                        emit(event)
                    }
                    is LlmEvent.Usage -> {
                        finalUsage = TokenUsage(
                            inputTokens = if (event.inputTokens > 0) event.inputTokens else finalUsage.inputTokens,
                            outputTokens = if (event.outputTokens > 0) event.outputTokens else finalUsage.outputTokens
                        )
                        emit(event)
                    }
                    is LlmEvent.Done -> {
                        val blocks = mutableListOf<ContentBlock>()
                        if (fullTextBuilder.isNotEmpty()) {
                            blocks.add(ContentBlock.Text(fullTextBuilder.toString()))
                        }
                        blocks.addAll(toolUseBlocks)

                        repository.updateMessage(
                            assistantMessage.copy(
                                blocks = blocks,
                                status = MessageStatus.COMPLETE,
                                usage = finalUsage
                            )
                        )
                        emit(event)
                    }
                    is LlmEvent.Error -> {
                        val blocks = if (fullTextBuilder.isNotEmpty()) {
                            listOf(ContentBlock.Text(fullTextBuilder.toString()))
                        } else emptyList()

                        repository.updateMessage(
                            assistantMessage.copy(
                                blocks = blocks,
                                status = MessageStatus.ERROR
                            )
                        )
                        emit(event)
                    }
                    else -> emit(event)
                }
            }
        } catch (e: Exception) {
            val blocks = if (fullTextBuilder.isNotEmpty()) {
                listOf(ContentBlock.Text(fullTextBuilder.toString()))
            } else emptyList()

            repository.updateMessage(
                assistantMessage.copy(
                    blocks = blocks,
                    status = MessageStatus.ERROR
                )
            )
            emit(LlmEvent.Error(AppError.Network(e.message ?: "Stream failed", e)))
        }
    }
}
