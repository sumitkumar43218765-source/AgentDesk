package com.agentdesk.app.feature.build

import com.agentdesk.app.feature.build.data.engine.BuildScheduler
import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import com.agentdesk.app.feature.terminal.domain.ShellResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildSchedulerTest {

    private val fakeShellBackend = object : ShellBackend {
        var cancelledExecutionId: String? = null
        override suspend fun isReady(): Boolean = true
        override fun run(request: ShellRequest): Flow<ShellEvent> = emptyFlow()
        override suspend fun runToCompletion(request: ShellRequest): ShellResult =
            ShellResult(0, "", "", 0L, false, null)
        override suspend fun cancel(executionId: String) {
            cancelledExecutionId = executionId
        }
        override fun openSession(request: SessionRequest): SessionHandle = error("Not needed")
    }

    @Test
    fun testScheduleExecutesBlockAndResetsRunningState() = runTest {
        val scheduler = BuildScheduler(fakeShellBackend)
        assertFalse(scheduler.isRunning())

        var executed = false
        scheduler.schedule("build_123") {
            assertTrue(scheduler.isRunning())
            assertEquals("build_123", scheduler.getActiveBuildId())
            executed = true
        }

        assertTrue(executed)
        assertFalse(scheduler.isRunning())
    }

    @Test
    fun testCancelDelegatesToShellBackend() = runTest {
        val scheduler = BuildScheduler(fakeShellBackend)
        scheduler.registerExecutionId("exec_456")

        launch {
            scheduler.schedule("build_456") {
                scheduler.cancel("build_456")
            }
        }.join()

        assertEquals("exec_456", fakeShellBackend.cancelledExecutionId)
    }
}
