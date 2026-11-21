package com.agentdesk.app.feature.build.presentation.build

import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.StepStatus

data class BuildUiState(
    val projectId: String = "",
    val projectName: String = "",
    val isBuilding: Boolean = false,
    val currentStep: BuildStepId? = null,
    val currentBuildId: String? = null,
    val stepStatuses: Map<BuildStepId, StepStatus> = BuildStepId.entries.associateWith { StepStatus.PENDING },
    val stepDurations: Map<BuildStepId, Long> = emptyMap(),
    val logOutput: String = "",
    val diagnostics: List<BuildDiagnostic> = emptyList(),
    val latestRun: BuildRun? = null,
    val selectedVariant: BuildVariant = BuildVariant.DEBUG,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
