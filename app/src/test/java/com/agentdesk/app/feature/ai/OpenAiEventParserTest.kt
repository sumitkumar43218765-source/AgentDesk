package com.agentdesk.app.feature.ai

import com.agentdesk.app.feature.ai.data.ToolCallAssembler
import com.agentdesk.app.feature.ai.data.http.SseEvent
import com.agentdesk.app.feature.ai.data.openai.OpenAiEventParser
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.StopReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiEventParserTest {

    private lateinit var parser: OpenAiEventParser
    private lateinit var assembler: ToolCallAssembler

    @Before
    fun setUp() {
        assembler = ToolCallAssembler()
        parser = OpenAiEventParser(assembler)
    }

    @Test
    fun parse_textChunk_emitsTextDelta() {
        val fixture = SseEvent(
            data = """{"id":"chatcmpl-123","choices":[{"index":0,"delta":{"content":"Hi there"},"finish_reason":null}]}"""
        )

        val events = parser.parse(fixture)

        assertEquals(1, events.size)
        assertTrue(events[0] is LlmEvent.TextDelta)
        assertEquals("Hi there", (events[0] as LlmEvent.TextDelta).text)
    }

    @Test
    fun parse_doneMarker_emitsDone() {
        val fixture = SseEvent(data = "[DONE]")

        val events = parser.parse(fixture)

        assertEquals(1, events.size)
        assertTrue(events[0] is LlmEvent.Done)
        assertEquals(StopReason.END_TURN, (events[0] as LlmEvent.Done).stopReason)
    }
}
