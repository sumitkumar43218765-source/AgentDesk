package com.agentdesk.app.feature.setup.presentation.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.check.StorageUsageCalculator
import com.agentdesk.app.feature.setup.domain.model.CheckResult
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.model.ToolState
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.setup.domain.usecase.RemoveToolUseCase
import com.agentdesk.app.feature.setup.domain.usecase.RetryToolUseCase
import com.agentdesk.app.feature.setup.domain.usecase.RunHealthChecksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolManagerUiState(
    val tools: List<ToolDefinition> = emptyList(),
    val states: Map<String, ToolState> = emptyMap(),
    val healthResults: Map<String, CheckResult> = emptyMap(),
    val isRunningHealth: Boolean = false,
    val storageBreakdown: StorageUsageCalculator.StorageBreakdown? = null,
    val message: String? = null
)

@HiltViewModel
class ToolManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val runHealthChecksUseCase: RunHealthChecksUseCase,
    private val removeToolUseCase: RemoveToolUseCase,
    private val retryToolUseCase: RetryToolUseCase,
    private val storageCalculator: StorageUsageCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolManagerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
        refreshStorage()
    }

    private fun loadData() {
        viewModelScope.launch {
            catalogRepository.loadCatalog()
            combine(
                catalogRepository.getCatalog(),
                stateRepository.observeToolStates()
            ) { tools, states ->
                tools to states.associateBy { it.toolId }
            }.collect { (tools, stateMap) ->
                _uiState.update { it.copy(tools = tools, states = stateMap) }
            }
        }
    }

    fun runHealthChecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningHealth = true) }
            val results = runHealthChecksUseCase()
            _uiState.update { it.copy(healthResults = results, isRunningHealth = false) }
        }
    }

    fun refreshStorage() {
        viewModelScope.launch {
            val breakdown = storageCalculator.calculateBreakdown()
            _uiState.update { it.copy(storageBreakdown = breakdown) }
        }
    }

    fun cleanCaches() {
        viewModelScope.launch {
            val freed = storageCalculator.cleanCaches()
            refreshStorage()
            _uiState.update { it.copy(message = "Cleaned ${freed / (1024 * 1024)} MB of cache files") }
        }
    }

    fun installTool(toolId: String) {
        viewModelScope.launch {
            retryToolUseCase(toolId)
        }
    }

    fun removeTool(toolId: String) {
        viewModelScope.launch {
            val result = removeToolUseCase(toolId)
            if (result is AppResult.Success) {
                _uiState.update { it.copy(message = "Tool removed") }
            } else if (result is AppResult.Failure) {
                _uiState.update { it.copy(message = result.error.message) }
            }
        }
    }

    fun copyDiagnostics() {
        val results = _uiState.value.healthResults
        val text = buildString {
            appendLine("=== AgentDesk Diagnostics ===")
            appendLine("Timestamp: ${System.currentTimeMillis()}")
            for ((k, v) in results) {
                appendLine("$k: ${if (v is CheckResult.Pass) "PASS" else "FAIL"}")
            }
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Diagnostics", text))
        _uiState.update { it.copy(message = "Diagnostics copied to clipboard") }
    }
}
