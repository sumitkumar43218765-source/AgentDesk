package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import kotlinx.serialization.json.JsonObject

interface AgentTool {
    val spec: ToolSpec
    val permission: PermissionLevel
    suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput
    fun describe(args: JsonObject): ToolSummary
}
