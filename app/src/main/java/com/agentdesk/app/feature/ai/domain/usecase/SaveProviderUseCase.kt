package com.agentdesk.app.feature.ai.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import javax.inject.Inject

class SaveProviderUseCase @Inject constructor(
    private val repository: ProviderConfigRepository
) {
    suspend operator fun invoke(config: ProviderConfig, apiKey: String?): AppResult<Unit> {
        return repository.saveProvider(config, apiKey)
    }
}
