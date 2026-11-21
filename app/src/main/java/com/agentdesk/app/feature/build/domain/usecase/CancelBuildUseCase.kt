package com.agentdesk.app.feature.build.domain.usecase

import com.agentdesk.app.feature.build.domain.BuildEngine
import javax.inject.Inject

class CancelBuildUseCase @Inject constructor(
    private val buildEngine: BuildEngine
) {
    suspend operator fun invoke(buildId: String) {
        buildEngine.cancel(buildId)
    }
}
