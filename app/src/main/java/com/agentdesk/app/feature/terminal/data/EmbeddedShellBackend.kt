package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import com.agentdesk.app.feature.terminal.domain.ShellResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class EmbeddedShellBackend @Inject constructor(
    private val commandBuilder: ProotCommandBuilder,
    private val executionRegistry: ExecutionRegistry,
    private val outputBuffer: OutputBuffer,
    private val pathProvider: PathProvider
) : ShellBackend {

    override suspend fun isReady(): Boolean = true

    override fun run(request: ShellRequest): Flow<ShellEvent> = callbackFlow {
        val startTime = System.currentTimeMillis()
        trySend(ShellEvent.Started(request.executionId))

        val sessionBuffer = outputBuffer.createSession(request.executionId)
        val cmdList = commandBuilder.buildCommandLine(request)

        val processBuilder = ProcessBuilder(cmdList)
        request.cwd?.let {
            val hostCwd = pathProvider.guestToHost(it)
            if (hostCwd.exists()) {
                processBuilder.directory(hostCwd)
            }
        }

        // Add custom environment variables
        val processEnv = processBuilder.environment()
        for ((k, v) in request.env) {
            processEnv[k] = v
        }

        val process = try {
            processBuilder.start()
        } catch (e: Exception) {
            trySend(ShellEvent.Failure(AppError.Shell("Failed to start process: ${e.message}")))
            close()
            return@callbackFlow
        }

        executionRegistry.register(request.executionId, coroutineContext[kotlinx.coroutines.Job]!!, process)

        val stdoutThread = thread(name = "stdout-${request.executionId}") {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val chunk = "$line\n"
                        sessionBuffer.appendStdout(chunk)
                        trySend(ShellEvent.Stdout(chunk))
                    }
                }
            } catch (ignored: Exception) {}
        }

        val stderrThread = thread(name = "stderr-${request.executionId}") {
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val chunk = "$line\n"
                        sessionBuffer.appendStderr(chunk)
                        trySend(ShellEvent.Stderr(chunk))
                    }
                }
            } catch (ignored: Exception) {}
        }

        thread(name = "wait-${request.executionId}") {
            try {
                val exitCode = process.waitFor()
                stdoutThread.join(2000)
                stderrThread.join(2000)
                val duration = System.currentTimeMillis() - startTime
                trySend(ShellEvent.Exited(exitCode, duration))
                close()
            } catch (e: InterruptedException) {
                trySend(ShellEvent.Cancelled)
                close()
            } finally {
                executionRegistry.unregister(request.executionId)
            }
        }

        awaitClose {
            executionRegistry.cancel(request.executionId)
        }
    }

    override suspend fun runToCompletion(request: ShellRequest): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val sessionBuffer = outputBuffer.createSession(request.executionId)
        var exitCode = -1

        val result = withTimeoutOrNull(request.timeoutMs) {
            run(request).collect { event ->
                when (event) {
                    is ShellEvent.Stdout -> sessionBuffer.appendStdout(event.chunk)
                    is ShellEvent.Stderr -> sessionBuffer.appendStderr(event.chunk)
                    is ShellEvent.Exited -> exitCode = event.code
                    is ShellEvent.Cancelled -> exitCode = -2
                    is ShellEvent.TimedOut -> exitCode = -3
                    is ShellEvent.Failure -> exitCode = -4
                    else -> {}
                }
            }
            true
        }

        val duration = System.currentTimeMillis() - startTime
        val bufferRes = sessionBuffer.finish()

        if (result == null) {
            cancel(request.executionId)
            ShellResult(
                exitCode = -3,
                stdout = bufferRes.stdout,
                stderr = "Command timed out after ${request.timeoutMs}ms",
                durationMs = duration,
                truncated = bufferRes.isTruncated,
                fullLogPath = bufferRes.fullLogPath
            )
        } else {
            ShellResult(
                exitCode = exitCode,
                stdout = bufferRes.stdout,
                stderr = bufferRes.stderr,
                durationMs = duration,
                truncated = bufferRes.isTruncated,
                fullLogPath = bufferRes.fullLogPath
            )
        }
    }

    override suspend fun cancel(executionId: String) {
        executionRegistry.cancel(executionId)
    }

    override fun openSession(request: SessionRequest): SessionHandle {
        val sessionId = "session_${System.currentTimeMillis()}"
        val isRunningState = MutableStateFlow(true)

        return object : SessionHandle {
            override val id: String = sessionId
            override val isRunning: StateFlow<Boolean> = isRunningState

            override fun write(text: String) {
                // Interactive PTY write
            }

            override fun close() {
                isRunningState.value = false
            }
        }
    }
}
