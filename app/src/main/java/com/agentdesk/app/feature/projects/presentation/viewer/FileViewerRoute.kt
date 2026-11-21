package com.agentdesk.app.feature.projects.presentation.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun FileViewerRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: FileViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    FileViewerScreen(
        uiState = uiState,
        onCopyPath = viewModel::copyPath,
        onAskAgent = viewModel::askAgentAboutFile,
        onBackClick = { navController.popBackStack() },
        onClearMessage = viewModel::clearMessage,
        modifier = modifier
    )
}
