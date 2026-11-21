package com.agentdesk.app.feature.projects.domain.usecase

import com.agentdesk.app.feature.projects.domain.model.Project
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EvaluateBackupReminderUseCase @Inject constructor() {

    companion object {
        const val CHECKPOINT_THRESHOLD = 10
        val TIME_THRESHOLD_MS = TimeUnit.DAYS.toMillis(7)
    }

    operator fun invoke(project: Project, now: Long = System.currentTimeMillis()): Boolean {
        // Remind if >= 10 checkpoints since last backup
        if (project.checkpointCount >= CHECKPOINT_THRESHOLD) {
            return true
        }

        // Remind if >= 7 days since last backup (or since created if never backed up)
        val lastBackupTime = project.lastBackupAt ?: project.createdAt
        val elapsed = now - lastBackupTime
        return elapsed >= TIME_THRESHOLD_MS
    }
}
