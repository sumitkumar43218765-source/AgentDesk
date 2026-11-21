package com.agentdesk.app.feature.agent.domain.model

sealed interface PermissionDecision {
    data object AllowOnce : PermissionDecision
    data object AllowSession : PermissionDecision
    data object AllowProject : PermissionDecision
    data class Deny(val reason: String? = null) : PermissionDecision
}
