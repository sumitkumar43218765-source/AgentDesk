package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import com.agentdesk.app.feature.terminal.domain.ShellResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeShellBackend @Inject constructor() : ShellBackend {

    override suspend fun isReady(): Boolean = true

    override fun run(request: ShellRequest): Flow<ShellEvent> = flow {
        emit(ShellEvent.Started(request.executionId))

        when {
            request.command.contains("1>&2") -> {
                emit(ShellEvent.Stdout("hi\n"))
                emit(ShellEvent.Stderr("err\n"))
                emit(ShellEvent.Exited(0, 10))
            }
            request.command.startsWith("echo ") -> {
                val output = request.command.removePrefix("echo ").trim('"', '\'', ' ') + "\n"
                emit(ShellEvent.Stdout(output))
                emit(ShellEvent.Exited(0, 5))
            }
            request.command.contains("exit ") -> {
                val code = request.command.substringAfter("exit ").trim().toIntOrNull() ?: 1
                emit(ShellEvent.Exited(code, 5))
            }
            else -> {
                emit(ShellEvent.Stdout("Fake output for: ${request.command}\n"))
                emit(ShellEvent.Exited(0, 10))
            }
        }
    }

    override suspend fun runToCompletion(request: ShellRequest): ShellResult {
        var stdout = ""
        var stderr = ""
        var exitCode = 0
        var duration = 0L

        run(request).collect { event ->
            when (event) {
                is ShellEvent.Stdout -> stdout += event.chunk
                is ShellEvent.Stderr -> stderr += event.chunk
                is ShellEvent.Exited -> {
                    exitCode = event.code
                    duration = event.durationMs
                }
                else -> {}
            }
        }

        return ShellResult(
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            durationMs = duration,
            truncated = false,
            fullLogPath = null
        )
    }

    override suspend fun cancel(executionId: String) {}

    override fun openSession(request: SessionRequest): SessionHandle {
        return object : SessionHandle {
            override val id: String = "fake_session_1"
            override val isRunning: StateFlow<Boolean> = MutableStateFlow(true)
            override fun write(text: String) {}
            override fun close() {}
        }
    }
}
