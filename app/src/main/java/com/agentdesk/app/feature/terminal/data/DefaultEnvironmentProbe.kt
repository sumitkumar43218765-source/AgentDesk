package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.feature.terminal.domain.EnvironmentProbe
import com.agentdesk.app.feature.terminal.domain.ProbeResult
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEnvironmentProbe @Inject constructor(
    private val shellBackend: ShellBackend
) : EnvironmentProbe {

    override suspend fun runProbes(): List<ProbeResult> {
        val probes = listOf(
            Triple("bash", "bash --version", ShellEnvironment.TERMUX_NATIVE),
            Triple("proot", "proot --version", ShellEnvironment.TERMUX_NATIVE),
            Triple("ubuntu", "cat /etc/os-release", ShellEnvironment.PROOT_UBUNTU),
            Triple("java", "java -version", ShellEnvironment.PROOT_UBUNTU),
            Triple("aapt2", "aapt2 version", ShellEnvironment.PROOT_UBUNTU)
        )

        val results = mutableListOf<ProbeResult>()

        for ((name, cmd, env) in probes) {
            try {
                val req = ShellRequest(
                    command = cmd,
                    environment = env,
                    executionId = "probe_${UUID.randomUUID()}",
                    timeoutMs = 10_000
                )
                val res = shellBackend.runToCompletion(req)
                val passed = res.exitCode == 0
                val output = (res.stdout + "\n" + res.stderr).trim().take(150)

                results.add(
                    ProbeResult(
                        name = name,
                        passed = passed,
                        version = if (passed) output else null,
                        error = if (!passed) "Exit code ${res.exitCode}: $output" else null
                    )
                )
            } catch (e: Exception) {
                results.add(
                    ProbeResult(
                        name = name,
                        passed = false,
                        error = e.message ?: "Probe error"
                    )
                )
            }
        }

        return results
    }
}
