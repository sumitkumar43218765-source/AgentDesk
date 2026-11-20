package com.agentdesk.app.feature.setup.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import javax.inject.Inject

class LoadToolCatalogUseCase @Inject constructor(
    private val catalogRepository: ToolCatalogRepository
) {
    suspend operator fun invoke(): AppResult<List<ToolDefinition>> {
        return catalogRepository.loadCatalog()
    }
}
