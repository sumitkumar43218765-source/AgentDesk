package com.agentdesk.app.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    @Serializable
    data object Splash : Routes

    @Serializable
    data object Home : Routes

    @Serializable
    data object ConversationList : Routes

    @Serializable
    data class Chat(val id: String) : Routes

    @Serializable
    data object ProviderList : Routes

    @Serializable
    data class ProviderEdit(val id: String? = null) : Routes

    @Serializable
    data object Terminal : Routes

    @Serializable
    data object Projects : Routes

    @Serializable
    data class ProjectDetail(val id: String) : Routes

    @Serializable
    data class FileViewer(val projectId: String, val path: String, val line: Int? = null) : Routes

    @Serializable
    data object Setup : Routes

    @Serializable
    data object ToolSetup : Routes

    @Serializable
    data object Install : Routes

    @Serializable
    data object ToolManager : Routes

    @Serializable
    data object Settings : Routes
}
