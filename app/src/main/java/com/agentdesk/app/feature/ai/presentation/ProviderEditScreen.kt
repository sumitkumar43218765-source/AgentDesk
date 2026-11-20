package com.agentdesk.app.feature.ai.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agentdesk.app.R
import com.agentdesk.app.core.ui.components.AppScaffold
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.ErrorState
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import com.agentdesk.app.feature.ai.presentation.components.MaskedKeyField

@Composable
fun ProviderEditScreen(
    viewModel: ProviderEditViewModel,
    providerId: String?,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(providerId) {
        viewModel.onEvent(ProviderEditEvent.LoadProvider(providerId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProviderEditEffect.NavigateBack -> onNavigateBack()
                else -> {}
            }
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = if (providerId != null) stringResource(R.string.edit_provider) else stringResource(R.string.add_provider),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(ProviderEditEvent.Save) },
                        enabled = !state.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.provider_type), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                FilterChip(
                    selected = state.type == ProviderType.ANTHROPIC,
                    onClick = { viewModel.onEvent(ProviderEditEvent.UpdateType(ProviderType.ANTHROPIC)) },
                    label = { Text("Anthropic") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = state.type == ProviderType.OPENAI_COMPATIBLE,
                    onClick = { viewModel.onEvent(ProviderEditEvent.UpdateType(ProviderType.OPENAI_COMPATIBLE)) },
                    label = { Text("OpenAI-Compatible") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.displayName,
                onValueChange = { viewModel.onEvent(ProviderEditEvent.UpdateDisplayName(it)) },
                label = { Text(stringResource(R.string.provider_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = { viewModel.onEvent(ProviderEditEvent.UpdateBaseUrl(it)) },
                label = { Text(stringResource(R.string.provider_base_url)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            MaskedKeyField(
                value = state.apiKey,
                onValueChange = { viewModel.onEvent(ProviderEditEvent.UpdateApiKey(it)) },
                label = stringResource(R.string.provider_api_key),
                isExistingKey = state.hasExistingKey
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.defaultModel,
                onValueChange = { viewModel.onEvent(ProviderEditEvent.UpdateDefaultModel(it)) },
                label = { Text(stringResource(R.string.provider_default_model)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(ProviderEditEvent.TestConnection) },
                    enabled = !state.isTesting
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.test_connection))
                }

                if (state.testStatus != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusChip(text = state.testStatus.orEmpty(), level = StatusLevel.SUCCESS)
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                StatusChip(text = state.error.orEmpty(), level = StatusLevel.ERROR)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onEvent(ProviderEditEvent.Save) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
