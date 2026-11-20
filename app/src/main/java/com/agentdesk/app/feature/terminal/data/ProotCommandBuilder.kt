package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProotCommandBuilder @Inject constructor(
    private val pathProvider: PathProvider
) {

    fun buildCommandLine(request: ShellRequest): List<String> {
        val command = request.command

        return when (request.environment) {
            ShellEnvironment.TERMUX_NATIVE -> {
                val bashPath = File(pathProvider.termuxPrefix, "bin/bash").absolutePath
                listOf(
                    if (File(bashPath).exists()) bashPath else "bash",
                    "-lc",
                    command
                )
            }
            ShellEnvironment.PROOT_UBUNTU -> {
                val prootBinary = File(pathProvider.termuxPrefix, "bin/proot").absolutePath
                val prootCmd = if (File(prootBinary).exists()) prootBinary else "proot"

                val prootArgs = mutableListOf(
                    prootCmd,
                    "-r", pathProvider.rootfsDir.absolutePath,
                    "-0",
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "/sys",
                    "-b", "${pathProvider.tmpDir.absolutePath}:/tmp"
                )

                if (request.bindExternalStorage) {
                    prootArgs.add("-b")
                    prootArgs.add("/storage/emulated/0:/storage/emulated/0")
                }

                val guestCwd = request.cwd ?: "/root"
                prootArgs.add("-w")
                prootArgs.add(guestCwd)

                prootArgs.add("/usr/bin/env")
                prootArgs.add("-i")
                prootArgs.add("HOME=/root")
                prootArgs.add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
                prootArgs.add("TERM=xterm-256color")
                prootArgs.add("LANG=C.UTF-8")

                for ((key, value) in request.env) {
                    prootArgs.add("$key=$value")
                }

                prootArgs.add("/bin/bash")
                prootArgs.add("-lc")
                prootArgs.add(command)

                prootArgs
            }
        }
    }
}
