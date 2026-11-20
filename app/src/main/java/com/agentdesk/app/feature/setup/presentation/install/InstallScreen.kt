package com.agentdesk.app.feature.setup.presentation.install

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.MonoText
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.setup.domain.model.Phase
import com.agentdesk.app.feature.setup.presentation.install.components.InstallActionsBar
import com.agentdesk.app.feature.setup.presentation.install.components.ProgressHeader

@Composable
fun InstallScreen(
    uiState: InstallUiState,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onOpenTerminal: () -> Unit,
    onCopyLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Installing Environment",
                subtitle = "Setting up Linux packages & build tools"
            )
        },
        bottomBar = {
            if (uiState.progress.phase == Phase.FAILED) {
                InstallActionsBar(
                    canSkip = uiState.canSkip,
                    onRetry = onRetry,
                    onSkip = onSkip,
                    onOpenTerminal = onOpenTerminal,
                    onCopyLog = onCopyLog
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProgressHeader(progress = uiState.progress)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(Spacing.medium)
                    .verticalScroll(rememberScrollState())
            ) {
                val lastTool = uiState.progress.tools.find { it.toolId == uiState.progress.activeToolId }
                val output = buildString {
                    appendLine("=== Installer Console ===")
                    for (tool in uiState.progress.tools) {
                        appendLine("[${tool.status.name}] ${tool.name}")
                        if (tool.message.isNotEmpty()) {
                            appendLine("  > ${tool.message}")
                        }
                    }
                    if (lastTool != null) {
                        appendLine()
                        appendLine("Currently working on: ${lastTool.name}...")
                    }
                }

                MonoText(
                    text = output,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
