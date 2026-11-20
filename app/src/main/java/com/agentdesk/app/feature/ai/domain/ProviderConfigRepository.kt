package com.agentdesk.app.feature.ai.domain

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import kotlinx.coroutines.flow.Flow

interface ProviderConfigRepository {
    fun observeProviders(): Flow<List<ProviderConfig>>
    suspend fun getProvider(id: String): ProviderConfig?
    suspend fun getApiKey(providerId: String): String?
    suspend fun saveProvider(config: ProviderConfig, apiKey: String?): AppResult<Unit>
    suspend fun deleteProvider(id: String): AppResult<Unit>
}
