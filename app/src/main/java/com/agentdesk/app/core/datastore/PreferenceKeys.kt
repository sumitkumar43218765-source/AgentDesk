package com.agentdesk.app.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val TERMINAL_FONT_SIZE = intPreferencesKey("terminal_font_size")
    val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
    val LAST_ONBOARDING_VERSION = intPreferencesKey("last_onboarding_version")
    val DEFAULT_PROVIDER_ID = stringPreferencesKey("default_provider_id")
    val DEFAULT_AGENT_MODE = stringPreferencesKey("default_agent_mode")
}
