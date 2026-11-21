package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.PermissionRequest
import com.agentdesk.app.feature.agent.domain.model.RunMode
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

interface PermissionGateway {
    suspend fun check(
        runId: String,
        projectId: String,
        tool: AgentTool,
        args: JsonObject,
        mode: RunMode
    ): PermissionDecision

    suspend fun resolve(requestId: String, decision: PermissionDecision)

    fun observePendingRequests(runId: String): Flow<List<PermissionRequest>>
}
