package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.repository.GitService
import javax.inject.Inject

class RestoreCheckpointUseCase @Inject constructor(
    private val gitService: GitService
) {
    suspend operator fun invoke(projectId: String, commitHash: String): AppResult<Unit> {
        return gitService.restore(projectId, commitHash)
    }
}
