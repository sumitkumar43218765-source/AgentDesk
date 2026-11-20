package com.agentdesk.app.feature.chat.presentation.list

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import com.agentdesk.app.feature.chat.domain.usecase.CreateConversationUseCase
import com.agentdesk.app.feature.chat.domain.usecase.DeleteConversationUseCase
import com.agentdesk.app.feature.chat.domain.usecase.ObserveConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val providerConfigRepository: ProviderConfigRepository
) : MviViewModel<ConversationListUiState, ConversationListEvent, ConversationListEffect>(
    ConversationListUiState(isLoading = true)
) {

    init {
        combine(
            observeConversationsUseCase(),
            providerConfigRepository.observeProviders()
        ) { convs, providers ->
            Pair(convs, providers)
        }
            .onEach { (convs, providers) ->
                setState { copy(conversations = convs, providers = providers, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ConversationListEvent) {
        when (event) {
            is ConversationListEvent.SearchQueryChanged -> setState { copy(searchQuery = event.query) }
            is ConversationListEvent.CreateNewChat -> createNewChat(event.providerId, event.model)
            is ConversationListEvent.DeleteConversation -> {
                viewModelScope.launch {
                    deleteConversationUseCase(event.id)
                }
            }
        }
    }

    private fun createNewChat(providerId: String, model: String) {
        viewModelScope.launch {
            val conv = createConversationUseCase(
                title = "New Chat",
                providerId = providerId,
                model = model
            )
            emitEffect(ConversationListEffect.OpenChat(conv.id))
        }
    }
}
