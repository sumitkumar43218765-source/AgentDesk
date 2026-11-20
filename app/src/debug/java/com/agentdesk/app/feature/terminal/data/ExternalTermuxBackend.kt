package com.agentdesk.app.feature.terminal.data

import android.content.Context
import android.content.Intent
import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import com.agentdesk.app.feature.terminal.domain.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExternalTermuxBackend @Inject constructor(
    @ApplicationContext private val context: Context
) : ShellBackend {

    override suspend fun isReady(): Boolean = true

    override fun run(request: ShellRequest): Flow<ShellEvent> = flow {
        emit(ShellEvent.Started(request.executionId))
        // Dev bridge via RUN_COMMAND intent to official Termux
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", request.command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }
        try {
            context.startService(intent)
            emit(ShellEvent.Stdout("Command sent to external Termux\n"))
            emit(ShellEvent.Exited(0, 10))
        } catch (e: Exception) {
            emit(ShellEvent.Stderr("External Termux not available: ${e.message}\n"))
            emit(ShellEvent.Exited(1, 10))
        }
    }

    override suspend fun runToCompletion(request: ShellRequest): ShellResult {
        return ShellResult(
            exitCode = 0,
            stdout = "Executed via external Termux",
            stderr = "",
            durationMs = 10,
            truncated = false,
            fullLogPath = null
        )
    }

    override suspend fun cancel(executionId: String) {}

    override fun openSession(request: SessionRequest): SessionHandle {
        return object : SessionHandle {
            override val id: String = "ext_1"
            override val isRunning = MutableStateFlow(true)
            override fun write(text: String) {}
            override fun close() {}
        }
    }
}
