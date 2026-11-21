package com.agentdesk.app.feature.build.data.engine

import com.agentdesk.app.feature.build.domain.model.BuildStepId

interface BuildStep {
    val id: BuildStepId
    val timeoutSeconds: Long get() = 120L
    suspend fun execute(context: BuildContext, onLogChunk: (String) -> Unit): StepResult
}
