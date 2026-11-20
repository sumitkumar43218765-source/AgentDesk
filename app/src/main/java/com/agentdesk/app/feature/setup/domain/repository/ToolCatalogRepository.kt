package com.agentdesk.app.feature.setup.domain.repository

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import kotlinx.coroutines.flow.Flow

interface ToolCatalogRepository {
    fun getCatalog(): Flow<List<ToolDefinition>>
    suspend fun loadCatalog(): AppResult<List<ToolDefinition>>
    suspend fun getTool(id: String): ToolDefinition?
}
