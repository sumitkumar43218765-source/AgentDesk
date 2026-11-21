package com.agentdesk.app.feature.agent.data.runtime

import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.agent.domain.AgentLoop
import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.model.AgentConfig
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.AgentRun
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.agent.domain.model.RunStatus
import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.MessageStatus
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAgentRunner @Inject constructor(
    private val agentLoop: AgentLoop,
    private val runStateStore: RunStateStore,
    private val chatRepository: ChatRepository,
    private val idGenerator: IdGenerator
) : AgentRunner {

    private val activeJobs = ConcurrentHashMap<String, Job>()

    override fun run(
        conversationId: String,
        projectId: String,
        userPrompt: String,
        mode: RunMode
    ): Flow<AgentEvent> = flow {
        val runId = idGenerator.newId()
        val now = System.currentTimeMillis()

        if (userPrompt.isNotBlank()) {
            val userMsg = ChatMessage(
                id = idGenerator.newId(),
                conversationId = conversationId,
                role = Role.USER,
                blocks = listOf(ContentBlock.Text(userPrompt)),
                status = MessageStatus.COMPLETE,
                createdAt = now
            )
            chatRepository.saveMessage(userMsg)
        }

        val run = AgentRun(
            id = runId,
            conversationId = conversationId,
            projectId = projectId,
            status = RunStatus.RUNNING,
            mode = mode,
            stepCount = 0,
            startedAt = now,
            lastEventAt = now,
            totalTokensUsed = 0
        )
        runStateStore.saveRun(run)

        try {
            agentLoop.execute(
                run = run,
                config = AgentConfig(),
                onEvent = { event ->
                    emit(event)
                }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                emit(AgentEvent.Cancelled)
                runStateStore.updateStatus(runId, RunStatus.CANCELLED)
                throw e
            } else {
                emit(AgentEvent.Error("Run failed: ${e.message}", fatal = true))
                runStateStore.updateStatus(runId, RunStatus.FAILED)
            }
        } finally {
            activeJobs.remove(runId)
        }
    }

    override suspend fun stop(runId: String) {
        activeJobs[runId]?.cancel()
        activeJobs.remove(runId)
        runStateStore.updateStatus(runId, RunStatus.CANCELLED)
    }

    override fun continueRun(runId: String): Flow<AgentEvent> = flow {
        val existing = runStateStore.getRun(runId)
        if (existing == null) {
            emit(AgentEvent.Error("Run $runId not found to continue.", fatal = true))
            return@flow
        }

        val resumedRun = existing.copy(
            status = RunStatus.RUNNING,
            lastEventAt = System.currentTimeMillis()
        )
        runStateStore.saveRun(resumedRun)

        try {
            agentLoop.execute(
                run = resumedRun,
                config = AgentConfig(),
                onEvent = { event ->
                    emit(event)
                }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                emit(AgentEvent.Cancelled)
                runStateStore.updateStatus(runId, RunStatus.CANCELLED)
                throw e
            } else {
                emit(AgentEvent.Error("Resumed run failed: ${e.message}", fatal = true))
                runStateStore.updateStatus(runId, RunStatus.FAILED)
            }
        } finally {
            activeJobs.remove(runId)
        }
    }
}
