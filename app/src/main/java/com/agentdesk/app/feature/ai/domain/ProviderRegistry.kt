package com.agentdesk.app.feature.ai.domain

import com.agentdesk.app.feature.ai.domain.model.ProviderConfig

interface ProviderRegistry {
    suspend fun getProvider(configId: String): LlmProvider?
    suspend fun createProvider(config: ProviderConfig, apiKey: String): LlmProvider
}
