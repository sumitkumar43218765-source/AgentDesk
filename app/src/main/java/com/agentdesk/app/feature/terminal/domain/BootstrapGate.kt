package com.agentdesk.app.feature.terminal.domain

import com.agentdesk.app.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

interface BootstrapGate {
    val status: StateFlow<EnvironmentStatus>
    suspend fun ensureBootstrap(): AppResult<Unit>
}
