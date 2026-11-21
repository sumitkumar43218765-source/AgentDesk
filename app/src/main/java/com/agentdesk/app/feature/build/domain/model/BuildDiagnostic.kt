package com.agentdesk.app.feature.build.domain.model

data class BuildDiagnostic(
    val severity: Severity,
    val file: String?,
    val line: Int?,
    val column: Int?,
    val message: String,
    val step: BuildStepId
)
