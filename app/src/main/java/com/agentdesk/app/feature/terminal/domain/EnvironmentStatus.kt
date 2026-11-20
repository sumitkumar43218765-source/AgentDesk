package com.agentdesk.app.feature.terminal.domain

import com.agentdesk.app.core.common.AppError

sealed interface EnvironmentStatus {
    data object NotInstalled : EnvironmentStatus
    data class Extracting(val progress: Float) : EnvironmentStatus
    data object BootstrapReady : EnvironmentStatus
    data object Ready : EnvironmentStatus
    data class Failed(val error: AppError) : EnvironmentStatus
}
