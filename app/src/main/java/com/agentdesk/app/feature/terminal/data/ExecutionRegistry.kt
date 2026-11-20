package com.agentdesk.app.feature.terminal.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionRegistry @Inject constructor() {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeProcesses = ConcurrentHashMap<String, Process>()
    val concurrencySemaphore = Semaphore(2)

    fun register(id: String, job: Job, process: Process? = null) {
        activeJobs[id] = job
        if (process != null) {
            activeProcesses[id] = process
        }
    }

    fun unregister(id: String) {
        activeJobs.remove(id)
        activeProcesses.remove(id)
    }

    fun cancel(id: String) {
        activeJobs[id]?.cancel()
        activeProcesses[id]?.let { proc ->
            try {
                proc.destroy() // SIGTERM
                Thread.sleep(100)
                if (proc.isAlive) {
                    proc.destroyForcibly() // SIGKILL
                }
            } catch (ignored: Exception) {}
        }
        unregister(id)
    }
}
