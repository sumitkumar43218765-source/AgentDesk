package com.agentdesk.app.feature.setup.presentation.install

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.agentdesk.app.app.navigation.Routes

@Composable
fun InstallRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: InstallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect {
            navController.navigate(Routes.Home) {
                popUpTo(Routes.Install) { inclusive = true }
            }
        }
    }

    InstallScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onSkip = viewModel::skip,
        onOpenTerminal = {
            navController.navigate(Routes.Terminal)
        },
        onCopyLog = viewModel::copyLog,
        modifier = modifier
    )
}
