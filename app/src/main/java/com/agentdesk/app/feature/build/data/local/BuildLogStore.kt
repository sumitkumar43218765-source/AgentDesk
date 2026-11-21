package com.agentdesk.app.feature.build.data.local

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.model.LogSlice
import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildLogStore @Inject constructor(
    private val pathProvider: PathProvider
) {
    fun getLogFile(buildId: String): File {
        val dir = pathProvider.buildLogsDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$buildId.log")
    }

    fun append(buildId: String, chunk: String) {
        val file = getLogFile(buildId)
        FileOutputStream(file, true).use { stream ->
            stream.write(chunk.toByteArray(Charsets.UTF_8))
        }
    }

    fun write(buildId: String, fullLog: String) {
        val file = getLogFile(buildId)
        file.writeText(fullLog, Charsets.UTF_8)
    }

    fun readSlice(buildId: String, offset: Int, limit: Int): AppResult<LogSlice> {
        val file = getLogFile(buildId)
        if (!file.exists()) {
            return AppResult.Failure(AppError.Storage("Log file not found for build $buildId"))
        }

        return try {
            val lines = file.readLines(Charsets.UTF_8)
            val totalLines = lines.size
            val safeOffset = offset.coerceIn(0, totalLines)
            val end = (safeOffset + limit).coerceAtMost(totalLines)
            val slice = if (safeOffset < totalLines) lines.subList(safeOffset, end) else emptyList()
            val hasMore = end < totalLines

            AppResult.Success(
                LogSlice(
                    lines = slice,
                    totalLines = totalLines,
                    offset = safeOffset,
                    hasMore = hasMore
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage("Failed to read build log: ${e.message}"))
        }
    }

    fun readAll(buildId: String): String {
        val file = getLogFile(buildId)
        return if (file.exists()) file.readText(Charsets.UTF_8) else ""
    }

    fun delete(buildId: String): Boolean {
        val file = getLogFile(buildId)
        return if (file.exists()) file.delete() else true
    }

    fun getTotalLogSizeBytes(): Long {
        val dir = pathProvider.buildLogsDir
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
