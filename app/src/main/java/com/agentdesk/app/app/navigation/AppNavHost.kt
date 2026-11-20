package com.agentdesk.app.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AppNavHost(
    navController: NavHostController,
    registrars: Set<NavGraphRegistrar>,
    modifier: Modifier = Modifier,
    startDestination: Routes = Routes.Splash
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        for (registrar in registrars) {
            registrar.register(this, navController)
        }
    }
}
