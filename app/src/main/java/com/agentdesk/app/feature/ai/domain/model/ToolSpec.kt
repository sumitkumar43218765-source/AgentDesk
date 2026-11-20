package com.agentdesk.app.feature.ai.domain.model

data class ToolSpec(
    val name: String,
    val description: String,
    val inputSchemaJson: String
)
