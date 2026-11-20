package com.agentdesk.app.feature.setup.presentation.setup

import com.agentdesk.app.feature.setup.domain.model.InstallPlan
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition

data class ToolSetupUiState(
    val tools: List<ToolDefinition> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val plan: InstallPlan? = null,
    val isWifi: Boolean = true,
    val isLoading: Boolean = false,
    val isStarting: Boolean = false,
    val error: String? = null
)
