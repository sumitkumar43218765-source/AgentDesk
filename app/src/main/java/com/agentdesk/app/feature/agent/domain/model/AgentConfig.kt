package com.agentdesk.app.feature.agent.domain.model

data class AgentConfig(
    val maxSteps: Int = 40,
    val maxConsecutiveToolErrors: Int = 5,
    val maxBuildRetries: Int = 5,
    val toolOutputMaxChars: Int = 8000,
    val runTokenBudget: Int = 500_000,
    val shellTimeoutSeconds: Long = 120L,
    val buildTimeoutSeconds: Long = 600L
)
