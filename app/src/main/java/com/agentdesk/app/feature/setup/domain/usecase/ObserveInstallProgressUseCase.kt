package com.agentdesk.app.feature.setup.domain.usecase

import com.agentdesk.app.feature.setup.data.install.SetupInstallService
import com.agentdesk.app.feature.setup.domain.model.InstallProgress
import com.agentdesk.app.feature.setup.domain.model.Phase
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolProgress
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveInstallProgressUseCase @Inject constructor(
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository
) {
    operator fun invoke(): Flow<InstallProgress> {
        return combine(
            catalogRepository.getCatalog(),
            stateRepository.observeToolStates(),
            SetupInstallService.currentPhase,
            SetupInstallService.overallPct
        ) { tools, states, phase, overallPct ->
            val stateMap = states.associateBy { it.toolId }
            var activeTool: String? = null
            var errorMsg: String? = null

            val toolProgressList = tools.map { tool ->
                val state = stateMap[tool.id]
                val status = state?.status ?: ToolInstallStatus.NOT_INSTALLED
                if (status == ToolInstallStatus.INSTALLING) {
                    activeTool = tool.id
                }
                if (status == ToolInstallStatus.FAILED && errorMsg == null) {
                    errorMsg = state?.lastError
                }
                ToolProgress(
                    toolId = tool.id,
                    name = tool.name,
                    status = status,
                    pct = if (status == ToolInstallStatus.INSTALLED) 100 else 0,
                    message = state?.lastError ?: ""
                )
            }

            InstallProgress(
                tools = toolProgressList,
                overallPct = overallPct,
                phase = phase,
                activeToolId = activeTool,
                error = errorMsg
            )
        }
    }
}
