package com.agentdesk.app.feature.settings.presentation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.settings.presentation.about.AboutScreen
import com.agentdesk.app.feature.settings.presentation.about.LicensesScreen
import com.agentdesk.app.feature.settings.presentation.agent.AgentSettingsScreen
import com.agentdesk.app.feature.settings.presentation.agent.GrantsManagerScreen
import com.agentdesk.app.feature.settings.presentation.appearance.AppearanceScreen
import com.agentdesk.app.feature.settings.presentation.data.DataPrivacyScreen
import com.agentdesk.app.feature.settings.presentation.environment.EnvironmentSettingsScreen
import com.agentdesk.app.feature.settings.presentation.permissions.PermissionsScreen
import com.agentdesk.app.feature.settings.presentation.projects.ProjectSettingsScreen
import com.agentdesk.app.feature.settings.presentation.terminal.TerminalSettingsScreen
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {

    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.Settings> {
            SettingsRoute(navController = navController)
        }

        navGraphBuilder.composable<Routes.SettingsAgent> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            AgentSettingsScreen(
                defaultRunMode = uiState.agentDefaultRunMode,
                maxSteps = uiState.agentMaxSteps,
                shellTimeout = uiState.agentShellTimeout,
                allowNetwork = uiState.agentAllowNetwork,
                tokenBudget = uiState.agentTokenBudget,
                onRunModeChanged = viewModel::setAgentDefaultRunMode,
                onMaxStepsChanged = viewModel::setAgentMaxSteps,
                onShellTimeoutChanged = viewModel::setAgentShellTimeout,
                onAllowNetworkChanged = viewModel::setAgentAllowNetwork,
                onTokenBudgetChanged = viewModel::setAgentTokenBudget,
                onManageGrantsClick = { navController.navigate(Routes.SettingsGrants) },
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsGrants> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            GrantsManagerScreen(
                grants = uiState.grants,
                onRevokeGrant = viewModel::revokeGrant,
                onClearAll = viewModel::clearAllGrants,
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsEnvironment> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            EnvironmentSettingsScreen(
                aptMirror = uiState.aptMirror,
                wifiOnly = uiState.wifiOnlyDownloads,
                onAptMirrorChanged = viewModel::setAptMirror,
                onWifiOnlyChanged = viewModel::setWifiOnlyDownloads,
                onNavigateToToolManager = { navController.navigate(Routes.ToolManager) },
                onRepairEnvironment = { navController.navigate(Routes.Setup) },
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsProjects> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            ProjectSettingsScreen(
                gitAuthorName = uiState.gitAuthorName,
                gitAuthorEmail = uiState.gitAuthorEmail,
                autoCheckpoint = uiState.autoCheckpoint,
                backupReminders = uiState.backupReminders,
                onGitAuthorNameChanged = viewModel::setGitAuthorName,
                onGitAuthorEmailChanged = viewModel::setGitAuthorEmail,
                onAutoCheckpointChanged = viewModel::setAutoCheckpoint,
                onBackupRemindersChanged = viewModel::setBackupReminders,
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsTerminal> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            TerminalSettingsScreen(
                fontSize = uiState.terminalFontSize,
                keepScreenOn = uiState.terminalKeepScreenOn,
                onFontSizeChanged = viewModel::setTerminalFontSize,
                onKeepScreenOnChanged = viewModel::setTerminalKeepScreenOn,
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsAppearance> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            AppearanceScreen(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.dynamicColor,
                onThemeModeChanged = viewModel::setThemeMode,
                onDynamicColorChanged = viewModel::setDynamicColor,
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsPermissions> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            PermissionsScreen(
                permissions = uiState.permissions,
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsDataPrivacy> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            DataPrivacyScreen(
                isExporting = uiState.isExporting,
                isWiping = uiState.isWiping,
                statusMessage = uiState.statusMessage,
                errorMessage = uiState.errorMessage,
                exportedZipPath = uiState.exportedZipPath,
                onExportDiagnostics = viewModel::exportDiagnostics,
                onWipeWorkspace = viewModel::wipeWorkspace,
                onClearBuildLogs = viewModel::clearBuildLogs,
                onClearChats = viewModel::clearChats,
                onDismissMessages = viewModel::dismissMessages,
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsAbout> {
            AboutScreen(
                onNavigateToLicenses = { navController.navigate(Routes.SettingsLicenses) },
                onBackClick = { navController.popBackStack() }
            )
        }

        navGraphBuilder.composable<Routes.SettingsLicenses> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            LicensesScreen(
                licenses = uiState.licenses,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
