package com.agentdesk.app.feature.setup.presentation.manager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.feature.setup.presentation.manager.components.HealthCheckCard
import com.agentdesk.app.feature.setup.presentation.manager.components.StorageUsageCard
import com.agentdesk.app.feature.setup.presentation.manager.components.ToolStatusCard

@Composable
fun ToolManagerScreen(
    uiState: ToolManagerUiState,
    onRunHealthChecks: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onCleanCaches: () -> Unit,
    onInstallTool: (String) -> Unit,
    onRemoveTool: (String) -> Unit,
    onBackClick: () -> Unit,
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
                title = "Tool Manager & Environment",
                subtitle = "Manage packages, rootfs & diagnostics",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                HealthCheckCard(
                    results = uiState.healthResults,
                    isRunning = uiState.isRunningHealth,
                    onRunChecks = onRunHealthChecks,
                    onCopyDiagnostics = onCopyDiagnostics
                )
            }

            item {
                StorageUsageCard(
                    breakdown = uiState.storageBreakdown,
                    onCleanCaches = onCleanCaches
                )
            }

            items(uiState.tools, key = { it.id }) { tool ->
                ToolStatusCard(
                    tool = tool,
                    state = uiState.states[tool.id],
                    onInstall = { onInstallTool(tool.id) },
                    onRemove = { onRemoveTool(tool.id) },
                    onRepair = { onInstallTool(tool.id) }
                )
            }
        }
    }
}
