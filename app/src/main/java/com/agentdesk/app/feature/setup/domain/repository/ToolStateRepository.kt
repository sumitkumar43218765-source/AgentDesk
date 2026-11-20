package com.agentdesk.app.feature.setup.domain.repository

import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolState
import kotlinx.coroutines.flow.Flow

interface ToolStateRepository {
    fun observeToolStates(): Flow<List<ToolState>>
    suspend fun getToolState(toolId: String): ToolState?
    suspend fun updateToolState(state: ToolState)
    suspend fun setToolStatus(toolId: String, status: ToolInstallStatus, error: String? = null)
    suspend fun getAllStates(): List<ToolState>
}
