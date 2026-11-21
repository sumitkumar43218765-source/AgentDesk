package com.agentdesk.app.feature.agent.data.integration

import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import com.agentdesk.app.feature.projects.domain.repository.AskAgentAboutFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AskAgentAboutFileImpl @Inject constructor(
    private val chatRepository: ChatRepository,
    private val agentRunner: AgentRunner
) : AskAgentAboutFile {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun ask(projectId: String, relativePath: String, lineRange: IntRange?) {
        scope.launch {
            val conversations = chatRepository.observeConversations().firstOrNull() ?: emptyList()
            val conversation = conversations.firstOrNull { it.projectId == projectId } ?: conversations.firstOrNull()
            if (conversation != null) {
                val linesDesc = if (lineRange != null) " (lines ${lineRange.first}-${lineRange.last})" else ""
                val prompt = "Please inspect file `$relativePath`$linesDesc and explain its purpose and structure."
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
