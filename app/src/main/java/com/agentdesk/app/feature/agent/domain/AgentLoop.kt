package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.agent.domain.model.AgentConfig
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.AgentRun

interface AgentLoop {
    suspend fun execute(
        run: AgentRun,
        config: AgentConfig,
        onEvent: suspend (AgentEvent) -> Unit
    ): AgentRun
}
