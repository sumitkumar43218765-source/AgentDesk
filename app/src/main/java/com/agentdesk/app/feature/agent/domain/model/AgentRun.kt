package com.agentdesk.app.feature.agent.domain.model

data class AgentRun(
    val id: String,
    val conversationId: String,
    val projectId: String,
    val status: RunStatus,
    val mode: RunMode,
    val stepCount: Int,
    val startedAt: Long,
    val lastEventAt: Long,
    val totalTokensUsed: Int
)
