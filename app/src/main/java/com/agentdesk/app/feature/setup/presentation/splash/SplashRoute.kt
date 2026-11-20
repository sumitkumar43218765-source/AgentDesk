package com.agentdesk.app.feature.setup.presentation.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.agentdesk.app.app.navigation.Routes

@Composable
fun SplashRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val targetRoute by viewModel.targetRoute.collectAsState()

    LaunchedEffect(targetRoute) {
        val destination = targetRoute
        if (destination != null) {
            navController.navigate(destination) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
        }
    }

    SplashScreen(modifier = modifier)
}
