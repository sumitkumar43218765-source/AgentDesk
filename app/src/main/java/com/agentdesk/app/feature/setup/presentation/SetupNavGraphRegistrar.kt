package com.agentdesk.app.feature.setup.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.setup.presentation.install.InstallRoute
import com.agentdesk.app.feature.setup.presentation.manager.ToolManagerRoute
import com.agentdesk.app.feature.setup.presentation.setup.ToolSetupRoute
import com.agentdesk.app.feature.setup.presentation.splash.SplashRoute
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetupNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {

    override fun register(builder: NavGraphBuilder, navController: NavController) {
        builder.composable<Routes.Splash> {
            SplashRoute(navController = navController)
        }
        builder.composable<Routes.ToolSetup> {
            ToolSetupRoute(navController = navController)
        }
        builder.composable<Routes.Install> {
            InstallRoute(navController = navController)
        }
        builder.composable<Routes.ToolManager> {
            ToolManagerRoute(navController = navController)
        }
    }
}
