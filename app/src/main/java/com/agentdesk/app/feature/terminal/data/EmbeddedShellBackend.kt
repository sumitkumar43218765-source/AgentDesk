package com.agentdesk.app.feature.terminal.data

import android.content.Context
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import com.agentdesk.app.feature.terminal.domain.ShellResult
import com.termux.terminal.TerminalSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class EmbeddedShellBackend @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandBuilder: ProotCommandBuilder,
    private val executionRegistry: ExecutionRegistry,
    private val outputBuffer: OutputBuffer,
    private val pathProvider: PathProvider,
    private val termuxServiceConnection: TermuxServiceConnection
) : ShellBackend {

    init {
        termuxServiceConnection.bind()
    }

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
        val sessionId = "session_${UUID.randomUUID()}"
        pathProvider.termuxHome.mkdirs()
        pathProvider.tmpDir.mkdirs()

        val shellPath: String
        val shellArgs: Array<String>
        val shellEnv: Array<String>
        val cwd: String

        if (request.isFailsafe) {
            // Termux Failsafe emergency recovery shell
            shellPath = "/system/bin/sh"
            shellArgs = arrayOf("-i")
            cwd = pathProvider.termuxHome.absolutePath
            shellEnv = arrayOf(
                "HOME=${pathProvider.termuxHome.absolutePath}",
                "PATH=/system/bin:/system/xbin",
                "TERM=xterm-256color",
                "COLORTERM=truecolor",
                "PS1=failsafe:\\w\\$ "
            )
        } else {
            val isProotReady = request.environment == ShellEnvironment.PROOT_UBUNTU &&
                    File(pathProvider.termuxPrefix, "bin/proot").exists() &&
                    pathProvider.rootfsDir.exists()

            if (isProotReady) {
                shellPath = File(pathProvider.termuxPrefix, "bin/proot").absolutePath
                val prootCmd = commandBuilder.buildCommandLine(
                    ShellRequest(
                        command = request.command ?: "/bin/bash -l",
                        cwd = request.cwd ?: "/root",
                        environment = ShellEnvironment.PROOT_UBUNTU,
                        executionId = sessionId
                    )
                )
                shellArgs = prootCmd.drop(1).toTypedArray()
                cwd = pathProvider.rootfsHome.absolutePath
                shellEnv = arrayOf(
                    "HOME=/root",
                    "TERM=xterm-256color",
                    "COLORTERM=truecolor",
                    "LANG=C.UTF-8"
                )
            } else if (File(pathProvider.termuxPrefix, "bin/bash").exists()) {
                shellPath = File(pathProvider.termuxPrefix, "bin/bash").absolutePath
                shellArgs = if (request.command != null) arrayOf("-lc", request.command) else arrayOf("-l")
                cwd = request.cwd?.let { pathProvider.guestToHost(it).absolutePath } ?: pathProvider.termuxHome.absolutePath
                shellEnv = arrayOf(
                    "HOME=${pathProvider.termuxHome.absolutePath}",
                    "PREFIX=${pathProvider.termuxPrefix.absolutePath}",
                    "PATH=${pathProvider.termuxPrefix.absolutePath}/bin:${pathProvider.termuxPrefix.absolutePath}/bin/applets:/system/bin",
                    "TERM=xterm-256color",
                    "COLORTERM=truecolor",
                    "LANG=en_US.UTF-8",
                    "TMPDIR=${pathProvider.tmpDir.absolutePath}"
                )
            } else {
                shellPath = "/system/bin/sh"
                shellArgs = if (request.command != null) arrayOf("-c", request.command) else arrayOf("-i")
                cwd = pathProvider.termuxHome.absolutePath
                val path = "${pathProvider.termuxPrefix.absolutePath}/bin:/system/bin:/system/xbin"
                shellEnv = arrayOf(
                    "HOME=${pathProvider.termuxHome.absolutePath}",
                    "PATH=$path",
                    "TERM=xterm-256color",
                    "COLORTERM=truecolor",
                    "PS1=agentdesk:\\w\\$ "
                )
            }
        }

        var handle: TermuxSessionHandle? = null

        val client = DefaultTerminalSessionClient(
            context = context,
            onTitleUpdate = { _, _ -> },
            onSessionExit = {
                handle?.markFinished()
            }
        )

        val terminalSession = TerminalSession(
            shellPath,
            cwd,
            shellArgs,
            shellEnv,
            2000,
            client
        )

        termuxServiceConnection.service.value?.addSession(terminalSession)

        handle = TermuxSessionHandle(sessionId, terminalSession) {
            termuxServiceConnection.service.value?.removeSession(terminalSession)
        }

        return handle
    }
}
