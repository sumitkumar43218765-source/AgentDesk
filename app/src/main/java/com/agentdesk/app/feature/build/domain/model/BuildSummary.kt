package com.agentdesk.app.feature.build.domain.model

data class BuildSummary(
    val failedStep: BuildStepId?,
    val diagnostics: List<BuildDiagnostic>,
    val steps: List<StepResultSummary>
)
