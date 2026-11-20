package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutputBuffer @Inject constructor(
    private val pathProvider: PathProvider
) {
    companion object {
        const val MAX_MEMORY_BYTES = 1024 * 1024 // 1 MB
    }

    fun createSession(executionId: String): SessionBuffer {
        return SessionBuffer(executionId, pathProvider.logsDir)
    }

    class SessionBuffer(
        private val executionId: String,
        private val logsDir: File
    ) {
        private val stdoutBuilder = StringBuilder()
        private val stderrBuilder = StringBuilder()
        private var isTruncated = false
        private var diskFile: File? = null
        private var fileWriter: FileWriter? = null

        @Synchronized
        fun appendStdout(chunk: String) {
            writeToDisk("STDOUT: $chunk")
            if (stdoutBuilder.length + chunk.length <= MAX_MEMORY_BYTES) {
                stdoutBuilder.append(chunk)
            } else {
                isTruncated = true
                val remaining = MAX_MEMORY_BYTES - stdoutBuilder.length
                if (remaining > 0) {
                    stdoutBuilder.append(chunk.take(remaining))
                }
            }
        }

        @Synchronized
        fun appendStderr(chunk: String) {
            writeToDisk("STDERR: $chunk")
            if (stderrBuilder.length + chunk.length <= MAX_MEMORY_BYTES) {
                stderrBuilder.append(chunk)
            } else {
                isTruncated = true
                val remaining = MAX_MEMORY_BYTES - stderrBuilder.length
                if (remaining > 0) {
                    stderrBuilder.append(chunk.take(remaining))
                }
            }
        }

        private fun writeToDisk(line: String) {
            try {
                if (diskFile == null) {
                    val execDir = File(logsDir, "exec").apply { if (!exists()) mkdirs() }
                    diskFile = File(execDir, "$executionId.log")
                    fileWriter = FileWriter(diskFile, true)
                }
                fileWriter?.write(line)
                fileWriter?.flush()
            } catch (ignored: Exception) {}
        }

        @Synchronized
        fun finish(): BufferResult {
            try {
                fileWriter?.close()
            } catch (ignored: Exception) {}

            return BufferResult(
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                isTruncated = isTruncated,
                fullLogPath = diskFile?.absolutePath
            )
        }
    }

    data class BufferResult(
        val stdout: String,
        val stderr: String,
        val isTruncated: Boolean,
        val fullLogPath: String?
    )
}
