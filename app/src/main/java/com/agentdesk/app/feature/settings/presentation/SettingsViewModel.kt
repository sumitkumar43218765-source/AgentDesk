package com.agentdesk.app.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.datastore.AppPreferences
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import com.agentdesk.app.feature.settings.data.LicenseRepository
import com.agentdesk.app.feature.settings.domain.usecase.ExportDiagnosticsUseCase
import com.agentdesk.app.feature.settings.domain.usecase.ObservePermissionsUseCase
import com.agentdesk.app.feature.settings.domain.usecase.RevokeGrantUseCase
import com.agentdesk.app.feature.settings.domain.usecase.WipeWorkspaceUseCase
import com.agentdesk.app.feature.terminal.domain.PathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val observePermissionsUseCase: ObservePermissionsUseCase,
    private val revokeGrantUseCase: RevokeGrantUseCase,
    private val wipeWorkspaceUseCase: WipeWorkspaceUseCase,
    private val exportDiagnosticsUseCase: ExportDiagnosticsUseCase,
    private val licenseRepository: LicenseRepository,
    private val pathProvider: PathProvider,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadLicenses()
        observePreferences()
        observePermissions()
        observeGrants()
    }

    private fun loadLicenses() {
        val licenses = licenseRepository.getLicenses()
        _uiState.update { it.copy(licenses = licenses) }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            appPreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            appPreferences.dynamicColor.collect { dc ->
                _uiState.update { it.copy(dynamicColor = dc) }
            }
        }
        viewModelScope.launch {
            appPreferences.terminalFontSize.collect { size ->
                _uiState.update { it.copy(terminalFontSize = size) }
            }
        }
        viewModelScope.launch {
            appPreferences.terminalKeepScreenOn.collect { keepOn ->
                _uiState.update { it.copy(terminalKeepScreenOn = keepOn) }
            }
        }
        viewModelScope.launch {
            appPreferences.wifiOnlyDownloads.collect { wifiOnly ->
                _uiState.update { it.copy(wifiOnlyDownloads = wifiOnly) }
            }
        }
        viewModelScope.launch {
            appPreferences.defaultProviderId.collect { provider ->
                _uiState.update { it.copy(defaultProviderId = provider) }
            }
        }
        viewModelScope.launch {
            appPreferences.defaultAgentMode.collect { mode ->
                _uiState.update { it.copy(defaultAgentMode = mode) }
            }
        }
        viewModelScope.launch {
            appPreferences.agentDefaultRunMode.collect { mode ->
                _uiState.update { it.copy(agentDefaultRunMode = mode) }
            }
        }
        viewModelScope.launch {
            appPreferences.agentMaxSteps.collect { steps ->
                _uiState.update { it.copy(agentMaxSteps = steps) }
            }
        }
        viewModelScope.launch {
            appPreferences.agentShellTimeout.collect { timeout ->
                _uiState.update { it.copy(agentShellTimeout = timeout) }
            }
        }
        viewModelScope.launch {
            appPreferences.agentAllowNetwork.collect { allowNet ->
                _uiState.update { it.copy(agentAllowNetwork = allowNet) }
            }
        }
        viewModelScope.launch {
            appPreferences.agentTokenBudget.collect { budget ->
                _uiState.update { it.copy(agentTokenBudget = budget) }
            }
        }
        viewModelScope.launch {
            appPreferences.gitAuthorName.collect { name ->
                _uiState.update { it.copy(gitAuthorName = name) }
            }
        }
        viewModelScope.launch {
            appPreferences.gitAuthorEmail.collect { email ->
                _uiState.update { it.copy(gitAuthorEmail = email) }
            }
        }
        viewModelScope.launch {
            appPreferences.autoCheckpoint.collect { autoCp ->
                _uiState.update { it.copy(autoCheckpoint = autoCp) }
            }
        }
        viewModelScope.launch {
            appPreferences.backupReminders.collect { reminders ->
                _uiState.update { it.copy(backupReminders = reminders) }
            }
        }
        viewModelScope.launch {
            appPreferences.aptMirror.collect { mirror ->
                _uiState.update { it.copy(aptMirror = mirror) }
            }
        }
    }

    private fun observePermissions() {
        viewModelScope.launch {
            observePermissionsUseCase().collect { perms ->
                _uiState.update { it.copy(permissions = perms) }
            }
        }
    }

    private fun observeGrants() {
        viewModelScope.launch {
            revokeGrantUseCase.observeAll().collect { grants ->
                _uiState.update { it.copy(grants = grants) }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColor(enabled) }
    }

    fun setTerminalFontSize(size: Int) {
        viewModelScope.launch { appPreferences.setTerminalFontSize(size) }
    }

    fun setTerminalKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setTerminalKeepScreenOn(enabled) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setWifiOnlyDownloads(enabled) }
    }

    fun setAgentDefaultRunMode(mode: String) {
        viewModelScope.launch { appPreferences.setAgentDefaultRunMode(mode) }
    }

    fun setAgentMaxSteps(steps: Int) {
        viewModelScope.launch { appPreferences.setAgentMaxSteps(steps) }
    }

    fun setAgentShellTimeout(seconds: Int) {
        viewModelScope.launch { appPreferences.setAgentShellTimeout(seconds) }
    }

    fun setAgentAllowNetwork(allowed: Boolean) {
        viewModelScope.launch { appPreferences.setAgentAllowNetwork(allowed) }
    }

    fun setAgentTokenBudget(budget: Int) {
        viewModelScope.launch { appPreferences.setAgentTokenBudget(budget) }
    }

    fun setGitAuthorName(name: String) {
        viewModelScope.launch { appPreferences.setGitAuthorName(name) }
    }

    fun setGitAuthorEmail(email: String) {
        viewModelScope.launch { appPreferences.setGitAuthorEmail(email) }
    }

    fun setAutoCheckpoint(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAutoCheckpoint(enabled) }
    }

    fun setBackupReminders(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setBackupReminders(enabled) }
    }

    fun setAptMirror(mirror: String) {
        viewModelScope.launch { appPreferences.setAptMirror(mirror) }
    }

    fun revokeGrant(id: String) {
        viewModelScope.launch {
            revokeGrantUseCase.revoke(id)
            _uiState.update { it.copy(statusMessage = "Grant revoked") }
        }
    }

    fun clearGrantsForProject(projectId: String) {
        viewModelScope.launch {
            revokeGrantUseCase.clearForProject(projectId)
            _uiState.update { it.copy(statusMessage = "Grants cleared for project") }
        }
    }

    fun clearAllGrants() {
        viewModelScope.launch {
            revokeGrantUseCase.clearAll()
            _uiState.update { it.copy(statusMessage = "All permission grants cleared") }
        }
    }

    fun exportDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, exportedZipPath = null) }
            when (val result = exportDiagnosticsUseCase()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportedZipPath = result.value.absolutePath,
                            statusMessage = "Diagnostics bundle saved to ${result.value.name}"
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun wipeWorkspace(confirmation: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWiping = true, errorMessage = null) }
            when (val result = wipeWorkspaceUseCase(confirmation)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isWiping = false,
                            wipeSuccess = true,
                            statusMessage = "Workspace wiped successfully"
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isWiping = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearBuildLogs() {
        viewModelScope.launch {
            runCatching {
                pathProvider.buildLogsDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            _uiState.update { it.copy(statusMessage = "Build logs cleared") }
        }
    }

    fun clearChats() {
        viewModelScope.launch {
            val convs = chatRepository.observeConversations().firstOrNull() ?: emptyList()
            for (c in convs) {
                chatRepository.deleteConversation(c.id)
            }
            _uiState.update { it.copy(statusMessage = "All chats cleared") }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
