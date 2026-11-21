package com.agentdesk.app.feature.build.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BuildHistoryRoute(
    onNavigateBack: () -> Unit,
    viewModel: BuildHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BuildHistoryScreen(
        uiState = uiState,
        onSelectRun = viewModel::selectRun,
        onDeleteRun = viewModel::deleteRun,
        onNavigateBack = onNavigateBack
    )
}
