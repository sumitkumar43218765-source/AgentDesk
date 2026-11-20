package com.agentdesk.app.feature.chat.domain.model

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.ai.domain.model.TokenUsage

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: Role,
    val blocks: List<ContentBlock>,
    val status: MessageStatus = MessageStatus.COMPLETE,
    val usage: TokenUsage? = null,
    val createdAt: Long = System.currentTimeMillis()
)
