package com.agentdesk.app.feature.ai.presentation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {
    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.ProviderList> {
            val viewModel: ProviderListViewModel = hiltViewModel()
            ProviderListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddProvider = { navController.navigate(Routes.ProviderEdit(null)) },
                onEditProvider = { id -> navController.navigate(Routes.ProviderEdit(id)) }
            )
        }

        navGraphBuilder.composable<Routes.ProviderEdit> { backStackEntry ->
            val editRoute = backStackEntry.toRoute<Routes.ProviderEdit>()
            val viewModel: ProviderEditViewModel = hiltViewModel()
            ProviderEditScreen(
                viewModel = viewModel,
                providerId = editRoute.id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
