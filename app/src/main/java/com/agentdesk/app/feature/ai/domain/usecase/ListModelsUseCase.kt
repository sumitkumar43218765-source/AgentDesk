package com.agentdesk.app.feature.ai.domain.usecase

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.model.ModelInfo
import javax.inject.Inject

class ListModelsUseCase @Inject constructor(
    private val providerRegistry: ProviderRegistry
) {
    suspend operator fun invoke(configId: String): AppResult<List<ModelInfo>> {
        val provider = providerRegistry.getProvider(configId)
            ?: return AppResult.Failure(AppError.Storage("Provider not found"))
        return provider.listModels()
    }
}
