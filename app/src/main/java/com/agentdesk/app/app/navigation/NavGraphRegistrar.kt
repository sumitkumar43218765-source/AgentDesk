package com.agentdesk.app.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

interface NavGraphRegistrar {
    fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController)
}
