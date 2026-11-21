package com.agentdesk.app.feature.settings.data

import com.agentdesk.app.core.logging.LogRedactor
import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor(
    private val pathProvider: PathProvider,
    private val logRedactor: LogRedactor
) {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun handleCrash(thread: Thread, throwable: Throwable) {
        runCatching {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val crashReport = buildString {
                append("Crash in thread: ${thread.name}\n")
                append("Time: ${System.currentTimeMillis()}\n")
                append("Exception: ${throwable.javaClass.name}: ${throwable.message}\n\n")
                append("Stack trace:\n")
                append(stackTrace)
            }

            val redacted = logRedactor.redact(crashReport)
            val crashFile = File(pathProvider.logsDir, "crash_${System.currentTimeMillis()}.log")
            crashFile.writeText(redacted, Charsets.UTF_8)
        }
    }
}
