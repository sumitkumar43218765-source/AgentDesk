package com.agentdesk.app.feature.agent.domain.model

data class PermissionGrant(
    val id: String,
    val projectId: String,
    val toolName: String,
    val scopeKey: String,
    val grantedAt: Long
)
