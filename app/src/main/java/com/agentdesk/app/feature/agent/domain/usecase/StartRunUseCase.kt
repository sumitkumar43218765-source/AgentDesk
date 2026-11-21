package com.agentdesk.app.feature.agent.domain.usecase

import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.RunMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartRunUseCase @Inject constructor(
    private val agentRunner: AgentRunner
) {
    operator fun invoke(
        conversationId: String,
        projectId: String,
        userPrompt: String,
        mode: RunMode = RunMode.ASK
    ): Flow<AgentEvent> {
        return agentRunner.run(conversationId, projectId, userPrompt, mode)
    }
}
