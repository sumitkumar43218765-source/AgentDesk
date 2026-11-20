package com.agentdesk.app.feature.chat.presentation.chat

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.ai.domain.usecase.ListModelsUseCase
import com.agentdesk.app.feature.chat.data.local.StreamingPersister
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import com.agentdesk.app.feature.chat.domain.usecase.RecoverInterruptedUseCase
import com.agentdesk.app.feature.chat.domain.usecase.RetryLastUseCase
import com.agentdesk.app.feature.chat.domain.usecase.SendMessageUseCase
import com.agentdesk.app.feature.chat.domain.usecase.StopStreamingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val stopStreamingUseCase: StopStreamingUseCase,
    private val retryLastUseCase: RetryLastUseCase,
    private val listModelsUseCase: ListModelsUseCase,
    private val recoverInterruptedUseCase: RecoverInterruptedUseCase,
    private val streamingPersister: StreamingPersister
) : MviViewModel<ChatUiState, ChatEvent, ChatEffect>(ChatUiState()) {

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            recoverInterruptedUseCase()
        }
    }

    override fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadConversation -> loadConversation(event.id)
            is ChatEvent.InputTextChanged -> setState { copy(inputText = event.text) }
            is ChatEvent.SendMessage -> sendMessage()
            is ChatEvent.StopStreaming -> stopStreaming()
            is ChatEvent.RetryLast -> retryLast()
            is ChatEvent.ChangeModel -> changeModel(event.modelId)
            is ChatEvent.ToggleModelPicker -> setState { copy(isModelPickerOpen = event.open) }
        }
    }

    private fun loadConversation(id: String) {
        viewModelScope.launch {
            val conv = repository.getConversation(id)
            setState { copy(conversation = conv) }

            if (conv != null) {
                repository.observeMessages(id)
                    .onEach { msgs ->
                        setState { copy(messages = msgs) }
                    }
                    .launchIn(viewModelScope)

                val modelsRes = listModelsUseCase(conv.providerId)
                if (modelsRes is AppResult.Success) {
                    setState { copy(availableModels = modelsRes.value) }
                }
            }
        }
    }

    private fun sendMessage() {
        val s = currentState
        val text = s.inputText.trim()
        val conv = s.conversation ?: return
        if (text.isBlank() || s.isStreaming) return

        setState { copy(inputText = "", isStreaming = true, streamingPartialText = "", error = null) }
        streamingPersister.reset()

        streamJob = viewModelScope.launch {
            val partialBuilder = StringBuilder()
            sendMessageUseCase(conv.id, text, s.messages).collect { event ->
                when (event) {
                    is LlmEvent.TextDelta -> {
                        partialBuilder.append(event.text)
                        setState { copy(streamingPartialText = partialBuilder.toString()) }
                        val activeAssistant = s.messages.lastOrNull { it.role == Role.ASSISTANT }
                        if (activeAssistant != null) {
                            streamingPersister.persistChunkIfNeeded(activeAssistant, partialBuilder.toString())
                        }
                    }
                    is LlmEvent.Done -> {
                        setState { copy(isStreaming = false, streamingPartialText = "") }
                        emitEffect(ChatEffect.ScrollToBottom)
                    }
                    is LlmEvent.Error -> {
                        setState { copy(isStreaming = false, error = event.error.message) }
                    }
                    else -> {}
                }
            }
        }
        stopStreamingUseCase.registerJob(streamJob!!)
    }

    private fun stopStreaming() {
        stopStreamingUseCase.stop()
        streamJob?.cancel()
        streamJob = null
        setState { copy(isStreaming = false) }
    }

    private fun retryLast() {
        val s = currentState
        val conv = s.conversation ?: return
        if (s.isStreaming) return

        setState { copy(isStreaming = true, streamingPartialText = "", error = null) }
        streamJob = viewModelScope.launch {
            val partialBuilder = StringBuilder()
            retryLastUseCase(conv.id, s.messages).collect { event ->
                when (event) {
                    is LlmEvent.TextDelta -> {
                        partialBuilder.append(event.text)
                        setState { copy(streamingPartialText = partialBuilder.toString()) }
                    }
                    is LlmEvent.Done -> {
                        setState { copy(isStreaming = false, streamingPartialText = "") }
                        emitEffect(ChatEffect.ScrollToBottom)
                    }
                    is LlmEvent.Error -> {
                        setState { copy(isStreaming = false, error = event.error.message) }
                    }
                    else -> {}
                }
            }
        }
        stopStreamingUseCase.registerJob(streamJob!!)
    }

    private fun changeModel(modelId: String) {
        val conv = currentState.conversation ?: return
        viewModelScope.launch {
            val updated = conv.copy(model = modelId, updatedAt = System.currentTimeMillis())
            repository.updateConversation(updated)
            setState { copy(conversation = updated, isModelPickerOpen = false) }
        }
    }
}
