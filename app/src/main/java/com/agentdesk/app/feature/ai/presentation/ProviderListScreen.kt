package com.agentdesk.app.feature.ai.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel

@Composable
fun ProviderListScreen(
    viewModel: ProviderListViewModel,
    onNavigateBack: () -> Unit,
    onAddProvider: () -> Unit,
    onEditProvider: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.providers_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProvider) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_provider))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.providers.isEmpty() -> EmptyState(
                    title = stringResource(R.string.no_provider_title),
                    subtitle = stringResource(R.string.no_provider_subtitle)
                )
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.providers, key = { it.id }) { provider ->
                            ListItem(
                                headlineContent = { Text(provider.displayName) },
                                supportingContent = { Text(provider.defaultModel) },
                                leadingContent = {
                                    StatusChip(
                                        text = provider.type.name,
                                        level = StatusLevel.INFO
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.onEvent(ProviderListEvent.DeleteProvider(provider.id)) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditProvider(provider.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
