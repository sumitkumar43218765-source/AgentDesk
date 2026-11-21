package com.agentdesk.app.feature.build.data.engine

import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.StepStatus

data class StepResult(
    val status: StepStatus,
    val durationMs: Long,
    val rawLog: String,
    val diagnostics: List<BuildDiagnostic> = emptyList(),
    val errorMessage: String? = null
)
