package com.agentdesk.app.feature.settings.presentation

import com.agentdesk.app.feature.agent.domain.model.PermissionGrant
import com.agentdesk.app.feature.settings.domain.model.LicenseInfo
import com.agentdesk.app.feature.settings.domain.model.PermissionItem

data class SettingsUiState(
    val themeMode: String = "system",
    val dynamicColor: Boolean = true,
    val terminalFontSize: Int = 14,
    val terminalKeepScreenOn: Boolean = false,
    val wifiOnlyDownloads: Boolean = false,
    val defaultProviderId: String = "anthropic",
    val defaultAgentMode: String = "builder",
    val agentDefaultRunMode: String = "ASK",
    val agentMaxSteps: Int = 40,
    val agentShellTimeout: Int = 120,
    val agentAllowNetwork: Boolean = true,
    val agentTokenBudget: Int = 500_000,
    val gitAuthorName: String = "AgentDesk User",
    val gitAuthorEmail: String = "user@agentdesk.local",
    val autoCheckpoint: Boolean = true,
    val backupReminders: Boolean = true,
    val aptMirror: String = "https://ports.ubuntu.com/ubuntu-ports",
    val permissions: List<PermissionItem> = emptyList(),
    val grants: List<PermissionGrant> = emptyList(),
    val licenses: List<LicenseInfo> = emptyList(),
    val isWiping: Boolean = false,
    val wipeSuccess: Boolean = false,
    val isExporting: Boolean = false,
    val exportedZipPath: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)
