package com.agentdesk.app.feature.build.domain.model

data class StepResultSummary(
    val step: BuildStepId,
    val status: StepStatus,
    val durationMs: Long
)
