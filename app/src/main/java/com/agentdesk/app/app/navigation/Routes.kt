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
    data class Build(val projectId: String) : Routes

    @Serializable
    data class BuildHistory(val projectId: String) : Routes

    @Serializable
    data object Settings : Routes

    @Serializable
    data object SettingsAgent : Routes

    @Serializable
    data object SettingsGrants : Routes

    @Serializable
    data object SettingsEnvironment : Routes

    @Serializable
    data object SettingsProjects : Routes

    @Serializable
    data object SettingsTerminal : Routes

    @Serializable
    data object SettingsAppearance : Routes

    @Serializable
    data object SettingsPermissions : Routes

    @Serializable
    data object SettingsDataPrivacy : Routes

    @Serializable
    data object SettingsAbout : Routes

    @Serializable
    data object SettingsLicenses : Routes
}
