package com.agentdesk.app.feature.agent.presentation

import android.content.Context
import android.content.Intent
import com.agentdesk.app.feature.agent.data.runtime.RunStateStore
import com.agentdesk.app.feature.agent.data.service.AgentRunService
import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.model.AgentRun
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.PermissionRequest
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.agent.domain.usecase.AnswerAskUserUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentChatBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runStateStore: RunStateStore,
    private val permissionGateway: PermissionGateway,
    private val answerAskUserUseCase: AnswerAskUserUseCase
) {

    fun observeLatestRun(conversationId: String): Flow<AgentRun?> {
        return runStateStore.observeLatestRun(conversationId)
    }

    fun observePendingRequests(runId: String): Flow<List<PermissionRequest>> {
        return permissionGateway.observePendingRequests(runId)
    }

    fun startAgentRun(conversationId: String, projectId: String, prompt: String, mode: RunMode = RunMode.ASK) {
        val intent = Intent(context, AgentRunService::class.java).apply {
            action = AgentRunService.ACTION_START_RUN
            putExtra(AgentRunService.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(AgentRunService.EXTRA_PROJECT_ID, projectId)
            putExtra(AgentRunService.EXTRA_USER_PROMPT, prompt)
            putExtra(AgentRunService.EXTRA_MODE, mode.name)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopAgentRun(runId: String) {
        val intent = Intent(context, AgentRunService::class.java).apply {
            action = AgentRunService.ACTION_STOP_RUN
            putExtra(AgentRunService.EXTRA_RUN_ID, runId)
        }
        context.startService(intent)
    }

    fun continueRun(runId: String) {
        val intent = Intent(context, AgentRunService::class.java).apply {
            action = AgentRunService.ACTION_CONTINUE_RUN
            putExtra(AgentRunService.EXTRA_RUN_ID, runId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    suspend fun resolvePermission(requestId: String, decision: PermissionDecision) {
        permissionGateway.resolve(requestId, decision)
    }

    suspend fun answerQuestion(questionId: String, answer: String) {
        answerAskUserUseCase(questionId, answer)
    }
}
