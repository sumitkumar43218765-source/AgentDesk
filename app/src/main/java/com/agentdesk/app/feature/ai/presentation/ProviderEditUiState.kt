package com.agentdesk.app.feature.ai.presentation

import com.agentdesk.app.core.mvi.UiState
import com.agentdesk.app.feature.ai.domain.model.ModelInfo
import com.agentdesk.app.feature.ai.domain.model.ProviderType

data class ProviderEditUiState(
    val id: String = "",
    val type: ProviderType = ProviderType.ANTHROPIC,
    val displayName: String = "",
    val baseUrl: String = "https://api.anthropic.com",
    val apiKey: String = "",
    val hasExistingKey: Boolean = false,
    val defaultModel: String = "claude-3-5-sonnet-20241022",
    val availableModels: List<ModelInfo> = emptyList(),
    val isTesting: Boolean = false,
    val testStatus: String? = null,
    val isSuccess: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) : UiState
