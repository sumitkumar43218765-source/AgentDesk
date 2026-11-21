package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.ai.domain.model.LlmMessage
import com.agentdesk.app.feature.chat.domain.model.ChatMessage

interface ContextManager {
    fun prepareMessages(
        systemPrompt: String,
        messages: List<ChatMessage>,
        maxContextTokens: Int = 100_000
    ): List<LlmMessage>

    fun trimToolOutput(output: String, maxChars: Int = 8000): String
}
