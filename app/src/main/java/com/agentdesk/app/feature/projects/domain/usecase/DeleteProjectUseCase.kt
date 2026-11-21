package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): AppResult<Unit> {
        return repository.delete(projectId)
    }
}
