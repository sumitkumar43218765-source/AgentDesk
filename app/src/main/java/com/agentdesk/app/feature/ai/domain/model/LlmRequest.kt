package com.agentdesk.app.feature.ai.domain.model

data class LlmRequest(
    val model: String,
    val system: String?,
    val messages: List<LlmMessage>,
    val tools: List<ToolSpec> = emptyList(),
    val maxTokens: Int = 4096,
    val temperature: Double? = null
)
