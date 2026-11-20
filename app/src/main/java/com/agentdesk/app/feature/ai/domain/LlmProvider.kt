package com.agentdesk.app.feature.ai.domain

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.ModelInfo
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow

interface LlmProvider {
    val type: ProviderType
    suspend fun listModels(): AppResult<List<ModelInfo>>
    fun stream(request: LlmRequest): Flow<LlmEvent>
    suspend fun testConnection(): AppResult<Unit>
}
