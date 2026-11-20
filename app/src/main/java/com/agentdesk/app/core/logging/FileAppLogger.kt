package com.agentdesk.app.core.logging

import android.content.Context
import android.util.Log
import com.agentdesk.app.core.common.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileAppLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val redactor: LogRedactor,
    private val logRotation: LogRotation
) : AppLogger {

    private val logFile: File by lazy {
        val dir = File(context.filesDir, "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        File(dir, "app.log")
    }

    private val ringBuffer = ArrayDeque<String>(200)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun d(tag: String, message: String) {
        log("DEBUG", tag, message, null)
    }

    override fun i(tag: String, message: String) {
        log("INFO", tag, message, null)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        log("WARN", tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        log("ERROR", tag, message, throwable)
    }

    @Synchronized
    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        val redactedMessage = redactor.redact(message)
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            "\n" + redactor.redact(sw.toString())
        }.orEmpty()

        val timestamp = synchronized(dateFormat) {
            dateFormat.format(Date(clock.nowMillis()))
        }
        val logLine = "[$timestamp] [$level] [$tag] $redactedMessage$stackTrace\n"

        // Also write to Android logcat
        when (level) {
            "DEBUG" -> Log.d(tag, redactedMessage, throwable)
            "INFO" -> Log.i(tag, redactedMessage, throwable)
            "WARN" -> Log.w(tag, redactedMessage, throwable)
            "ERROR" -> Log.e(tag, redactedMessage, throwable)
        }

        // Append to in-memory fallback ring buffer
        if (ringBuffer.size >= 200) {
            ringBuffer.removeFirst()
        }
        ringBuffer.addLast(logLine)

        // Attempt write to file with rotation; fall back silently without crashing
        try {
            logRotation.rotateIfNeeded(logFile)
            FileWriter(logFile, true).use { writer ->
                writer.write(logLine)
            }
        } catch (ignored: Exception) {
            // Never crash if log file is not writable
        }
    }

    @Synchronized
    override fun getLogExport(): String {
        return try {
            if (logFile.exists() && logFile.length() > 0) {
                logFile.readText()
            } else {
                ringBuffer.joinToString("")
            }
        } catch (e: Exception) {
            ringBuffer.joinToString("")
        }
    }
}
