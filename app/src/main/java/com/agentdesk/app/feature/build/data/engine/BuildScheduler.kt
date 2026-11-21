package com.agentdesk.app.feature.build.data.engine

import com.agentdesk.app.feature.terminal.domain.ShellBackend
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildScheduler @Inject constructor(
    private val shellBackend: ShellBackend
) {
    private val buildMutex = Mutex()
    private val currentBuildId = AtomicReference<String?>(null)
    private val currentExecutionId = AtomicReference<String?>(null)
    private val currentJob = AtomicReference<Job?>(null)

    suspend fun <T> schedule(
        buildId: String,
        block: suspend () -> T
    ): T {
        return buildMutex.withLock {
            currentBuildId.set(buildId)
            try {
                block()
            } finally {
                currentBuildId.compareAndSet(buildId, null)
                currentExecutionId.set(null)
                currentJob.set(null)
            }
        }
    }

    fun registerActiveJob(job: Job) {
        currentJob.set(job)
    }

    fun registerExecutionId(execId: String) {
        currentExecutionId.set(execId)
    }

    suspend fun cancel(buildId: String) {
        if (currentBuildId.get() == buildId) {
            val execId = currentExecutionId.get()
            if (execId != null) {
                shellBackend.cancel(execId)
            }
            currentJob.get()?.cancel()
        }
    }

    fun isRunning(): Boolean = currentBuildId.get() != null

    fun getActiveBuildId(): String? = currentBuildId.get()
}
