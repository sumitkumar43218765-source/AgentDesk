package com.agentdesk.app.feature.build.domain.model

sealed interface BuildEvent {
    data class Started(val buildId: String) : BuildEvent
    data class StepStarted(val step: BuildStepId) : BuildEvent
    data class StepLog(val step: BuildStepId, val chunk: String) : BuildEvent
    data class StepFinished(val step: BuildStepId, val status: StepStatus, val durationMs: Long) : BuildEvent
    data class Finished(val run: BuildRun) : BuildEvent
}
