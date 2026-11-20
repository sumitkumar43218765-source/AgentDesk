package com.agentdesk.app.feature.setup.presentation.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.feature.setup.presentation.setup.components.SizeSummaryBar
import com.agentdesk.app.feature.setup.presentation.setup.components.ToolGroupHeader
import com.agentdesk.app.feature.setup.presentation.setup.components.ToolRow

@Composable
fun ToolSetupScreen(
    uiState: ToolSetupUiState,
    onToggleTool: (String) -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val err = uiState.error
        if (err != null) {
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tool Setup",
                subtitle = "Select tools for on-device development"
            )
        },
        bottomBar = {
            SizeSummaryBar(
                plan = uiState.plan,
                isWifi = uiState.isWifi,
                isStarting = uiState.isStarting,
                onDoneClick = onDoneClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val requiredTools = uiState.tools.filter { it.required }
            val optionalTools = uiState.tools.filter { !it.required }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (requiredTools.isNotEmpty()) {
                    item {
                        ToolGroupHeader(title = "Required Core Tools")
                    }
                    items(requiredTools, key = { it.id }) { tool ->
                        ToolRow(
                            tool = tool,
                            isSelected = uiState.selectedIds.contains(tool.id),
                            onToggle = { onToggleTool(tool.id) }
                        )
                    }
                }

                if (optionalTools.isNotEmpty()) {
                    item {
                        ToolGroupHeader(title = "Optional Development Tools")
                    }
                    items(optionalTools, key = { it.id }) { tool ->
                        ToolRow(
                            tool = tool,
                            isSelected = uiState.selectedIds.contains(tool.id),
                            onToggle = { onToggleTool(tool.id) }
                        )
                    }
                }
            }
        }
    }
}
