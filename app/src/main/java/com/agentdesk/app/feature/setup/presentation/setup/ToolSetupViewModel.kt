package com.agentdesk.app.feature.setup.presentation.setup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.usecase.LoadToolCatalogUseCase
import com.agentdesk.app.feature.setup.domain.usecase.PlanInstallUseCase
import com.agentdesk.app.feature.setup.domain.usecase.StartInstallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loadCatalogUseCase: LoadToolCatalogUseCase,
    private val planInstallUseCase: PlanInstallUseCase,
    private val startInstallUseCase: StartInstallUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolSetupUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _navigateToInstall = MutableSharedFlow<Unit>()
    val navigateToInstall = _navigateToInstall.asSharedFlow()

    init {
        checkNetwork()
        loadTools()
    }

    private fun checkNetwork() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val isWifi = cm?.activeNetwork?.let { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } ?: true
        _uiState.update { it.copy(isWifi = isWifi) }
    }

    private fun loadTools() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = loadCatalogUseCase()
            if (result is AppResult.Success) {
                val tools: List<ToolDefinition> = result.value
                val initialSelected: Set<String> = tools.filter { it.required }.map { it.id }.toSet()
                _uiState.update { it.copy(tools = tools, selectedIds = initialSelected, isLoading = false) }
                updatePlan(initialSelected, tools)
            } else if (result is AppResult.Failure) {
                _uiState.update { it.copy(isLoading = false, error = result.error.message) }
            }
        }
    }

    fun toggleTool(toolId: String) {
        val currentState = _uiState.value
        val tool = currentState.tools.find { it.id == toolId } ?: return
        if (tool.required) return

        val selected = currentState.selectedIds.toMutableSet()
        if (selected.contains(toolId)) {
            val toRemove = mutableSetOf(toolId)
            var changed = true
            while (changed) {
                val beforeSize = toRemove.size
                for (t in currentState.tools) {
                    if (t.dependsOn.any { toRemove.contains(it) }) {
                        toRemove.add(t.id)
                    }
                }
                changed = toRemove.size > beforeSize
            }
            selected.removeAll(toRemove)
        } else {
            val toAdd = mutableSetOf(toolId)
            var changed = true
            while (changed) {
                val beforeSize = toAdd.size
                for (id in toAdd.toList()) {
                    val current = currentState.tools.find { it.id == id }
                    if (current != null) {
                        toAdd.addAll(current.dependsOn)
                    }
                }
                changed = toAdd.size > beforeSize
            }
            selected.addAll(toAdd)
        }

        _uiState.update { it.copy(selectedIds = selected) }
        viewModelScope.launch {
            updatePlan(selected, currentState.tools)
        }
    }

    private suspend fun updatePlan(selected: Set<String>, tools: List<ToolDefinition>) {
        val planResult = planInstallUseCase(selected)
        if (planResult is AppResult.Success) {
            _uiState.update { it.copy(plan = planResult.value) }
        }
    }

    fun onDoneClick() {
        val state = _uiState.value
        if (state.isStarting) return
        val plan = state.plan ?: return
        if (!plan.hasEnoughSpace) return

        _uiState.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            val result = startInstallUseCase(plan)
            if (result is AppResult.Success) {
                _navigateToInstall.emit(Unit)
            } else if (result is AppResult.Failure) {
                _uiState.update { it.copy(isStarting = false, error = result.error.message) }
            }
        }
    }
}
