package com.agentdesk.app.feature.build.domain.usecase

import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.model.BuildEvent
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartBuildUseCase @Inject constructor(
    private val buildEngine: BuildEngine
) {
    operator fun invoke(projectId: String, variant: BuildVariant = BuildVariant.DEBUG): Flow<BuildEvent> {
        return buildEngine.build(projectId, variant)
    }
}
