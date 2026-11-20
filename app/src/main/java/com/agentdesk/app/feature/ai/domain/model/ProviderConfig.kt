package com.agentdesk.app.feature.ai.domain.model

data class ProviderConfig(
    val id: String,
    val type: ProviderType,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val extraHeaders: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
