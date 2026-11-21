package com.agentdesk.app.feature.agent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PermissionPolicyTest {

    private lateinit var gateway: DefaultPermissionGateway
    private val grantsList = mutableListOf<PermissionGrantEntity>()
    private lateinit var context: Context

    private val fakeDao = object : PermissionGrantDao {
        override suspend fun insert(grant: PermissionGrantEntity) { grantsList.add(grant) }
        override suspend fun getGrantsForTool(projectId: String, toolName: String): List<PermissionGrantEntity> {
            return grantsList.filter { it.projectId == projectId && it.toolName == toolName }
        }
        override fun observeGrantsForProject(projectId: String): Flow<List<PermissionGrantEntity>> {
            return flowOf(grantsList.filter { it.projectId == projectId })
        }
        override suspend fun revoke(id: String) { grantsList.removeIf { it.id == id } }
        override suspend fun revokeAllForProject(projectId: String) { grantsList.removeIf { it.projectId == projectId } }
    }

    private fun createFakeTool(name: String, permission: PermissionLevel): AgentTool {
        return object : AgentTool {
            override val spec = ToolSpec(name = name, description = "test", inputSchemaJson = "{}")
            override val permission = permission
            override suspend fun execute(args: kotlinx.serialization.json.JsonObject, ctx: ToolContext): ToolOutput =
                ToolOutput("ok", false)
            override fun describe(args: kotlinx.serialization.json.JsonObject): ToolSummary =
                ToolSummary("Test $name", RiskLevel.LOW)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = FakePermissionGrantDao()
        gateway = DefaultPermissionGateway(
            grantDao = fakeDao,
            riskClassifier = DefaultCommandRiskClassifier(),
            context = context
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
