package com.agentdesk.app.feature.agent.domain.model

sealed interface LoopGuardResult {
    data object Pass : LoopGuardResult
    data class Warning(val message: String) : LoopGuardResult
    data class Abort(val reason: String) : LoopGuardResult
}
