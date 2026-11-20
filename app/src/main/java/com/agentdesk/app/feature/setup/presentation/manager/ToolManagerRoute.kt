package com.agentdesk.app.feature.setup.presentation.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun ToolManagerRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ToolManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ToolManagerScreen(
        uiState = uiState,
        onRunHealthChecks = viewModel::runHealthChecks,
        onCopyDiagnostics = viewModel::copyDiagnostics,
        onCleanCaches = viewModel::cleanCaches,
        onInstallTool = viewModel::installTool,
        onRemoveTool = viewModel::removeTool,
        onBackClick = { navController.popBackStack() },
        modifier = modifier
    )
}
