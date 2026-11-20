package com.agentdesk.app.feature.setup.data.check

import android.content.Context
import com.agentdesk.app.feature.terminal.domain.PathProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageUsageCalculator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pathProvider: PathProvider
) {

    data class StorageBreakdown(
        val rootfsBytes: Long,
        val projectsBytes: Long,
        val logsBytes: Long,
        val cacheBytes: Long,
        val totalBytes: Long
    )

    fun calculateBreakdown(): StorageBreakdown {
        val rootfsBytes = getDirSize(pathProvider.rootfsDir)
        val projectsBytes = getDirSize(pathProvider.projectsRoot)
        val logsBytes = getDirSize(pathProvider.logsDir)
        val cacheBytes = getDirSize(context.cacheDir)
        val totalBytes = rootfsBytes + projectsBytes + logsBytes + cacheBytes

        return StorageBreakdown(
            rootfsBytes = rootfsBytes,
            projectsBytes = projectsBytes,
            logsBytes = logsBytes,
            cacheBytes = cacheBytes,
            totalBytes = totalBytes
        )
    }

    fun cleanCaches(): Long {
        val initialCache = getDirSize(context.cacheDir)
        deleteContents(context.cacheDir)
        deleteContents(pathProvider.tmpDir)
        return initialCache
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    private fun deleteContents(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }
}
