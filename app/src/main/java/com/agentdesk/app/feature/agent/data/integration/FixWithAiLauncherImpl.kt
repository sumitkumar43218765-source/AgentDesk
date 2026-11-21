package com.agentdesk.app.feature.agent.data.integration

import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.FixWithAiLauncher
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixWithAiLauncherImpl @Inject constructor(
    private val buildEngine: BuildEngine,
    private val chatRepository: ChatRepository,
    private val agentRunner: AgentRunner
) : FixWithAiLauncher {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun launch(projectId: String, buildId: String) {
        scope.launch {
            val run = buildEngine.getRun(buildId)
            val errorSummary = run?.summary?.diagnostics?.joinToString("\n") { "${it.severity}: ${it.message} (${it.file}:${it.line})" }?.ifBlank { null }
                ?: "Build failed at step ${run?.summary?.failedStep ?: "unknown"}."
            val conversations = chatRepository.observeConversations().firstOrNull() ?: emptyList()
            val conversation = conversations.firstOrNull { it.projectId == projectId } ?: conversations.firstOrNull()
            if (conversation != null) {
                val prompt = "Fix the following build error for project $projectId (Build ID: $buildId):\n\n$errorSummary"
                agentRunner.run(
                    conversationId = conversation.id,
                    projectId = projectId,
                    userPrompt = prompt,
                    mode = RunMode.ASK
                )
            }
        }
    }
}
