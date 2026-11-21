package com.agentdesk.app.feature.projects.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import com.agentdesk.app.feature.projects.domain.model.ExportTarget
import com.agentdesk.app.feature.projects.domain.model.GitCommit
import com.agentdesk.app.feature.projects.domain.model.GitStatus
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.repository.GitService
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import com.agentdesk.app.feature.projects.domain.usecase.CheckpointUseCase
import com.agentdesk.app.feature.projects.domain.usecase.ExportProjectUseCase
import com.agentdesk.app.feature.projects.domain.usecase.RestoreCheckpointUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectDetailUiState(
    val project: Project? = null,
    val gitStatus: GitStatus? = null,
    val commits: List<GitCommit> = emptyList(),
    val diffStat: String = "",
    val selectedTab: Int = 0,
    val isHistoryOpen: Boolean = false,
    val isCheckpointDialogOpen: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val gitService: GitService,
    private val checkpointUseCase: CheckpointUseCase,
    private val restoreCheckpointUseCase: RestoreCheckpointUseCase,
    private val exportProjectUseCase: ExportProjectUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String = savedStateHandle["id"] ?: ""

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        if (projectId.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val proj = projectRepository.get(projectId)
            if (proj != null) {
                _uiState.update { it.copy(project = proj) }
                refreshGit(proj.slug)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshGit(slug: String) {
        val statusRes = gitService.status(slug)
        val commitsRes = gitService.log(slug, 30)
        val diffRes = gitService.diffStat(slug)

        _uiState.update {
            it.copy(
                gitStatus = if (statusRes is AppResult.Success) statusRes.value else null,
                commits = if (commitsRes is AppResult.Success) commitsRes.value else emptyList(),
                diffStat = if (diffRes is AppResult.Success) diffRes.value else ""
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun openCheckpointDialog() {
        _uiState.update { it.copy(isCheckpointDialogOpen = true) }
    }

    fun closeCheckpointDialog() {
        _uiState.update { it.copy(isCheckpointDialogOpen = false) }
    }

    fun createCheckpoint(message: String) {
        val proj = _uiState.value.project ?: return
        viewModelScope.launch {
            val res = checkpointUseCase(proj.slug, message)
            if (res is AppResult.Success) {
                _uiState.update { it.copy(isCheckpointDialogOpen = false, message = "Checkpoint created") }
                refreshGit(proj.slug)
            } else if (res is AppResult.Failure) {
                _uiState.update { it.copy(message = res.error.message) }
            }
        }
    }

    fun restoreCheckpoint(commitHash: String) {
        val proj = _uiState.value.project ?: return
        viewModelScope.launch {
            val res = restoreCheckpointUseCase(proj.slug, commitHash)
            if (res is AppResult.Success) {
                _uiState.update { it.copy(isHistoryOpen = false, message = "Restored checkpoint $commitHash") }
                refreshGit(proj.slug)
            } else if (res is AppResult.Failure) {
                _uiState.update { it.copy(message = res.error.message) }
            }
        }
    }

    fun exportToDownloads() {
        val proj = _uiState.value.project ?: return
        viewModelScope.launch {
            val fileName = "${proj.slug}.zip"
            val res = exportProjectUseCase(
                projectId = proj.id,
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

    fun openHistory() {
        _uiState.update { it.copy(isHistoryOpen = true) }
    }

    fun closeHistory() {
        _uiState.update { it.copy(isHistoryOpen = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
