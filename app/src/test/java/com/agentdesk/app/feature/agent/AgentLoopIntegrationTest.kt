package com.agentdesk.app.feature.agent

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.agent.data.local.AgentRunDao
import com.agentdesk.app.feature.agent.data.local.AgentRunEntity
import com.agentdesk.app.feature.agent.data.runtime.DefaultAgentLoop
import com.agentdesk.app.feature.agent.data.runtime.DefaultContextManager
import com.agentdesk.app.feature.agent.data.runtime.RunStateStore
import com.agentdesk.app.feature.agent.data.tools.AskUserTool
import com.agentdesk.app.feature.agent.data.tools.BuildApkTool
import com.agentdesk.app.feature.agent.data.tools.DeletePathTool
import com.agentdesk.app.feature.agent.data.tools.EditFileTool
import com.agentdesk.app.feature.agent.data.tools.GitTool
import com.agentdesk.app.feature.agent.data.tools.InstallApkTool
import com.agentdesk.app.feature.agent.data.tools.ListDirTool
import com.agentdesk.app.feature.agent.data.tools.ReadBuildLogTool
import com.agentdesk.app.feature.agent.data.tools.ReadFileTool
import com.agentdesk.app.feature.agent.data.tools.SearchFilesTool
import com.agentdesk.app.feature.agent.data.tools.ShellExecTool
import com.agentdesk.app.feature.agent.data.tools.ToolRegistryImpl
import com.agentdesk.app.feature.agent.data.tools.WriteFileTool
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.SystemPromptBuilder
import com.agentdesk.app.feature.agent.domain.model.AgentConfig
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.AgentRun
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.PermissionRequest
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.agent.domain.model.RunStatus
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.agent.domain.usecase.AnswerAskUserUseCase
import com.agentdesk.app.feature.ai.domain.LlmProvider
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.ModelInfo
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import com.agentdesk.app.feature.ai.domain.model.StopReason
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.Conversation
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentLoopIntegrationTest {

    private val convMap = mutableMapOf<String, Conversation>()
    private val msgMap = mutableMapOf<String, MutableList<ChatMessage>>()
    private val runMap = mutableMapOf<String, AgentRunEntity>()
    private val idGenerator = IdGenerator()

    private val fakeChatRepo = object : ChatRepository {
        override fun observeConversations(): Flow<List<Conversation>> = flowOf(convMap.values.toList())
        override suspend fun getConversation(id: String): Conversation? = convMap[id]
        override suspend fun saveConversation(conversation: Conversation) { convMap[conversation.id] = conversation }
        override suspend fun updateConversation(conversation: Conversation) { convMap[conversation.id] = conversation }
        override suspend fun deleteConversation(id: String) { convMap.remove(id) }

        override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
            flowOf(msgMap[conversationId] ?: emptyList())
        override suspend fun getMessage(id: String): ChatMessage? = msgMap.values.flatten().find { it.id == id }
        override suspend fun saveMessage(message: ChatMessage) {
            msgMap.getOrPut(message.conversationId) { mutableListOf() }.add(message)
        }
        override suspend fun updateMessage(message: ChatMessage) {
            val list = msgMap[message.conversationId] ?: return
            val idx = list.indexOfFirst { it.id == message.id }
            if (idx >= 0) list[idx] = message
        }
        override suspend fun deleteMessage(id: String) {
            msgMap.values.forEach { it.removeIf { m -> m.id == id } }
        }
        override suspend fun recoverInterruptedMessages() {}
    }

    private val fakeDao = object : AgentRunDao {
        override fun observeLatestByConversation(conversationId: String): Flow<AgentRunEntity?> =
            flowOf(runMap.values.filter { it.conversationId == conversationId }.maxByOrNull { it.startedAt })

        override suspend fun getLatestByConversation(conversationId: String): AgentRunEntity? =
            runMap.values.filter { it.conversationId == conversationId }.maxByOrNull { it.startedAt }

        override suspend fun getById(id: String): AgentRunEntity? = runMap[id]
        override suspend fun upsert(entity: AgentRunEntity) { runMap[entity.id] = entity }
        override suspend fun markInterruptedOnStartup() {}
        override suspend fun deleteById(id: String) { runMap.remove(id) }
    }

    @Before
    fun setUp() {
        convMap.clear()
        msgMap.clear()
        runMap.clear()

        val conv = Conversation(
            id = "conv-1",
            title = "Test Run",
            projectId = "proj-1",
            providerId = "provider-1",
            model = "test-model"
        )
        convMap[conv.id] = conv
    }

    @Test
    fun `agent loop executes tool call and completes with assistant response`() = runBlocking {
        val scriptedSteps = listOf(
            listOf(
                LlmEvent.ToolCallCompleted(id = "call-1", name = "dummy_tool", argsJson = "{}"),
                LlmEvent.Done(StopReason.TOOL_USE)
            ),
            listOf(
                LlmEvent.TextDelta("Everything looks good!"),
                LlmEvent.Done(StopReason.END_TURN)
            )
        )

        var invocation = 0
        val fakeLlmProvider = object : LlmProvider {
            override val type = ProviderType.ANTHROPIC
            override suspend fun listModels(): AppResult<List<ModelInfo>> = AppResult.Success(emptyList())
            override fun stream(request: LlmRequest): Flow<LlmEvent> = flow {
                val currentEvents = scriptedSteps.getOrNull(invocation) ?: listOf(
                    LlmEvent.TextDelta("Done!"),
                    LlmEvent.Done(StopReason.END_TURN)
                )
                invocation++
                for (ev in currentEvents) {
                    emit(ev)
                }
            }
            override suspend fun testConnection(): AppResult<Unit> = AppResult.Success(Unit)
        }

        val fakeProviderRegistry = object : ProviderRegistry {
            override suspend fun getProvider(configId: String): LlmProvider = fakeLlmProvider
            override suspend fun createProvider(config: ProviderConfig, apiKey: String): LlmProvider = fakeLlmProvider
        }

        val dummyTool = object : AgentTool {
            override val spec = ToolSpec("dummy_tool", "Dummy", "{}")
            override val permission = com.agentdesk.app.feature.agent.domain.model.PermissionLevel.AUTO
            override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput =
                ToolOutput("Dummy success", false)
            override fun describe(args: JsonObject): ToolSummary =
                ToolSummary("Dummy execution", com.agentdesk.app.feature.agent.domain.model.RiskLevel.LOW)
        }

        val dummyRegistry = object : ToolRegistryImpl(
            dummyTool as? ListDirTool ?: ListDirTool(fakeFileSystem()),
            dummyTool as? ReadFileTool ?: ReadFileTool(fakeFileSystem()),
            dummyTool as? SearchFilesTool ?: SearchFilesTool(fakeFileSystem(), fakeShell()),
            dummyTool as? WriteFileTool ?: WriteFileTool(fakeFileSystem()),
            dummyTool as? EditFileTool ?: EditFileTool(fakeFileSystem()),
            dummyTool as? DeletePathTool ?: DeletePathTool(fakeFileSystem()),
            dummyTool as? ShellExecTool ?: ShellExecTool(fakeShell(), fakeFileSystem(), fakeClassifier(), idGenerator),
            dummyTool as? BuildApkTool ?: BuildApkTool(fakeBuildEngine()),
            dummyTool as? ReadBuildLogTool ?: ReadBuildLogTool(fakeBuildEngine()),
            dummyTool as? InstallApkTool ?: InstallApkTool(fakeBuildEngine(), fakeInstallApk()),
            dummyTool as? GitTool ?: GitTool(fakeGitService()),
            dummyTool as? AskUserTool ?: AskUserTool(AnswerAskUserUseCase())
        ) {
            override fun getTool(name: String): AgentTool? = if (name == "dummy_tool") dummyTool else null
        }

        val fakePromptBuilder = object : SystemPromptBuilder {
            override suspend fun build(projectId: String): String = "Prompt"
        }

        val fakePermissionGateway = object : PermissionGateway {
            override suspend fun check(runId: String, projectId: String, tool: AgentTool, args: JsonObject, mode: RunMode): PermissionDecision =
                PermissionDecision.AllowOnce
            override suspend fun resolve(requestId: String, decision: PermissionDecision) {}
            override fun observePendingRequests(runId: String): Flow<List<PermissionRequest>> = flowOf(emptyList())
        }

        val defaultAgentLoop = DefaultAgentLoop(
            chatRepository = fakeChatRepo,
            providerRegistry = fakeProviderRegistry,
            toolRegistry = dummyRegistry,
            systemPromptBuilder = fakePromptBuilder,
            contextManager = DefaultContextManager(),
            permissionGateway = fakePermissionGateway,
            idGenerator = idGenerator,
            runStateStore = RunStateStore(fakeDao)
        )

        val run = AgentRun(
            id = "run-1",
            conversationId = "conv-1",
            projectId = "proj-1",
            status = RunStatus.RUNNING,
            mode = RunMode.ASK,
            stepCount = 0,
            startedAt = System.currentTimeMillis(),
            lastEventAt = System.currentTimeMillis(),
            totalTokensUsed = 0
        )

        val events = mutableListOf<AgentEvent>()
        val resultRun = defaultAgentLoop.execute(
            run = run,
            config = AgentConfig(),
            onEvent = { events.add(it) }
        )

        assertEquals(RunStatus.COMPLETED, resultRun.status)
        assertTrue(events.any { it is AgentEvent.ToolRequested })
        assertTrue(events.any { it is AgentEvent.ToolFinished })
        assertTrue(events.any { it is AgentEvent.Completed })
    }

    private fun fakeFileSystem(): com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem =
        org.mockito.Mockito.mock(com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem::class.java)

    private fun fakeShell(): com.agentdesk.app.feature.terminal.domain.ShellBackend =
        org.mockito.Mockito.mock(com.agentdesk.app.feature.terminal.domain.ShellBackend::class.java)

    private fun fakeClassifier(): com.agentdesk.app.feature.agent.domain.CommandRiskClassifier =
        org.mockito.Mockito.mock(com.agentdesk.app.feature.agent.domain.CommandRiskClassifier::class.java)

    private fun fakeBuildEngine(): com.agentdesk.app.feature.build.domain.BuildEngine =
        org.mockito.Mockito.mock(com.agentdesk.app.feature.build.domain.BuildEngine::class.java)

    private fun fakeInstallApk(): com.agentdesk.app.feature.build.domain.usecase.InstallApkUseCase =
        org.mockito.Mockito.mock(com.agentdesk.app.feature.build.domain.usecase.InstallApkUseCase::class.java)

    private fun fakeGitService(): com.agentdesk.app.feature.projects.domain.repository.GitService =
        org.mockito.Mockito.mock(com.agentdesk.app.feature.projects.domain.repository.GitService::class.java)
}
