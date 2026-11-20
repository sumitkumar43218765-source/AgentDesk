package com.agentdesk.app.feature.setup.domain.usecase

import com.agentdesk.app.feature.setup.domain.PostInstallCheck
import com.agentdesk.app.feature.setup.domain.model.CheckResult
import javax.inject.Inject

class RunHealthChecksUseCase @Inject constructor(
    private val checks: Set<@JvmSuppressWildcards PostInstallCheck>
) {
    suspend operator fun invoke(): Map<String, CheckResult> {
        val results = mutableMapOf<String, CheckResult>()
        for (check in checks) {
            results[check.title] = check.run()
        }
        return results
    }
}
