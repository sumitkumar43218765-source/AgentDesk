package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import javax.inject.Inject

class CreateProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(name: String, template: TemplateId, packageName: String? = null): AppResult<Project> {
        return repository.create(name, template, packageName)
    }
}
