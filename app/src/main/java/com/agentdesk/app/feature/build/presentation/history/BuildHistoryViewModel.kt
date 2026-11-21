package com.agentdesk.app.feature.build.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.data.local.RoomBuildHistoryRepository
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.usecase.ObserveBuildHistoryUseCase
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuildHistoryViewModel @Inject constructor(
    private val observeBuildHistoryUseCase: ObserveBuildHistoryUseCase,
    private val historyRepository: RoomBuildHistoryRepository,
    private val projectDao: ProjectDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String = savedStateHandle["projectId"] ?: ""

    private val _uiState = MutableStateFlow(BuildHistoryUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<BuildHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            val project = projectDao.getById(projectId)
            if (project != null) {
                _uiState.update { it.copy(projectName = project.name) }
            }
            observeBuildHistoryUseCase(projectId).collect { list ->
                _uiState.update { it.copy(runs = list, isLoading = false) }
            }
        }
    }

    fun selectRun(run: BuildRun?) {
        if (run == null) {
            _uiState.update { it.copy(selectedRun = null, selectedRunLog = "") }
            return
        }
        viewModelScope.launch {
            val logSlice = historyRepository.readLog(run.id, 0, 1000)
            val logText = if (logSlice is AppResult.Success) {
                logSlice.value.lines.joinToString("\n")
            } else {
                "Unable to load log file."
            }
            _uiState.update { it.copy(selectedRun = run, selectedRunLog = logText) }
        }
    }

    fun deleteRun(buildId: String) {
        viewModelScope.launch {
            historyRepository.deleteRun(buildId)
            if (_uiState.value.selectedRun?.id == buildId) {
                _uiState.update { it.copy(selectedRun = null, selectedRunLog = "") }
            }
        }
    }
}
