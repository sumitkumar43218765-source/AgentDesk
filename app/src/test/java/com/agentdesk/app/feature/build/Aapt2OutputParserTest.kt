package com.agentdesk.app.feature.build

import com.agentdesk.app.feature.build.data.parse.Aapt2OutputParser
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class Aapt2OutputParserTest {

    private lateinit var parser: Aapt2OutputParser

    @Before
    fun setup() {
        parser = Aapt2OutputParser()
    }

    @Test
    fun testParseFileLineError() {
        val raw = "res/layout/activity_main.xml:12: error: resource string/non_existent not found."
        val list = parser.parse(raw, BuildStepId.RESOURCE_LINK)

        assertEquals(1, list.size)
        val diag = list.first()
        assertEquals(Severity.ERROR, diag.severity)
        assertEquals("res/layout/activity_main.xml", diag.file)
        assertEquals(12, diag.line)
        assertNull(diag.column)
        assertEquals("resource string/non_existent not found.", diag.message)
        assertEquals(BuildStepId.RESOURCE_LINK, diag.step)
    }

    @Test
    fun testParseFileLineColumnWarning() {
        val raw = "res/values/strings.xml:5:10: warning: string name should start with lowercase."
        val list = parser.parse(raw, BuildStepId.RESOURCE_COMPILE)

        assertEquals(1, list.size)
        val diag = list.first()
        assertEquals(Severity.WARNING, diag.severity)
        assertEquals("res/values/strings.xml", diag.file)
        assertEquals(5, diag.line)
        assertEquals(10, diag.column)
        assertEquals("string name should start with lowercase.", diag.message)
    }

    @Test
    fun testParseGenericError() {
        val raw = "error: failed to open directory: No such file or directory"
        val list = parser.parse(raw, BuildStepId.RESOURCE_COMPILE)

        assertEquals(1, list.size)
        val diag = list.first()
        assertEquals(Severity.ERROR, diag.severity)
        assertNull(diag.file)
        assertNull(diag.line)
        assertEquals("failed to open directory: No such file or directory", diag.message)
    }
}
