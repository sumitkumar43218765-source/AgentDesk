package com.agentdesk.app.feature.build.presentation.build

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BuildRoute(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (projectId: String) -> Unit,
    onNavigateToFile: (projectId: String, relativePath: String, line: Int?) -> Unit,
    viewModel: BuildViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BuildScreen(
        uiState = uiState,
        onVariantChange = viewModel::setVariant,
        onStartBuild = viewModel::startBuild,
        onCancelBuild = viewModel::cancelBuild,
        onInstallApk = viewModel::installApk,
        onExportApk = viewModel::exportApk,
        onShareApk = viewModel::shareApk,
        onFixWithAi = viewModel::fixWithAi,
        onDiagnosticClick = { file, line ->
            if (file != null) {
                onNavigateToFile(uiState.projectId, file, line)
            }
        },
        onNavigateToHistory = {
            onNavigateToHistory(uiState.projectId)
        },
        onNavigateBack = onNavigateBack,
        onClearStatus = viewModel::clearStatusMessage
    )
}
