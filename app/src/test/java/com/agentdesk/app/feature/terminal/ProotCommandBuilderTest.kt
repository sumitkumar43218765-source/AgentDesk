package com.agentdesk.app.feature.terminal

import com.agentdesk.app.feature.terminal.data.ProotCommandBuilder
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ProotCommandBuilderTest {

    private lateinit var pathProvider: PathProvider
    private lateinit var builder: ProotCommandBuilder

    @Before
    fun setUp() {
        pathProvider = mockk()
        every { pathProvider.termuxPrefix } returns File("/data/data/com.agentdesk.app/files/usr")
        every { pathProvider.rootfsDir } returns File("/data/data/com.agentdesk.app/files/rootfs/ubuntu")
        every { pathProvider.tmpDir } returns File("/data/data/com.agentdesk.app/files/usr/tmp")
        builder = ProotCommandBuilder(pathProvider)
    }

    @Test
    fun buildCommandLine_prootUbuntu_includesBindsAndEnvScrub() {
        val request = ShellRequest(
            command = "echo hello",
            environment = ShellEnvironment.PROOT_UBUNTU,
            executionId = "123",
            cwd = "/root/projects/app"
        )

        val cmd = builder.buildCommandLine(request)
        val fullString = cmd.joinToString(" ")

        assertTrue(fullString.contains("-r /data/data/com.agentdesk.app/files/rootfs/ubuntu"))
        assertTrue(fullString.contains("-b /dev"))
        assertTrue(fullString.contains("-b /proc"))
        assertTrue(fullString.contains("-b /sys"))
        assertTrue(fullString.contains("HOME=/root"))
        assertTrue(fullString.contains("-w /root/projects/app"))
        assertTrue(fullString.contains("echo hello"))
    }

    @Test
    fun buildCommandLine_bindExternalStorage_includesStorageBind() {
        val request = ShellRequest(
            command = "ls /storage/emulated/0",
            environment = ShellEnvironment.PROOT_UBUNTU,
            executionId = "124",
            bindExternalStorage = true
        )

        val cmd = builder.buildCommandLine(request)
        val fullString = cmd.joinToString(" ")

        assertTrue(fullString.contains("-b /storage/emulated/0:/storage/emulated/0"))
    }
}
