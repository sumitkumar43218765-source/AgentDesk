package com.agentdesk.app.feature.build.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.build.presentation.build.BuildRoute
import com.agentdesk.app.feature.build.presentation.history.BuildHistoryRoute
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {

    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.Build> {
            BuildRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { projectId ->
                    navController.navigate(Routes.BuildHistory(projectId))
                },
                onNavigateToFile = { projectId, relativePath, line ->
                    navController.navigate(Routes.FileViewer(projectId, relativePath, line))
                }
            )
        }

        navGraphBuilder.composable<Routes.BuildHistory> {
            BuildHistoryRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
