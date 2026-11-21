package com.agentdesk.app.feature.projects.domain.repository

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import com.agentdesk.app.feature.projects.domain.model.ExportTarget
import com.agentdesk.app.feature.projects.domain.model.ImportSource
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    suspend fun create(name: String, template: TemplateId, packageName: String? = null): AppResult<Project>
    suspend fun import(source: ImportSource): AppResult<Project>
    suspend fun export(projectId: String, target: ExportTarget, options: ExportOptions): AppResult<String>
    suspend fun rename(projectId: String, newName: String): AppResult<Project>
    suspend fun duplicate(projectId: String): AppResult<Project>
    suspend fun delete(projectId: String): AppResult<Unit>
    suspend fun get(projectId: String): Project?
}
