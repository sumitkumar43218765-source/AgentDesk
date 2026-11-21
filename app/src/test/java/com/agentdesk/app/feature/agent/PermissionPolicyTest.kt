package com.agentdesk.app.feature.agent

import com.agentdesk.app.feature.agent.data.local.PermissionGrantDao
import com.agentdesk.app.feature.agent.data.local.PermissionGrantEntity
import com.agentdesk.app.feature.agent.data.permission.DefaultCommandRiskClassifier
import com.agentdesk.app.feature.agent.data.permission.DefaultPermissionGateway
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PermissionPolicyTest {

    private lateinit var gateway: DefaultPermissionGateway
    private val grantsList = mutableListOf<PermissionGrantEntity>()

    private val fakeDao = object : PermissionGrantDao {
        override suspend fun insert(entity: PermissionGrantEntity) { grantsList.add(entity) }
        override suspend fun getGrantsForTool(projectId: String, toolName: String): List<PermissionGrantEntity> {
            return grantsList.filter { it.projectId == projectId && it.toolName == toolName }
        }
        override fun observeAll(): kotlinx.coroutines.flow.Flow<List<PermissionGrantEntity>> = kotlinx.coroutines.flow.flowOf(grantsList)
        override suspend fun deleteById(id: String) {
            grantsList.removeAll { it.id == id }
        }
        override suspend fun clearForProject(projectId: String) {
            grantsList.removeAll { it.projectId == projectId }
        }
        override suspend fun clearAll() {
            grantsList.clear()
        }
    }

    private fun createFakeTool(name: String, permission: PermissionLevel): AgentTool {
        return object : AgentTool {
            override val spec = ToolSpec(name = name, description = "test", inputSchemaJson = "{}")
            override val permission = permission
            override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput =
                ToolOutput("ok", false)
            override fun describe(args: JsonObject): ToolSummary =
                ToolSummary("Test $name", RiskLevel.LOW)
        }
    }

    @Before
    fun setUp() {
        grantsList.clear()
        gateway = DefaultPermissionGateway(
            grantDao = fakeDao,
            riskClassifier = DefaultCommandRiskClassifier()
        )
    }

    @Test
    fun `auto tool always allows immediately`() = runBlocking {
        val autoTool = createFakeTool("list_dir", PermissionLevel.AUTO)
        val decision = gateway.check("run-1", "proj-1", autoTool, buildJsonObject {}, RunMode.ASK)
        assertEquals(PermissionDecision.AllowOnce, decision)
    }

    @Test
    fun `auto-accept edits allows write and edit tools without prompting`() = runBlocking {
        val writeTool = createFakeTool("write_file", PermissionLevel.ASK_ONCE)
        val decision = gateway.check("run-1", "proj-1", writeTool, buildJsonObject {}, RunMode.AUTO_ACCEPT_EDITS)
        assertEquals(PermissionDecision.AllowOnce, decision)
    }

    @Test
    fun `destructive command in deny list is denied immediately`() = runBlocking {
        val shellTool = createFakeTool("shell_exec", PermissionLevel.ASK_ALWAYS)
        val denyArgs = buildJsonObject { put("command", "rm -rf /") }
        val decision = gateway.check("run-1", "proj-1", shellTool, denyArgs, RunMode.ASK)
        assertTrue(decision is PermissionDecision.Deny)
    }

    @Test
    fun `shell allowlisted read command is allowed automatically in ask mode`() = runBlocking {
        val shellTool = createFakeTool("shell_exec", PermissionLevel.ASK_ALWAYS)
        val allowArgs = buildJsonObject { put("command", "git status") }
        val decision = gateway.check("run-1", "proj-1", shellTool, allowArgs, RunMode.ASK)
        assertEquals(PermissionDecision.AllowOnce, decision)
    }
}
