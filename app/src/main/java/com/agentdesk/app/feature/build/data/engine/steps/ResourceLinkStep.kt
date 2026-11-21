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

class ResourceLinkStep @Inject constructor(
    private val shellBackend: ShellBackend,
    private val aapt2OutputParser: Aapt2OutputParser
) : BuildStep {

    override val id: BuildStepId = BuildStepId.RESOURCE_LINK
    override val timeoutSeconds: Long = 120L

    override suspend fun execute(
        context: BuildContext,
        onLogChunk: (String) -> Unit
    ): StepResult {
        val startTime = System.currentTimeMillis()
        val resZip = File(context.buildDir, "res.zip")
        val resArg = if (resZip.exists()) "build/res.zip" else ""

        val command = "mkdir -p build/gen && aapt2 link -I \"${context.androidJarGuestPath}\" " +
                "--manifest AndroidManifest.xml " +
                "--java build/gen " +
                "--min-sdk-version ${context.manifestInfo.minSdkVersion} " +
                "--target-sdk-version ${context.manifestInfo.targetSdkVersion} " +
                "-o build/app.unsigned.apk $resArg"

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
            errorMessage = if (status == StepStatus.FAILED) "aapt2 link failed with exit code $exitCode" else null
        )
    }
}
