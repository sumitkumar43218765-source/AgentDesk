package com.agentdesk.app.feature.agent

import com.agentdesk.app.feature.agent.data.runtime.LoopGuard
import com.agentdesk.app.feature.agent.domain.model.LoopGuardResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoopGuardTest {

    private lateinit var loopGuard: LoopGuard

    @Before
    fun setUp() {
        loopGuard = LoopGuard(
            maxConsecutiveToolErrors = 5,
            repeatWarningThreshold = 3,
            repeatAbortThreshold = 5
        )
    }

    @Test
    fun `repeated identical tool calls trigger warning then abort`() {
        val args = buildJsonObject { put("path", "src/MainActivity.java") }

        // 1st and 2nd calls pass
        assertTrue(loopGuard.checkBeforeExecution("read_file", args) is LoopGuardResult.Pass)
        loopGuard.recordExecution("read_file", args, isError = false)

        assertTrue(loopGuard.checkBeforeExecution("read_file", args) is LoopGuardResult.Pass)
        loopGuard.recordExecution("read_file", args, isError = false)

        // 3rd call triggers warning
        val third = loopGuard.checkBeforeExecution("read_file", args)
        assertTrue("Expected Warning on 3rd call", third is LoopGuardResult.Warning)
        loopGuard.recordExecution("read_file", args, isError = false)

        // 4th call is warning
        val fourth = loopGuard.checkBeforeExecution("read_file", args)
        assertTrue(fourth is LoopGuardResult.Warning)
        loopGuard.recordExecution("read_file", args, isError = false)

        // 5th call aborts
        val fifth = loopGuard.checkBeforeExecution("read_file", args)
        assertTrue("Expected Abort on 5th call", fifth is LoopGuardResult.Abort)
    }

    @Test
    fun `consecutive errors abort after 5 failures and reset on success`() {
        val args = buildJsonObject { put("cmd", "broken") }

        for (i in 1..4) {
            val res = loopGuard.recordExecution("shell_exec", args, isError = true)
            assertTrue("Call $i should pass", res is LoopGuardResult.Pass)
        }

        // 5th consecutive error aborts
        val fifth = loopGuard.recordExecution("shell_exec", args, isError = true)
        assertTrue("5th consecutive error must abort", fifth is LoopGuardResult.Abort)

        // Reset and test success resets streak
        loopGuard.reset()
        loopGuard.recordExecution("shell_exec", args, isError = true)
        loopGuard.recordExecution("shell_exec", args, isError = true)
        loopGuard.recordExecution("shell_exec", args, isError = false) // Success!
        val afterSuccess = loopGuard.recordExecution("shell_exec", args, isError = true)
        assertTrue("After success, error streak resets", afterSuccess is LoopGuardResult.Pass)
    }
}
