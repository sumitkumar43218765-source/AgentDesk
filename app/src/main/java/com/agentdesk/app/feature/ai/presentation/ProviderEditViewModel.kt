package com.agentdesk.app.feature.ai.presentation

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.core.mvi.UiEffect
import com.agentdesk.app.core.mvi.UiEvent
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import com.agentdesk.app.feature.ai.domain.usecase.SaveProviderUseCase
import com.agentdesk.app.feature.ai.domain.usecase.TestProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProviderEditEvent : UiEvent {
    data class LoadProvider(val id: String?) : ProviderEditEvent
    data class UpdateType(val type: ProviderType) : ProviderEditEvent
    data class UpdateDisplayName(val name: String) : ProviderEditEvent
    data class UpdateBaseUrl(val url: String) : ProviderEditEvent
    data class UpdateApiKey(val key: String) : ProviderEditEvent
    data class UpdateDefaultModel(val model: String) : ProviderEditEvent
    data object TestConnection : ProviderEditEvent
    data object Save : ProviderEditEvent
}

sealed interface ProviderEditEffect : UiEffect {
    data class ShowToast(val message: String) : ProviderEditEffect
    data object NavigateBack : ProviderEditEffect
}

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    private val repository: ProviderConfigRepository,
    private val saveProviderUseCase: SaveProviderUseCase,
    private val testProviderUseCase: TestProviderUseCase,
    private val idGenerator: IdGenerator
) : MviViewModel<ProviderEditUiState, ProviderEditEvent, ProviderEditEffect>(ProviderEditUiState()) {

    override fun onEvent(event: ProviderEditEvent) {
        when (event) {
            is ProviderEditEvent.LoadProvider -> load(event.id)
            is ProviderEditEvent.UpdateType -> {
                val defaultUrl = if (event.type == ProviderType.ANTHROPIC) "https://api.anthropic.com" else "https://api.openai.com/v1"
                val defaultModel = if (event.type == ProviderType.ANTHROPIC) "claude-3-5-sonnet-20241022" else "gpt-4o"
                setState { copy(type = event.type, baseUrl = defaultUrl, defaultModel = defaultModel) }
            }
            is ProviderEditEvent.UpdateDisplayName -> setState { copy(displayName = event.name) }
            is ProviderEditEvent.UpdateBaseUrl -> setState { copy(baseUrl = event.url) }
            is ProviderEditEvent.UpdateApiKey -> setState { copy(apiKey = event.key) }
            is ProviderEditEvent.UpdateDefaultModel -> setState { copy(defaultModel = event.model) }
            is ProviderEditEvent.TestConnection -> testConnection()
            is ProviderEditEvent.Save -> save()
        }
    }

    private fun load(id: String?) {
        if (id == null) {
            setState { copy(id = idGenerator.newId(), displayName = "Anthropic") }
            return
        }
        viewModelScope.launch {
            val provider = repository.getProvider(id)
            if (provider != null) {
                val hasKey = repository.getApiKey(id)?.isNotBlank() == true
                setState {
                    copy(
                        id = provider.id,
                        type = provider.type,
                        displayName = provider.displayName,
                        baseUrl = provider.baseUrl,
                        defaultModel = provider.defaultModel,
                        hasExistingKey = hasKey
                    )
                }
            }
        }
    }

    private fun testConnection() {
        val s = currentState
        viewModelScope.launch {
            setState { copy(isTesting = true, testStatus = null, error = null) }
            val keyToUse = if (s.apiKey.isNotBlank()) s.apiKey else repository.getApiKey(s.id).orEmpty()
            val config = ProviderConfig(
                id = s.id,
                type = s.type,
                displayName = s.displayName,
                baseUrl = s.baseUrl,
                defaultModel = s.defaultModel
            )
            when (val res = testProviderUseCase(config, keyToUse)) {
                is AppResult.Success -> {
                    setState { copy(isTesting = false, testStatus = "Connection successful!") }
                }
                is AppResult.Failure -> {
                    setState { copy(isTesting = false, error = res.error.message) }
                }
            }
        }
    }

    private fun save() {
        val s = currentState
        if (s.displayName.isBlank()) {
            setState { copy(error = "Display name is required") }
            return
        }
        viewModelScope.launch {
            setState { copy(isSaving = true, error = null) }
            val config = ProviderConfig(
                id = s.id,
                type = s.type,
                displayName = s.displayName,
                baseUrl = s.baseUrl,
                defaultModel = s.defaultModel
            )
            val apiKey = s.apiKey.ifBlank { null }
            when (val res = saveProviderUseCase(config, apiKey)) {
                is AppResult.Success -> {
                    emitEffect(ProviderEditEffect.ShowToast("Provider saved"))
                    emitEffect(ProviderEditEffect.NavigateBack)
                }
                is AppResult.Failure -> {
                    setState { copy(isSaving = false, error = res.error.message) }
                }
            }
        }
    }
}
