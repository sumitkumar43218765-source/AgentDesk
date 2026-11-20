package com.agentdesk.app.feature.ai.domain.model

data class TokenUsage(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0
) {
    val totalTokens: Int
        get() = inputTokens + outputTokens
}
