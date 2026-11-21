package com.agentdesk.app.feature.agent.data.runtime

import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.agent.data.tools.ToolRegistryImpl
import com.agentdesk.app.feature.agent.domain.AgentLoop
import com.agentdesk.app.feature.agent.domain.ContextManager
import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.SystemPromptBuilder
import com.agentdesk.app.feature.agent.domain.model.AgentConfig
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.AgentRun
import com.agentdesk.app.feature.agent.domain.model.LoopGuardResult
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.RunStatus
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.ai.domain.model.StopReason
import com.agentdesk.app.feature.ai.domain.model.TokenUsage
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.MessageStatus
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAgentLoop @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRegistry: ProviderRegistry,
    private val toolRegistry: ToolRegistryImpl,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val contextManager: ContextManager,
    private val permissionGateway: PermissionGateway,
    private val idGenerator: IdGenerator,
    private val runStateStore: RunStateStore
) : AgentLoop {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun execute(
        run: AgentRun,
        config: AgentConfig,
        onEvent: suspend (AgentEvent) -> Unit
    ): AgentRun {
        var currentRun = run.copy(status = RunStatus.RUNNING)
        runStateStore.saveRun(currentRun)

        val conversation = chatRepository.getConversation(run.conversationId)
        if (conversation == null) {
            val errorMsg = "Conversation ${run.conversationId} not found."
            onEvent(AgentEvent.Error(errorMsg, fatal = true))
            currentRun = currentRun.copy(status = RunStatus.FAILED)
            runStateStore.saveRun(currentRun)
            return currentRun
        }

        val provider = providerRegistry.getProvider(conversation.providerId)
        if (provider == null) {
            val errorMsg = "Provider ${conversation.providerId} not found or not configured."
            onEvent(AgentEvent.Error(errorMsg, fatal = true))
            currentRun = currentRun.copy(status = RunStatus.FAILED)
            runStateStore.saveRun(currentRun)
            return currentRun
        }

        val systemPrompt = systemPromptBuilder.build(run.projectId)
        val loopGuard = LoopGuard(maxConsecutiveToolErrors = config.maxConsecutiveToolErrors)

        var totalTokensUsed = run.totalTokensUsed

        for (step in run.stepCount until config.maxSteps) {
            currentRun = currentRun.copy(stepCount = step)
            runStateStore.updateStep(run.id, step)
            onEvent(AgentEvent.StepStarted(step))

            val existingMessages = chatRepository.observeMessages(run.conversationId).first()
            val preparedMessages = contextManager.prepareMessages(
                systemPrompt = systemPrompt,
                messages = existingMessages,
                maxContextTokens = config.runTokenBudget
            )

            val request = LlmRequest(
                model = conversation.model,
                system = systemPrompt,
                messages = preparedMessages,
                tools = toolRegistry.getTools()
            )

            // Stream response
            val assistantMsgId = idGenerator.newId()
            val textBuilder = StringBuilder()
            val pendingToolCalls = mutableListOf<ContentBlock.ToolUse>()
            var stopReason: StopReason? = null
            var stepTokens = 0

            try {
                provider.stream(request).collect { event ->
                    when (event) {
                        is LlmEvent.TextDelta -> {
                            textBuilder.append(event.text)
                            onEvent(AgentEvent.AssistantText(event.text))
                        }
                        is LlmEvent.ToolCallCompleted -> {
                            val toolBlock = ContentBlock.ToolUse(event.id, event.name, event.argsJson)
                            pendingToolCalls.add(toolBlock)
                        }
                        is LlmEvent.Usage -> {
                            stepTokens = event.inputTokens + event.outputTokens
                        }
                        is LlmEvent.Done -> {
                            stopReason = event.stopReason
                        }
                        is LlmEvent.Error -> {
                            onEvent(AgentEvent.Error(event.error.message, fatal = true))
                            currentRun = currentRun.copy(status = RunStatus.FAILED)
                            runStateStore.saveRun(currentRun)
                            return currentRun
                        }
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    onEvent(AgentEvent.Cancelled)
                    currentRun = currentRun.copy(status = RunStatus.CANCELLED)
                    runStateStore.saveRun(currentRun)
                    throw e
                }
                val msg = "LLM streaming failed: ${e.message}"
                onEvent(AgentEvent.Error(msg, fatal = true))
                currentRun = currentRun.copy(status = RunStatus.FAILED)
                runStateStore.saveRun(currentRun)
                return currentRun
            }

            totalTokensUsed += stepTokens

            // Persist assistant message
            val assistantBlocks = mutableListOf<ContentBlock>()
            if (textBuilder.isNotEmpty()) {
                assistantBlocks.add(ContentBlock.Text(textBuilder.toString()))
            }
            assistantBlocks.addAll(pendingToolCalls)

            val assistantMessage = ChatMessage(
                id = assistantMsgId,
                conversationId = run.conversationId,
                role = Role.ASSISTANT,
                blocks = assistantBlocks,
                status = MessageStatus.COMPLETE,
                usage = TokenUsage(outputTokens = stepTokens)
            )
            chatRepository.saveMessage(assistantMessage)

            // If stop reason is not TOOL_USE or no tools requested, finish
            if (stopReason != StopReason.TOOL_USE || pendingToolCalls.isEmpty()) {
                val finalText = textBuilder.toString()
                onEvent(AgentEvent.Completed(finalText))
                currentRun = currentRun.copy(status = RunStatus.COMPLETED, totalTokensUsed = totalTokensUsed)
                runStateStore.saveRun(currentRun)
                return currentRun
            }

            // Execute each requested tool call sequentially
            val toolResultBlocks = mutableListOf<ContentBlock.ToolResult>()

            for (call in pendingToolCalls) {
                val parsedArgs = try {
                    if (call.argsJson.isBlank()) JsonObject(emptyMap())
                    else json.parseToJsonElement(call.argsJson).jsonObject
                } catch (e: Exception) {
                    JsonObject(emptyMap())
                }

                onEvent(AgentEvent.ToolRequested(call.id, call.name, parsedArgs))

                val tool = toolRegistry.getTool(call.name)
                if (tool == null) {
                    val errMsg = "Tool '${call.name}' not found."
                    val out = ToolOutput(errMsg, isError = true)
                    onEvent(AgentEvent.ToolFinished(call.id, call.name, out))
                    toolResultBlocks.add(ContentBlock.ToolResult(call.id, errMsg, isError = true))
                    continue
                }

                val guardBefore = loopGuard.checkBeforeExecution(call.name, parsedArgs)
                if (guardBefore is LoopGuardResult.Abort) {
                    onEvent(AgentEvent.Error(guardBefore.reason, fatal = true))
                    val out = ToolOutput(guardBefore.reason, isError = true)
                    toolResultBlocks.add(ContentBlock.ToolResult(call.id, guardBefore.reason, isError = true))
                    currentRun = currentRun.copy(status = RunStatus.FAILED)
                    runStateStore.saveRun(currentRun)
                    return currentRun
                }

                // Permission Gateway check
                val decision = permissionGateway.check(
                    runId = run.id,
                    projectId = run.projectId,
                    tool = tool,
                    args = parsedArgs,
                    mode = run.mode
                )

                if (decision is PermissionDecision.Deny) {
                    val denyMsg = "Tool execution denied by user: ${decision.reason ?: "No reason given"}"
                    val out = ToolOutput(denyMsg, isError = true)
                    onEvent(AgentEvent.ToolFinished(call.id, call.name, out))
                    toolResultBlocks.add(ContentBlock.ToolResult(call.id, denyMsg, isError = true))
                    continue
                }

                // Allowed: execute tool
                onEvent(AgentEvent.ToolStarted(call.id, call.name))
                val ctx = ToolContext(
                    projectId = run.projectId,
                    conversationId = run.conversationId,
                    runId = run.id,
                    cancelJob = currentCoroutineContext().job,
                    toolCallId = call.id
                )

                val toolOutput = try {
                    tool.execute(parsedArgs, ctx)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        onEvent(AgentEvent.Cancelled)
                        currentRun = currentRun.copy(status = RunStatus.CANCELLED)
                        runStateStore.saveRun(currentRun)
                        throw e
                    }
                    ToolOutput("Error executing ${call.name}: ${e.message}", isError = true)
                }

                val trimmed = contextManager.trimToolOutput(toolOutput.text, config.toolOutputMaxChars)
                onEvent(AgentEvent.ToolFinished(call.id, call.name, toolOutput))
                toolResultBlocks.add(ContentBlock.ToolResult(call.id, trimmed, isError = toolOutput.isError))

                val guardAfter = loopGuard.recordExecution(call.name, parsedArgs, toolOutput.isError)
                if (guardAfter is LoopGuardResult.Abort) {
                    onEvent(AgentEvent.Error(guardAfter.reason, fatal = true))
                    val resultMessage = ChatMessage(
                        id = idGenerator.newId(),
                        conversationId = run.conversationId,
                        role = Role.USER,
                        blocks = toolResultBlocks,
                        status = MessageStatus.COMPLETE
                    )
                    chatRepository.saveMessage(resultMessage)

                    currentRun = currentRun.copy(status = RunStatus.FAILED)
                    runStateStore.saveRun(currentRun)
                    return currentRun
                }
            }

            // Persist tool results
            val toolResultMessage = ChatMessage(
                id = idGenerator.newId(),
                conversationId = run.conversationId,
                role = Role.USER,
                blocks = toolResultBlocks,
                status = MessageStatus.COMPLETE
            )
            chatRepository.saveMessage(toolResultMessage)
        }

        val maxStepsMsg = "Agent reached maximum step limit (${config.maxSteps}) without completing."
        onEvent(AgentEvent.Completed(maxStepsMsg))
        currentRun = currentRun.copy(status = RunStatus.COMPLETED, totalTokensUsed = totalTokensUsed)
        runStateStore.saveRun(currentRun)
        return currentRun
    }
}
