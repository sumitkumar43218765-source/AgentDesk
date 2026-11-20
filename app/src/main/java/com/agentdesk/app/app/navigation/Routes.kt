package com.agentdesk.app.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    @Serializable
    data object Home : Routes

    @Serializable
    data object Chat : Routes

    @Serializable
    data object Terminal : Routes

    @Serializable
    data object Projects : Routes

    @Serializable
    data object Setup : Routes

    @Serializable
    data object Settings : Routes
}
