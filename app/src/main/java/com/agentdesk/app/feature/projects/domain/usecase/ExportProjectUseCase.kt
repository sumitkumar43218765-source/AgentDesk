package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import com.agentdesk.app.feature.projects.domain.model.ExportTarget
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import javax.inject.Inject

class ExportProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(
        projectId: String,
        target: ExportTarget,
        options: ExportOptions = ExportOptions()
    ): AppResult<String> {
        return repository.export(projectId, target, options)
    }
}
