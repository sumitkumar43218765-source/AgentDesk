package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.ai.domain.ToolRegistry

interface AgentToolRegistry : ToolRegistry {
    fun getTool(name: String): AgentTool?
    fun getAllAgentTools(): List<AgentTool>
}
