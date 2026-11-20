package com.agentdesk.app.feature.setup.domain.model

data class InstallPlan(
    val toolsToInstall: List<ToolDefinition>,
    val totalDownloadMb: Long,
    val requiredDiskSpaceMb: Long,
    val availableDiskSpaceMb: Long,
    val hasEnoughSpace: Boolean,
    val spaceShortfallMb: Long = 0L
)
