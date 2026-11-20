package com.agentdesk.app.feature.terminal.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.AppScaffold
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.feature.terminal.presentation.components.ExtraKeysBar
import com.agentdesk.app.feature.terminal.presentation.components.SessionTabs
import com.agentdesk.app.feature.terminal.presentation.components.TerminalViewHost

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activeSession = state.sessions.getOrNull(state.selectedSessionIndex) ?: state.sessions.first()

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Terminal",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TerminalEvent.ClearCurrentSession) }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
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
            SessionTabs(
                sessionTitles = state.sessions.map { it.title },
                selectedIndex = state.selectedSessionIndex,
                onSelectSession = { viewModel.onEvent(TerminalEvent.SelectSession(it)) },
                onNewSession = { viewModel.onEvent(TerminalEvent.NewSession) },
                onCloseSession = { viewModel.onEvent(TerminalEvent.CloseSession(it)) }
            )

            TerminalViewHost(
                output = activeSession.output,
                modifier = Modifier.weight(1f)
            )

            ExtraKeysBar(
                onKeyPress = { viewModel.onEvent(TerminalEvent.ExtraKeyPressed(it)) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.commandInput,
                    onValueChange = { viewModel.onEvent(TerminalEvent.InputChanged(it)) },
                    placeholder = { Text("Enter shell command…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                IconButton(
                    onClick = { viewModel.onEvent(TerminalEvent.ExecuteCommand) },
                    enabled = !state.isRunning && state.commandInput.isNotBlank()
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Execute")
                }
            }
        }
    }
}
