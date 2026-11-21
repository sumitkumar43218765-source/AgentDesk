package com.agentdesk.app.feature.build.data.engine.steps

import com.agentdesk.app.feature.build.data.engine.BuildContext
import com.agentdesk.app.feature.build.data.engine.BuildStep
import com.agentdesk.app.feature.build.data.engine.StepResult
import com.agentdesk.app.feature.build.data.parse.Aapt2OutputParser
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.StepStatus
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ResourceCompileStep @Inject constructor(
    private val shellBackend: ShellBackend,
    private val aapt2OutputParser: Aapt2OutputParser
) : BuildStep {

    override val id: BuildStepId = BuildStepId.RESOURCE_COMPILE
    override val timeoutSeconds: Long = 120L

    override suspend fun execute(
        context: BuildContext,
        onLogChunk: (String) -> Unit
    ): StepResult {
        val resDir = File(context.projectDir, "res")
        if (!resDir.exists() || (resDir.list()?.isEmpty() == true)) {
            val msg = "No resources found in res/, skipping resource compilation.\n"
            onLogChunk(msg)
            return StepResult(
                status = StepStatus.SUCCESS,
                durationMs = 0L,
                rawLog = msg
            )
        }

        val startTime = System.currentTimeMillis()
        val command = "mkdir -p build && aapt2 compile --dir res -o build/res.zip"
        val request = ShellRequest(
            command = command,
            cwd = context.guestProjectDir,
            env = mapOf(
                "ANDROID_JAR" to context.androidJarGuestPath,
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
        val diagnostics = if (status == StepStatus.FAILED) {
            aapt2OutputParser.parse(rawLog, id, context.projectDir.absolutePath)
        } else {
            emptyList()
        }

        return StepResult(
            status = status,
            durationMs = duration,
            rawLog = rawLog,
            diagnostics = diagnostics,
            errorMessage = if (status == StepStatus.FAILED) "aapt2 compile failed with exit code $exitCode" else null
        )
    }
}
