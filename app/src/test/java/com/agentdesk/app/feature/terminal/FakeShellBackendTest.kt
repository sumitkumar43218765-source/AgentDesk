package com.agentdesk.app.feature.terminal

import com.agentdesk.app.feature.terminal.data.FakeShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeShellBackendTest {

    private lateinit var fakeShellBackend: FakeShellBackend

    @Before
    fun setUp() {
        fakeShellBackend = FakeShellBackend()
    }

    @Test
    fun run_echoHiAndErr_yieldsStdoutThenStderrThenExited() = runTest {
        val request = ShellRequest(
            command = "echo hi && echo err 1>&2",
            executionId = "test_run"
        )

        val events = fakeShellBackend.run(request).toList()

        assertTrue(events[0] is ShellEvent.Started)
        assertTrue(events.any { it is ShellEvent.Stdout && it.chunk.contains("hi") })
        assertTrue(events.any { it is ShellEvent.Stderr && it.chunk.contains("err") })
        val lastEvent = events.last()
        assertTrue(lastEvent is ShellEvent.Exited && lastEvent.code == 0)
    }

    @Test
    fun runToCompletion_echoText_returnsShellResult() = runTest {
        val request = ShellRequest(
            command = "echo Hello World",
            executionId = "test_echo"
        )

        val result = fakeShellBackend.runToCompletion(request)
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("Hello World"))
    }
}
