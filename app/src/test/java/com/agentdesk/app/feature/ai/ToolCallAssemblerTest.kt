package com.agentdesk.app.feature.ai

import com.agentdesk.app.feature.ai.data.ToolCallAssembler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToolCallAssemblerTest {

    private lateinit var assembler: ToolCallAssembler

    @Before
    fun setUp() {
        assembler = ToolCallAssembler()
    }

    @Test
    fun completeToolCall_splitAcrossFiveDeltas_assembledCorrectly() {
        val toolId = "call_abc123"
        assembler.onToolCallStarted(toolId, "write_file")

        // 5 partial JSON chunks
        val chunks = listOf(
            "{\"path\": \"/root",
            "/hello.txt\", ",
            "\"content\": ",
            "\"Hello World!\", ",
            "\"overwrite\": true}"
        )

        for (chunk in chunks) {
            assembler.onArgsDelta(toolId, chunk)
        }

        val result = assembler.completeToolCall(toolId)

        assertEquals(toolId, result.id)
        assertEquals("write_file", result.name)
        assertTrue(result.isValidJson)
        assertTrue(result.argsJson.contains("\"overwrite\": true"))
        assertTrue(result.argsJson.contains("Hello World!"))
    }

    @Test
    fun completeToolCall_invalidJson_flagsInvalid() {
        val toolId = "call_broken"
        assembler.onToolCallStarted(toolId, "run_cmd")
        assembler.onArgsDelta(toolId, "{broken json missing quote: ")

        val result = assembler.completeToolCall(toolId)

        assertEquals("run_cmd", result.name)
        assertFalse(result.isValidJson)
    }

    @Test
    fun completeToolCall_emptyArgs_returnsValidEmptyObject() {
        val toolId = "call_empty"
        assembler.onToolCallStarted(toolId, "check_env")

        val result = assembler.completeToolCall(toolId)

        assertEquals("check_env", result.name)
        assertEquals("{}", result.argsJson)
        assertTrue(result.isValidJson)
    }
}
