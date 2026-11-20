package com.agentdesk.app.feature.ai.domain.usecase

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import javax.inject.Inject

class TestProviderUseCase @Inject constructor(
    private val providerRegistry: ProviderRegistry
) {
    suspend operator fun invoke(config: ProviderConfig, apiKey: String): AppResult<Unit> {
        return try {
            val provider = providerRegistry.createProvider(config, apiKey)
            provider.testConnection()
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message ?: "Connection test failed", e))
        }
    }
}
