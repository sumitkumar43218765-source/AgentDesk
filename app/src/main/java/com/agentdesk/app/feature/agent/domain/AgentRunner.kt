package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.RunMode
import kotlinx.coroutines.flow.Flow

interface AgentRunner {
    fun run(
        conversationId: String,
        projectId: String,
        userPrompt: String,
        mode: RunMode = RunMode.ASK
    ): Flow<AgentEvent>

    suspend fun stop(runId: String)

    fun continueRun(runId: String): Flow<AgentEvent>
}
