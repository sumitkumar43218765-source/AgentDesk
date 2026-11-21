package com.agentdesk.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val themeMode: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.THEME_MODE] ?: "system"
    }

    val dynamicColor: Flow<Boolean> = safeData.map { prefs ->
        prefs[PreferenceKeys.DYNAMIC_COLOR] ?: true
    }

    val terminalFontSize: Flow<Int> = safeData.map { prefs ->
        prefs[PreferenceKeys.TERMINAL_FONT_SIZE] ?: 14
    }

    val terminalKeepScreenOn: Flow<Boolean> = safeData.map { prefs ->
        prefs[PreferenceKeys.TERMINAL_KEEP_SCREEN_ON] ?: false
    }

    val wifiOnlyDownloads: Flow<Boolean> = safeData.map { prefs ->
        prefs[PreferenceKeys.WIFI_ONLY_DOWNLOADS] ?: false
    }

    val lastOnboardingVersion: Flow<Int> = safeData.map { prefs ->
        prefs[PreferenceKeys.LAST_ONBOARDING_VERSION] ?: 0
    }

    val defaultProviderId: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.DEFAULT_PROVIDER_ID] ?: "anthropic"
    }

    val defaultAgentMode: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.DEFAULT_AGENT_MODE] ?: "builder"
    }

    val agentDefaultRunMode: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.AGENT_DEFAULT_RUN_MODE] ?: "ASK"
    }

    val agentMaxSteps: Flow<Int> = safeData.map { prefs ->
        prefs[PreferenceKeys.AGENT_MAX_STEPS] ?: 40
    }

    val agentShellTimeout: Flow<Int> = safeData.map { prefs ->
        prefs[PreferenceKeys.AGENT_SHELL_TIMEOUT] ?: 120
    }

    val agentAllowNetwork: Flow<Boolean> = safeData.map { prefs ->
        prefs[PreferenceKeys.AGENT_ALLOW_NETWORK] ?: true
    }

    val agentTokenBudget: Flow<Int> = safeData.map { prefs ->
        prefs[PreferenceKeys.AGENT_TOKEN_BUDGET] ?: 500_000
    }

    val gitAuthorName: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.GIT_AUTHOR_NAME] ?: "AgentDesk User"
    }

    val gitAuthorEmail: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.GIT_AUTHOR_EMAIL] ?: "user@agentdesk.local"
    }

    val autoCheckpoint: Flow<Boolean> = safeData.map { prefs ->
        prefs[PreferenceKeys.AUTO_CHECKPOINT] ?: true
    }

    val backupReminders: Flow<Boolean> = safeData.map { prefs ->
        prefs[PreferenceKeys.BACKUP_REMINDERS] ?: true
    }

    val aptMirror: Flow<String> = safeData.map { prefs ->
        prefs[PreferenceKeys.APT_MIRROR] ?: "https://ports.ubuntu.com/ubuntu-ports"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[PreferenceKeys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setTerminalFontSize(size: Int) {
        dataStore.edit { it[PreferenceKeys.TERMINAL_FONT_SIZE] = size }
    }

    suspend fun setTerminalKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.TERMINAL_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setLastOnboardingVersion(version: Int) {
        dataStore.edit { it[PreferenceKeys.LAST_ONBOARDING_VERSION] = version }
    }

    suspend fun setDefaultProviderId(providerId: String) {
        dataStore.edit { it[PreferenceKeys.DEFAULT_PROVIDER_ID] = providerId }
    }

    suspend fun setDefaultAgentMode(mode: String) {
        dataStore.edit { it[PreferenceKeys.DEFAULT_AGENT_MODE] = mode }
    }

    suspend fun setAgentDefaultRunMode(mode: String) {
        dataStore.edit { it[PreferenceKeys.AGENT_DEFAULT_RUN_MODE] = mode }
    }

    suspend fun setAgentMaxSteps(steps: Int) {
        dataStore.edit { it[PreferenceKeys.AGENT_MAX_STEPS] = steps }
    }

    suspend fun setAgentShellTimeout(seconds: Int) {
        dataStore.edit { it[PreferenceKeys.AGENT_SHELL_TIMEOUT] = seconds }
    }

    suspend fun setAgentAllowNetwork(allowed: Boolean) {
        dataStore.edit { it[PreferenceKeys.AGENT_ALLOW_NETWORK] = allowed }
    }

    suspend fun setAgentTokenBudget(budget: Int) {
        dataStore.edit { it[PreferenceKeys.AGENT_TOKEN_BUDGET] = budget }
    }

    suspend fun setGitAuthorName(name: String) {
        dataStore.edit { it[PreferenceKeys.GIT_AUTHOR_NAME] = name }
    }

    suspend fun setGitAuthorEmail(email: String) {
        dataStore.edit { it[PreferenceKeys.GIT_AUTHOR_EMAIL] = email }
    }

    suspend fun setAutoCheckpoint(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.AUTO_CHECKPOINT] = enabled }
    }

    suspend fun setBackupReminders(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.BACKUP_REMINDERS] = enabled }
    }

    suspend fun setAptMirror(mirror: String) {
        dataStore.edit { it[PreferenceKeys.APT_MIRROR] = mirror }
    }
}
