package com.agentdesk.app.feature.setup.domain.model

data class ToolState(
    val toolId: String,
    val installedVersion: String? = null,
    val status: ToolInstallStatus = ToolInstallStatus.NOT_INSTALLED,
    val lastVerifiedAt: Long? = null,
    val sizeBytes: Long = 0L,
    val lastError: String? = null
)
