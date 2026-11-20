package com.agentdesk.app.feature.terminal.presentation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {
    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.Terminal> {
            val viewModel: TerminalViewModel = hiltViewModel()
            TerminalScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
