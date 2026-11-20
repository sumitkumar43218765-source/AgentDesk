package com.agentdesk.app.feature.setup.presentation.install

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.InstallProgress
import com.agentdesk.app.feature.setup.domain.model.Phase
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.setup.domain.usecase.ObserveInstallProgressUseCase
import com.agentdesk.app.feature.setup.domain.usecase.RetryToolUseCase
import com.agentdesk.app.feature.terminal.domain.PathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class InstallUiState(
    val progress: InstallProgress = InstallProgress(emptyList(), 0, Phase.INSTALLING),
    val terminalOutput: String = "",
    val canSkip: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class InstallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeInstallProgressUseCase: ObserveInstallProgressUseCase,
    private val retryToolUseCase: RetryToolUseCase,
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val pathProvider: PathProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome = _navigateToHome.asSharedFlow()

    private var hasNavigated = false

    init {
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            observeInstallProgressUseCase().collect { progress ->
                val failedTool = progress.tools.firstOrNull { it.status == ToolInstallStatus.FAILED }
                val canSkip = if (failedTool != null) {
                    val def = catalogRepository.getTool(failedTool.toolId)
                    def?.required == false
                } else false

                _uiState.update { it.copy(progress = progress, canSkip = canSkip) }

                checkRequiredComplete(progress)
            }
        }
    }

    private suspend fun checkRequiredComplete(progress: InstallProgress) {
        if (hasNavigated) return

        val catalogResult = catalogRepository.loadCatalog()
        if (catalogResult is AppResult.Success) {
            val tools: List<ToolDefinition> = catalogResult.value
            val requiredIds: Set<String> = tools.filter { it.required }.map { it.id }.toSet()
            val toolStatusMap = progress.tools.associate { it.toolId to it.status }
            val allRequiredInstalled = requiredIds.all { id ->
                toolStatusMap[id] == ToolInstallStatus.INSTALLED
            }

            if (allRequiredInstalled && requiredIds.isNotEmpty() && progress.phase != Phase.FAILED) {
                hasNavigated = true
                _navigateToHome.emit(Unit)
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            retryToolUseCase()
        }
    }

    fun skip() {
        val failed = _uiState.value.progress.tools.firstOrNull { it.status == ToolInstallStatus.FAILED }
        if (failed != null) {
            viewModelScope.launch {
                stateRepository.setToolStatus(failed.toolId, ToolInstallStatus.SKIPPED)
            }
        }
    }

    fun copyLog() {
        val statusFile = File(pathProvider.setupStateDir, "status.jsonl")
        val content = if (statusFile.exists()) statusFile.readText() else "No install log found."
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Install Log", content))
        _uiState.update { it.copy(message = "Log copied to clipboard") }
    }
}
