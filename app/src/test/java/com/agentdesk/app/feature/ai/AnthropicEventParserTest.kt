package com.agentdesk.app.feature.ai

import com.agentdesk.app.feature.ai.data.ToolCallAssembler
import com.agentdesk.app.feature.ai.data.anthropic.AnthropicEventParser
import com.agentdesk.app.feature.ai.data.http.SseEvent
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.StopReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnthropicEventParserTest {

    private lateinit var parser: AnthropicEventParser
    private lateinit var assembler: ToolCallAssembler

    @Before
    fun setUp() {
        assembler = ToolCallAssembler()
        parser = AnthropicEventParser(assembler)
    }

    @Test
    fun parse_textDeltaFrame_emitsTextDelta() {
        val fixture = SseEvent(
            event = "content_block_delta",
            data = """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello world!"}}"""
        )

        val events = parser.parse(fixture)

        assertEquals(1, events.size)
        assertTrue(events[0] is LlmEvent.TextDelta)
        assertEquals("Hello world!", (events[0] as LlmEvent.TextDelta).text)
    }

    @Test
    fun parse_messageStart_emitsUsage() {
        val fixture = SseEvent(
            event = "message_start",
            data = """{"type":"message_start","message":{"id":"msg_123","type":"message","role":"assistant","usage":{"input_tokens":42,"output_tokens":0}}}"""
        )

        val events = parser.parse(fixture)

        assertEquals(1, events.size)
        assertTrue(events[0] is LlmEvent.Usage)
        assertEquals(42, (events[0] as LlmEvent.Usage).inputTokens)
    }

    @Test
    fun parse_messageDeltaEndTurn_emitsDone() {
        val fixture = SseEvent(
            event = "message_delta",
            data = """{"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":15}}"""
        )

        val events = parser.parse(fixture)

        assertTrue(events.any { it is LlmEvent.Done && it.stopReason == StopReason.END_TURN })
        assertTrue(events.any { it is LlmEvent.Usage && it.outputTokens == 15 })
    }
}
