package com.agentdesk.app.feature.build.domain.usecase

import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.model.BuildRun
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBuildHistoryUseCase @Inject constructor(
    private val buildEngine: BuildEngine
) {
    operator fun invoke(projectId: String): Flow<List<BuildRun>> {
        return buildEngine.observeHistory(projectId)
    }
}
