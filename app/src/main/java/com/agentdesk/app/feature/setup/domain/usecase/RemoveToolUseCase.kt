package com.agentdesk.app.feature.setup.domain.usecase

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.install.DoneMarkers
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import javax.inject.Inject

class RemoveToolUseCase @Inject constructor(
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val doneMarkers: DoneMarkers
) {
    suspend operator fun invoke(toolId: String): AppResult<Unit> {
        val tool = catalogRepository.getTool(toolId)
            ?: return AppResult.Failure(AppError.Unknown("Tool not found: $toolId"))

        if (tool.required) {
            return AppResult.Failure(
                AppError.Unknown(
                    message = "Required tools cannot be removed, only repaired."
                )
            )
        }

        doneMarkers.removeMarker(tool.id, tool.version)
        stateRepository.setToolStatus(toolId, ToolInstallStatus.NOT_INSTALLED)
        return AppResult.Success(Unit)
    }
}
