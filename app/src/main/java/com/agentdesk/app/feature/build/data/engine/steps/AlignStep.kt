package com.agentdesk.app.feature.build.data.engine.steps

import com.agentdesk.app.feature.build.data.engine.BuildContext
import com.agentdesk.app.feature.build.data.engine.BuildStep
import com.agentdesk.app.feature.build.data.engine.StepResult
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.StepStatus
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import java.util.UUID
import javax.inject.Inject

class AlignStep @Inject constructor(
    private val shellBackend: ShellBackend
) : BuildStep {

    override val id: BuildStepId = BuildStepId.ALIGN
    override val timeoutSeconds: Long = 120L

    override suspend fun execute(
        context: BuildContext,
        onLogChunk: (String) -> Unit
    ): StepResult {
        val startTime = System.currentTimeMillis()
        val command = "zipalign -f -p 4 build/app.unsigned.apk build/app.aligned.apk"

        val request = ShellRequest(
            command = command,
            cwd = context.guestProjectDir,
            env = mapOf(
                "PATH" to "${context.buildToolsGuestDir}:/usr/bin:/bin"
            ),
            environment = ShellEnvironment.PROOT_UBUNTU,
            timeoutMs = timeoutSeconds * 1000L,
            executionId = UUID.randomUUID().toString()
        )

        val logBuilder = StringBuilder()
        var exitCode = -1

        shellBackend.run(request).collect { event ->
            when (event) {
                is ShellEvent.Stdout -> {
                    onLogChunk(event.chunk)
                    logBuilder.append(event.chunk)
                }
                is ShellEvent.Stderr -> {
                    onLogChunk(event.chunk)
                    logBuilder.append(event.chunk)
                }
                is ShellEvent.Exited -> {
                    exitCode = event.code
                }
                is ShellEvent.TimedOut -> {
                    val msg = "\n[Step timed out after ${timeoutSeconds}s]\n"
                    onLogChunk(msg)
                    logBuilder.append(msg)
                    exitCode = 124
                }
                is ShellEvent.Cancelled -> {
                    val msg = "\n[Step cancelled]\n"
                    onLogChunk(msg)
                    logBuilder.append(msg)
                    exitCode = 130
                }
                is ShellEvent.Failure -> {
                    val msg = "\n[Shell error: ${event.error}]\n"
                    onLogChunk(msg)
                    logBuilder.append(msg)
                    exitCode = 1
                }
                is ShellEvent.Started -> Unit
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val rawLog = logBuilder.toString()
        val status = if (exitCode == 0) StepStatus.SUCCESS else StepStatus.FAILED

        return StepResult(
            status = status,
            durationMs = duration,
            rawLog = rawLog,
            errorMessage = if (status == StepStatus.FAILED) "zipalign failed with exit code $exitCode" else null
        )
    }
}
