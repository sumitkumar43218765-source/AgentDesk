package com.agentdesk.app.feature.terminal.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.components.AppScaffold
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.feature.terminal.data.TermuxSessionHandle
import com.agentdesk.app.feature.terminal.presentation.components.ExtraKeysBar
import com.agentdesk.app.feature.terminal.presentation.components.SessionTabs
import com.agentdesk.app.feature.terminal.presentation.components.TerminalViewHost

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activeSession = state.sessions.getOrNull(state.selectedSessionIndex)
    val termSession = (activeSession?.handle as? TermuxSessionHandle)?.terminalSession

    AppScaffold(
        topBar = {
            AppTopBar(
                title = activeSession?.title ?: "Terminal",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                terminalSession = termSession,
                terminalViewClient = viewModel.viewClient,
                modifier = Modifier.weight(1f)
            )

            ExtraKeysBar(
                onKeyPress = { viewModel.onEvent(TerminalEvent.ExtraKeyPressed(it)) },
                isCtrlActive = state.isCtrlActive,
                isAltActive = state.isAltActive
            )
        }
    }
}
