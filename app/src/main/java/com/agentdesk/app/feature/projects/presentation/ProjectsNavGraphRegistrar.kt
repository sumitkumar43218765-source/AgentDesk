package com.agentdesk.app.feature.projects.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.projects.presentation.detail.ProjectDetailRoute
import com.agentdesk.app.feature.projects.presentation.list.ProjectListRoute
import com.agentdesk.app.feature.projects.presentation.viewer.FileViewerRoute
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectsNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {

    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.Projects> {
            ProjectListRoute(navController = navController)
        }
        navGraphBuilder.composable<Routes.ProjectDetail> {
            ProjectDetailRoute(navController = navController)
        }
        navGraphBuilder.composable<Routes.FileViewer> {
            FileViewerRoute(navController = navController)
        }
    }
}
