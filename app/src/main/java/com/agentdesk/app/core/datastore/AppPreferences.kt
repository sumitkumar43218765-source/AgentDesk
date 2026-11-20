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

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[PreferenceKeys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setTerminalFontSize(size: Int) {
        dataStore.edit { it[PreferenceKeys.TERMINAL_FONT_SIZE] = size }
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
}
