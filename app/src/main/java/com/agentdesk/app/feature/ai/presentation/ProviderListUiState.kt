package com.agentdesk.app.feature.ai.presentation

import com.agentdesk.app.core.mvi.UiState
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig

data class ProviderListUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState
