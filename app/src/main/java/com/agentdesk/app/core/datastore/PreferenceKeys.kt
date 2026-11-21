package com.agentdesk.app.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val TERMINAL_FONT_SIZE = intPreferencesKey("terminal_font_size")
    val TERMINAL_KEEP_SCREEN_ON = booleanPreferencesKey("terminal_keep_screen_on")
    val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
    val LAST_ONBOARDING_VERSION = intPreferencesKey("last_onboarding_version")
    val DEFAULT_PROVIDER_ID = stringPreferencesKey("default_provider_id")
    val DEFAULT_AGENT_MODE = stringPreferencesKey("default_agent_mode")
    val AGENT_DEFAULT_RUN_MODE = stringPreferencesKey("agent_default_run_mode")
    val AGENT_MAX_STEPS = intPreferencesKey("agent_max_steps")
    val AGENT_SHELL_TIMEOUT = intPreferencesKey("agent_shell_timeout")
    val AGENT_ALLOW_NETWORK = booleanPreferencesKey("agent_allow_network")
    val AGENT_TOKEN_BUDGET = intPreferencesKey("agent_token_budget")
    val GIT_AUTHOR_NAME = stringPreferencesKey("git_author_name")
    val GIT_AUTHOR_EMAIL = stringPreferencesKey("git_author_email")
    val AUTO_CHECKPOINT = booleanPreferencesKey("auto_checkpoint")
    val BACKUP_REMINDERS = booleanPreferencesKey("backup_reminders")
    val APT_MIRROR = stringPreferencesKey("apt_mirror")
}
