package com.agentdesk.app.feature.setup

import com.agentdesk.app.feature.setup.data.install.StatusLineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StatusLineParserTest {

    private lateinit var parser: StatusLineParser

    @Before
    fun setUp() {
        parser = StatusLineParser()
    }

    @Test
    fun parseLine_validJsonLine_returnsEvent() {
        val line = """{"ts":1731900000,"tool":"jdk-17","event":"progress","pct":42,"msg":"Unpacking..."}"""
        val event = parser.parseLine(line)

        assertNotNull(event)
        assertEquals(1731900000L, event?.ts)
        assertEquals("jdk-17", event?.tool)
        assertEquals("progress", event?.event)
        assertEquals(42, event?.pct)
        assertEquals("Unpacking...", event?.msg)
    }

    @Test
    fun parseLine_invalidJson_returnsNull() {
        assertNull(parser.parseLine(""))
        assertNull(parser.parseLine("Random terminal output"))
        assertNull(parser.parseLine("{malformed json}"))
    }
}
