package com.agentdesk.app.feature.agent.domain.model

data class ToolSummary(
    val title: String,
    val risk: RiskLevel,
    val details: String? = null,
    val diff: String? = null
)
