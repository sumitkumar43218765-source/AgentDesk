package com.agentdesk.app.feature.chat.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.AppScaffold
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.ErrorState
import com.agentdesk.app.feature.ai.presentation.components.ModelPickerSheet
import com.agentdesk.app.feature.chat.presentation.components.Composer
import com.agentdesk.app.feature.chat.presentation.components.MessageList

@Composable
fun ChatScreen(
    conversationId: String,
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(conversationId) {
        viewModel.onEvent(ChatEvent.LoadConversation(conversationId))
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = state.conversation?.title ?: "Chat",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    state.conversation?.let { conv ->
                        Text(
                            text = conv.model,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { viewModel.onEvent(ChatEvent.ToggleModelPicker(true)) }
                                .padding(8.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.error != null) {
                ErrorState(
                    message = state.error.orEmpty(),
                    onRetry = { viewModel.onEvent(ChatEvent.RetryLast) }
                )
            }

            MessageList(
                messages = state.messages,
                onRetry = { viewModel.onEvent(ChatEvent.RetryLast) },
                modifier = Modifier.weight(1f)
            )

            Composer(
                text = state.inputText,
                onTextChange = { viewModel.onEvent(ChatEvent.InputTextChanged(it)) },
                onSend = { viewModel.onEvent(ChatEvent.SendMessage) },
                onStop = { viewModel.onEvent(ChatEvent.StopStreaming) },
                isStreaming = state.isStreaming
            )

            if (state.isModelPickerOpen) {
                ModelPickerSheet(
                    models = state.availableModels,
                    selectedModelId = state.conversation?.model.orEmpty(),
                    onModelSelected = { viewModel.onEvent(ChatEvent.ChangeModel(it)) },
                    onDismiss = { viewModel.onEvent(ChatEvent.ToggleModelPicker(false)) }
                )
            }
        }
    }
}
