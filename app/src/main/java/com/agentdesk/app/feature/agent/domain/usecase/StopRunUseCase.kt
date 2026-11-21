package com.agentdesk.app.feature.agent.domain.usecase

import com.agentdesk.app.feature.agent.domain.AgentRunner
import javax.inject.Inject

class StopRunUseCase @Inject constructor(
    private val agentRunner: AgentRunner
) {
    suspend operator fun invoke(runId: String) {
        agentRunner.stop(runId)
    }
}
