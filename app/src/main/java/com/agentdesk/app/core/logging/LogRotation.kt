package com.agentdesk.app.core.logging

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRotation @Inject constructor() {

    companion object {
        const val MAX_FILE_SIZE_BYTES: Long = 2 * 1024 * 1024 // 2 MB
        const val MAX_BACKUP_INDEX: Int = 5
    }

    @Synchronized
    fun rotateIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() < MAX_FILE_SIZE_BYTES) {
            return
        }

        val parentDir = logFile.parentFile ?: return

        // Delete the oldest log file if it exists (e.g. app.log.5)
        val oldestFile = File(parentDir, "${logFile.name}.$MAX_BACKUP_INDEX")
        if (oldestFile.exists()) {
            oldestFile.delete()
        }

        // Shift existing rotated files: app.log.4 -> app.log.5, etc.
        for (i in (MAX_BACKUP_INDEX - 1) downTo 1) {
            val current = File(parentDir, "${logFile.name}.$i")
            if (current.exists()) {
                val next = File(parentDir, "${logFile.name}.${i + 1}")
                current.renameTo(next)
            }
        }

        // Rename the active log file to app.log.1
        val firstBackup = File(parentDir, "${logFile.name}.1")
        logFile.renameTo(firstBackup)
    }
}
