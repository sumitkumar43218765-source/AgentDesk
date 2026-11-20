package com.agentdesk.app.feature.setup.domain.model

data class InstallProgress(
    val tools: List<ToolProgress>,
    val overallPct: Int,
    val phase: Phase,
    val activeToolId: String? = null,
    val error: String? = null
)
