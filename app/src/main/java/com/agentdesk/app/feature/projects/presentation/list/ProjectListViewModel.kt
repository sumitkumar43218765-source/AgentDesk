package com.agentdesk.app.feature.projects.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.data.local.ProjectIndexReconciler
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import com.agentdesk.app.feature.projects.domain.model.ExportTarget
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import com.agentdesk.app.feature.projects.domain.usecase.DeleteProjectUseCase
import com.agentdesk.app.feature.projects.domain.usecase.EvaluateBackupReminderUseCase
import com.agentdesk.app.feature.projects.domain.usecase.ExportProjectUseCase
import com.agentdesk.app.feature.projects.domain.usecase.ObserveProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val filteredProjects: List<Project> = emptyList(),
    val searchQuery: String = "",
    val projectsNeedingBackup: List<Project> = emptyList(),
    val projectToDelete: Project? = null,
    val isCreateSheetOpen: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val observeProjectsUseCase: ObserveProjectsUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val exportProjectUseCase: ExportProjectUseCase,
    private val projectRepository: ProjectRepository,
    private val evaluateBackupReminderUseCase: EvaluateBackupReminderUseCase,
    private val reconciler: ProjectIndexReconciler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectListUiState())
    val uiState = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            reconciler.reconcile()
        }
        observeProjects()
    }

    private fun observeProjects() {
        viewModelScope.launch {
            combine(
                observeProjectsUseCase(),
                searchQueryFlow
            ) { projects, query ->
                val filtered = if (query.isBlank()) {
                    projects
                } else {
                    projects.filter {
                        it.name.contains(query, ignoreCase = true) ||
                            it.packageName.contains(query, ignoreCase = true)
                    }
                }
                val needingBackup = projects.filter { evaluateBackupReminderUseCase(it) }
                Triple(projects, filtered, needingBackup)
            }.collect { (projects, filtered, needingBackup) ->
                _uiState.update {
                    it.copy(
                        projects = projects,
                        filteredProjects = filtered,
                        projectsNeedingBackup = needingBackup
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun promptDelete(project: Project) {
        _uiState.update { it.copy(projectToDelete = project) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(projectToDelete = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.projectToDelete ?: return
        viewModelScope.launch {
            deleteProjectUseCase(target.id)
            _uiState.update { it.copy(projectToDelete = null, message = "Project deleted") }
        }
    }

    fun duplicate(projectId: String) {
        viewModelScope.launch {
            val res = projectRepository.duplicate(projectId)
            if (res is AppResult.Success) {
                _uiState.update { it.copy(message = "Duplicated to ${res.value.name}") }
            } else if (res is AppResult.Failure) {
                _uiState.update { it.copy(message = res.error.message) }
            }
        }
    }

    fun exportToDownloads(project: Project) {
        viewModelScope.launch {
            val fileName = "${project.slug}.zip"
            val res = exportProjectUseCase(
                projectId = project.id,
                target = ExportTarget.Downloads(fileName),
                options = ExportOptions(excludeBuild = true, excludeGit = true)
            )
            if (res is AppResult.Success) {
                _uiState.update { it.copy(message = "Exported to Downloads/AgentDesk/$fileName") }
            } else if (res is AppResult.Failure) {
                _uiState.update { it.copy(message = res.error.message) }
            }
        }
    }

    fun openCreateSheet() {
        _uiState.update { it.copy(isCreateSheetOpen = true) }
    }

    fun closeCreateSheet() {
        _uiState.update { it.copy(isCreateSheetOpen = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
