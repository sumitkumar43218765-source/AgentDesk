package com.agentdesk.app.feature.ai.presentation

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.core.mvi.UiEffect
import com.agentdesk.app.core.mvi.UiEvent
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProviderListEvent : UiEvent {
    data class DeleteProvider(val id: String) : ProviderListEvent
}

sealed interface ProviderListEffect : UiEffect {
    data class ShowMessage(val message: String) : ProviderListEffect
}

@HiltViewModel
class ProviderListViewModel @Inject constructor(
    private val repository: ProviderConfigRepository
) : MviViewModel<ProviderListUiState, ProviderListEvent, ProviderListEffect>(ProviderListUiState(isLoading = true)) {

    init {
        repository.observeProviders()
            .onEach { list ->
                setState { copy(providers = list, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProviderListEvent) {
        when (event) {
            is ProviderListEvent.DeleteProvider -> {
                viewModelScope.launch {
                    repository.deleteProvider(event.id)
                    emitEffect(ProviderListEffect.ShowMessage("Provider removed"))
                }
            }
        }
    }
}
