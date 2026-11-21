package com.agentdesk.app.feature.settings.domain.model

data class DiagnosticsBundle(
    val timestamp: Long,
    val appVersion: String,
    val androidVersion: Int,
    val deviceModel: String,
    val logFiles: List<String>,
    val environmentSummary: String
)
