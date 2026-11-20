package com.agentdesk.app.feature.setup.domain.model

data class ToolProgress(
    val toolId: String,
    val name: String,
    val status: ToolInstallStatus,
    val pct: Int = 0,
    val message: String = ""
)
