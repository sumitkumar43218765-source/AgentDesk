package com.agentdesk.app.feature.setup.domain.model

sealed interface CheckResult {
    data object Pass : CheckResult
    data class Fail(val message: String, val details: String? = null) : CheckResult
}
