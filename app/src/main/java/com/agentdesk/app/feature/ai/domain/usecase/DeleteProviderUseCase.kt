package com.agentdesk.app.feature.ai.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import javax.inject.Inject

class DeleteProviderUseCase @Inject constructor(
    private val repository: ProviderConfigRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        return repository.deleteProvider(id)
    }
}
