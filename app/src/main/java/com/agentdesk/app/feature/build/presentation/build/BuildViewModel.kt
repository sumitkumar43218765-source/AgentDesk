package com.agentdesk.app.feature.build.presentation.build

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.FixWithAiLauncher
import com.agentdesk.app.feature.build.domain.model.BuildEvent
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.InstallOutcome
import com.agentdesk.app.feature.build.domain.model.StepStatus
import com.agentdesk.app.feature.build.domain.usecase.CancelBuildUseCase
import com.agentdesk.app.feature.build.domain.usecase.ExportApkUseCase
import com.agentdesk.app.feature.build.domain.usecase.InstallApkUseCase
import com.agentdesk.app.feature.build.domain.usecase.StartBuildUseCase
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val startBuildUseCase: StartBuildUseCase,
    private val cancelBuildUseCase: CancelBuildUseCase,
    private val installApkUseCase: InstallApkUseCase,
    private val exportApkUseCase: ExportApkUseCase,
    private val fixWithAiLauncher: FixWithAiLauncher,
    private val projectDao: ProjectDao,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String = savedStateHandle["projectId"] ?: ""

    private val _uiState = MutableStateFlow(BuildUiState(projectId = projectId))
    val uiState: StateFlow<BuildUiState> = _uiState.asStateFlow()

    init {
        loadProject()
    }

    private fun loadProject() {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            val project = projectDao.getById(projectId)
            if (project != null) {
                _uiState.update { it.copy(projectName = project.name) }
            }
        }
    }

    fun setVariant(variant: BuildVariant) {
        _uiState.update { it.copy(selectedVariant = variant) }
    }

    fun startBuild() {
        val variant = _uiState.value.selectedVariant
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBuilding = true,
                    logOutput = "",
                    diagnostics = emptyList(),
                    stepStatuses = BuildStepId.entries.associateWith { StepStatus.PENDING },
                    stepDurations = emptyMap(),
                    statusMessage = "Starting build...",
                    errorMessage = null
                )
            }

            startBuildUseCase(projectId, variant).collect { event ->
                when (event) {
                    is BuildEvent.Started -> {
                        _uiState.update { it.copy(currentBuildId = event.buildId) }
                    }
                    is BuildEvent.StepStarted -> {
                        _uiState.update { state ->
                            val updatedStatuses = state.stepStatuses.toMutableMap().apply {
                                put(event.step, StepStatus.RUNNING)
                            }
                            state.copy(
                                currentStep = event.step,
                                stepStatuses = updatedStatuses
                            )
                        }
                    }
                    is BuildEvent.StepLog -> {
                        _uiState.update { state ->
                            val combined = (state.logOutput + event.chunk).takeLast(100_000)
                            state.copy(logOutput = combined)
                        }
                    }
                    is BuildEvent.StepFinished -> {
                        _uiState.update { state ->
                            val updatedStatuses = state.stepStatuses.toMutableMap().apply {
                                put(event.step, event.status)
                            }
                            val updatedDurations = state.stepDurations.toMutableMap().apply {
                                put(event.step, event.durationMs)
                            }
                            state.copy(
                                stepStatuses = updatedStatuses,
                                stepDurations = updatedDurations
                            )
                        }
                    }
                    is BuildEvent.Finished -> {
                        _uiState.update { state ->
                            state.copy(
                                isBuilding = false,
                                latestRun = event.run,
                                diagnostics = event.run.summary.diagnostics,
                                currentStep = null,
                                statusMessage = if (event.run.status.name == "SUCCESS") "Build Successful" else "Build Failed"
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelBuild() {
        val buildId = _uiState.value.currentBuildId ?: return
        viewModelScope.launch {
            cancelBuildUseCase(buildId)
            _uiState.update { it.copy(isBuilding = false, statusMessage = "Build Cancelled") }
        }
    }

    fun installApk() {
        val run = _uiState.value.latestRun ?: return
        val apkPath = run.apkHostPath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Preparing installation...") }
            when (val result = installApkUseCase(apkPath)) {
                is AppResult.Success -> {
                    when (val outcome = result.value) {
                        is InstallOutcome.Success -> {
                            _uiState.update { it.copy(statusMessage = "Installation started") }
                        }
                        is InstallOutcome.NeedsPermission -> {
                            context.startActivity(outcome.settingsIntent)
                        }
                        is InstallOutcome.MismatchSignature -> {
                            _uiState.update { it.copy(errorMessage = "Signature mismatch with installed package '${outcome.packageName}'. Please uninstall the old version first.") }
                        }
                        is InstallOutcome.Failure -> {
                            _uiState.update { it.copy(errorMessage = "Install failed: ${outcome.message}") }
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun exportApk() {
        val run = _uiState.value.latestRun ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Exporting APK...") }
            when (val result = exportApkUseCase(run)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "APK exported to Downloads/AgentDesk") }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = "Export failed: ${result.error.message}") }
                }
            }
        }
    }

    fun shareApk() {
        val run = _uiState.value.latestRun ?: return
        viewModelScope.launch {
            when (val result = exportApkUseCase(run)) {
                is AppResult.Success -> {
                    val uri = result.value
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.android.package-archive"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share APK").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = "Share failed: ${result.error.message}") }
                }
            }
        }
    }

    fun fixWithAi() {
        val run = _uiState.value.latestRun ?: return
        fixWithAiLauncher.launch(projectId, run.id)
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
