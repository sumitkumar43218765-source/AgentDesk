package com.agentdesk.app.feature.agent.domain.usecase

import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContinueRunUseCase @Inject constructor(
    private val agentRunner: AgentRunner
) {
    operator fun invoke(runId: String): Flow<AgentEvent> {
        return agentRunner.continueRun(runId)
    }
}
