package com.agentdesk.app.feature.agent.domain

interface SystemPromptBuilder {
    suspend fun build(projectId: String): String
}
