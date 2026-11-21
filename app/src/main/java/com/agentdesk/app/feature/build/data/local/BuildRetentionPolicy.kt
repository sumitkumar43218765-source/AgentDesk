package com.agentdesk.app.feature.build.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildRetentionPolicy @Inject constructor(
    private val buildRunDao: BuildRunDao,
    private val buildLogStore: BuildLogStore
) {
    companion object {
        const val MAX_RUNS_PER_PROJECT = 20
        const val MAX_TOTAL_LOG_BYTES = 200L * 1024L * 1024L // 200 MB
    }

    suspend fun enforceRetention(projectId: String) {
        // 1. Enforce max runs per project (keep 20 latest)
        val projectRuns = buildRunDao.getByProject(projectId)
        if (projectRuns.size > MAX_RUNS_PER_PROJECT) {
            val excessRuns = projectRuns.drop(MAX_RUNS_PER_PROJECT)
            for (run in excessRuns) {
                buildLogStore.delete(run.id)
                buildRunDao.deleteById(run.id)
            }
        }

        // 2. Enforce total log size limit (200 MB) across all runs
        var totalBytes = buildLogStore.getTotalLogSizeBytes()
        if (totalBytes > MAX_TOTAL_LOG_BYTES) {
            val allOldestRuns = buildRunDao.getAllOldestFirst()
            for (oldRun in allOldestRuns) {
                if (totalBytes <= MAX_TOTAL_LOG_BYTES) break
                val logFile = buildLogStore.getLogFile(oldRun.id)
                val size = if (logFile.exists()) logFile.length() else 0L
                buildLogStore.delete(oldRun.id)
                buildRunDao.deleteById(oldRun.id)
                totalBytes -= size
            }
        }
    }
}
