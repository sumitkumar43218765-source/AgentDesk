package com.agentdesk.app.feature.agent.domain.model

data class ToolOutput(
    val text: String,
    val isError: Boolean,
    val artifacts: List<ArtifactRef> = emptyList()
)
