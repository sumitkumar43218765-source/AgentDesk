package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.ImportSource
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import javax.inject.Inject

class ImportProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(source: ImportSource): AppResult<Project> {
        return repository.import(source)
    }
}
