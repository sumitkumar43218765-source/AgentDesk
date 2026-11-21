package com.agentdesk.app.feature.agent.data.permission

import android.content.Context
import com.agentdesk.app.feature.agent.data.local.PermissionGrantDao
import com.agentdesk.app.feature.agent.data.local.PermissionGrantEntity
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.CommandRiskClassifier
import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.PermissionRequest
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.RunMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPermissionGateway @Inject constructor(
    private val grantDao: PermissionGrantDao,
    private val riskClassifier: CommandRiskClassifier,
    @ApplicationContext private val context: Context
) : PermissionGateway {

    private val pendingRequestsMap = ConcurrentHashMap<String, MutableStateFlow<List<PermissionRequest>>>()
    private val deferredDecisions = ConcurrentHashMap<String, CompletableDeferred<PermissionDecision>>()
    private val sessionGrants = ConcurrentHashMap<String, MutableSet<String>>() // runId -> granted tool/scope

    override suspend fun check(
        runId: String,
        projectId: String,
        tool: AgentTool,
        args: JsonObject,
        mode: RunMode
    ): PermissionDecision {
        val toolName = tool.spec.name

        // 1. AUTO level
        if (tool.permission == PermissionLevel.AUTO) {
            return PermissionDecision.AllowOnce
        }

        // 2. Auto-accept edits mode
        if (mode == RunMode.AUTO_ACCEPT_EDITS && (toolName == "write_file" || toolName == "edit_file")) {
            return PermissionDecision.AllowOnce
        }

        // 3. For shell_exec tool, check allowlist and deny list
        if (toolName == "shell_exec") {
            val cmd = args["command"]?.jsonPrimitive?.content ?: ""
            val risk = riskClassifier.classify(cmd)
            if (risk == RiskLevel.DENIED) {
                return PermissionDecision.Deny("Command refused: matched system safety deny list.")
            }
            if (risk == RiskLevel.LOW && ShellAllowlist.isAllowed(cmd)) {
                return PermissionDecision.AllowOnce
            }
        }

        // 4. Check session grants
        val sessionSet = sessionGrants.getOrPut(runId) { ConcurrentHashMap.newKeySet() }
        if (sessionSet.contains(toolName)) {
            // HIGH risk commands always prompt even if session granted
            val cmd = args["command"]?.jsonPrimitive?.content ?: ""
            if (toolName != "shell_exec" || riskClassifier.classify(cmd) != RiskLevel.HIGH) {
                return PermissionDecision.AllowOnce
            }
        }

        // 5. Check project persisted grants in database
        val projectGrants = grantDao.getGrantsForTool(projectId, toolName)
        if (projectGrants.isNotEmpty()) {
            if (toolName == "shell_exec") {
                val cmd = args["command"]?.jsonPrimitive?.content ?: ""
                val risk = riskClassifier.classify(cmd)
                // HIGH risk commands always prompt even with project grants
                if (risk != RiskLevel.HIGH) {
                    val hasMatchingPrefix = projectGrants.any { cmd.startsWith(it.scopeKey) }
                    if (hasMatchingPrefix) {
                        return PermissionDecision.AllowOnce
                    }
                }
            } else {
                return PermissionDecision.AllowOnce
            }
        }

        // 6. Suspend and request approval from user
        val summary = tool.describe(args)
        val requestId = UUID.randomUUID().toString()
        val request = PermissionRequest(
            id = requestId,
            runId = runId,
            toolName = toolName,
            summary = summary,
            level = tool.permission,
            args = args
        )

        val deferred = CompletableDeferred<PermissionDecision>()
        deferredDecisions[requestId] = deferred

        val stateFlow = pendingRequestsMap.getOrPut(runId) { MutableStateFlow(emptyList()) }
        stateFlow.update { it + request }

        val decision = deferred.await()

        stateFlow.update { list -> list.filterNot { it.id == requestId } }
        deferredDecisions.remove(requestId)

        // Handle persistence of decision
        when (decision) {
            is PermissionDecision.AllowSession -> {
                sessionSet.add(toolName)
            }
            is PermissionDecision.AllowProject -> {
                val scopeKey = if (toolName == "shell_exec") {
                    args["command"]?.jsonPrimitive?.content?.split(" ")?.firstOrNull() ?: toolName
                } else {
                    toolName
                }
                grantDao.insert(
                    PermissionGrantEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        toolName = toolName,
                        scopeKey = scopeKey,
                        grantedAt = System.currentTimeMillis()
                    )
                )
            }
            else -> Unit
        }

        return decision
    }

    override suspend fun resolve(requestId: String, decision: PermissionDecision) {
        val deferred = deferredDecisions[requestId]
        deferred?.complete(decision)
    }

    override fun observePendingRequests(runId: String): Flow<List<PermissionRequest>> {
        return pendingRequestsMap.getOrPut(runId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }
}
