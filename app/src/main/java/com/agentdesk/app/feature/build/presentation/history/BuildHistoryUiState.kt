package com.agentdesk.app.feature.build.presentation.history

import com.agentdesk.app.feature.build.domain.model.BuildRun

data class BuildHistoryUiState(
    val projectId: String = "",
    val projectName: String = "",
    val runs: List<BuildRun> = emptyList(),
    val isLoading: Boolean = false,
    val selectedRun: BuildRun? = null,
    val selectedRunLog: String = "",
    val errorMessage: String? = null
)
