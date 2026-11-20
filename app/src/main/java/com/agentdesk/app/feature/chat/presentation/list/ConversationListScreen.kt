package com.agentdesk.app.feature.chat.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agentdesk.app.R
import com.agentdesk.app.core.ui.components.AppScaffold
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.EmptyState
import com.agentdesk.app.core.ui.components.LoadingState

@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel,
    onOpenChat: (String) -> Unit,
    onOpenProviders: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConversationListEffect.OpenChat -> onOpenChat(effect.conversationId)
                is ConversationListEffect.OpenProvidersSetup -> onOpenProviders()
            }
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.conversations_title),
                actions = {
                    IconButton(onClick = onOpenProviders) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Providers")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.providers.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val firstProvider = state.providers.first()
                        viewModel.onEvent(
                            ConversationListEvent.CreateNewChat(
                                providerId = firstProvider.id,
                                model = firstProvider.defaultModel
                            )
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.new_chat))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.providers.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = stringResource(R.string.no_provider_title),
                    subtitle = stringResource(R.string.no_provider_subtitle)
                )
            } else if (state.conversations.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = stringResource(R.string.empty_conversations_title),
                    subtitle = stringResource(R.string.empty_conversations_subtitle)
                )
            } else {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(ConversationListEvent.SearchQueryChanged(it)) },
                    placeholder = { Text("Search chats…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                val filtered = if (state.searchQuery.isBlank()) {
                    state.conversations
                } else {
                    state.conversations.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { conv ->
                        ListItem(
                            headlineContent = { Text(conv.title) },
                            supportingContent = { Text(conv.model) },
                            leadingContent = {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.onEvent(ConversationListEvent.DeleteConversation(conv.id)) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChat(conv.id) }
                        )
                    }
                }
            }
        }
    }
}
