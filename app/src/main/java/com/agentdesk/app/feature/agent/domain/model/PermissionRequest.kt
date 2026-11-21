package com.agentdesk.app.feature.agent.domain.model

import kotlinx.serialization.json.JsonObject

data class PermissionRequest(
    val id: String,
    val runId: String,
    val toolName: String,
    val summary: ToolSummary,
    val level: PermissionLevel,
    val args: JsonObject
)
