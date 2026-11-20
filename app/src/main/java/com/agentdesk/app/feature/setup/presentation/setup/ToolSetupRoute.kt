package com.agentdesk.app.feature.setup.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.agentdesk.app.app.navigation.Routes

@Composable
fun ToolSetupRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ToolSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToInstall.collect {
            navController.navigate(Routes.Install) {
                popUpTo(Routes.ToolSetup) { inclusive = true }
            }
        }
    }

    ToolSetupScreen(
        uiState = uiState,
        onToggleTool = viewModel::toggleTool,
        onDoneClick = viewModel::onDoneClick,
        modifier = modifier
    )
}
