package com.agentdesk.app.feature.ai.domain.model

data class LlmMessage(
    val role: Role,
    val blocks: List<ContentBlock>
)
