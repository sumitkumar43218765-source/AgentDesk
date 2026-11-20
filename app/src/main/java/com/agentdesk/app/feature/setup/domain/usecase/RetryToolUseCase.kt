package com.agentdesk.app.feature.setup.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import javax.inject.Inject

class RetryToolUseCase @Inject constructor(
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val planInstallUseCase: PlanInstallUseCase,
    private val startInstallUseCase: StartInstallUseCase
) {
    suspend operator fun invoke(toolId: String? = null): AppResult<Unit> {
        val selected = if (toolId != null) setOf(toolId) else emptySet()
        val planResult = planInstallUseCase(selected)
        if (planResult is AppResult.Failure) {
            return planResult
        }
        val plan = (planResult as AppResult.Success).value
        return startInstallUseCase(plan)
    }
}
