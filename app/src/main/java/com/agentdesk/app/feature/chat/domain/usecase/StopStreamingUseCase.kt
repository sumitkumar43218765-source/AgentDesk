package com.agentdesk.app.feature.chat.domain.usecase

import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopStreamingUseCase @Inject constructor() {
    private var activeJob: Job? = null

    fun registerJob(job: Job) {
        activeJob = job
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
    }
}
