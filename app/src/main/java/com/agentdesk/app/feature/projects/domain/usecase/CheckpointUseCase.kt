package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.GitCommit
import com.agentdesk.app.feature.projects.domain.repository.GitService
import javax.inject.Inject

class CheckpointUseCase @Inject constructor(
    private val gitService: GitService
) {
    suspend operator fun invoke(projectId: String, message: String): AppResult<GitCommit?> {
        return gitService.checkpoint(projectId, message)
    }
}
