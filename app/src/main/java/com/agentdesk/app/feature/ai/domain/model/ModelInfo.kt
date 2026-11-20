package com.agentdesk.app.feature.ai.domain.model

data class ModelInfo(
    val id: String,
    val name: String,
    val contextWindow: Int = 128000,
    val supportsTools: Boolean = true
)
