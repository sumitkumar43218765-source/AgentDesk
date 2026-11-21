package com.agentdesk.app.feature.agent.domain.model

import kotlinx.coroutines.Job

data class ToolContext(
    val projectId: String,
    val conversationId: String,
    val runId: String,
    val cancelJob: Job,
    val toolCallId: String = ""
)
