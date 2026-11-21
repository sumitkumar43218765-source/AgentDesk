package com.agentdesk.app.feature.build

import com.agentdesk.app.feature.build.data.parse.EcjOutputParser
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EcjOutputParserTest {

    private lateinit var parser: EcjOutputParser

    @Before
    fun setup() {
        parser = EcjOutputParser()
    }

    @Test
    fun testParseSingleError() {
        val ecjOutput = """
----------
1. ERROR in /root/projects/hello-java/src/MainActivity.java (at line 15)
	setContentView(R.layout.non_existent);
	                        ^^^^^^^^^^^^
non_existent cannot be resolved or is not a field
----------
1 problem (1 error)
        """.trimIndent()

        val diagnostics = parser.parse(ecjOutput, "/root/projects/hello-java")
        assertEquals(1, diagnostics.size)

        val diag = diagnostics.first()
        assertEquals(Severity.ERROR, diag.severity)
        assertEquals("src/MainActivity.java", diag.file)
        assertEquals(15, diag.line)
        assertEquals(26, diag.column)
        assertEquals("non_existent cannot be resolved or is not a field", diag.message)
        assertEquals(BuildStepId.JAVA_COMPILE, diag.step)
    }

    @Test
    fun testParseWarningAndError() {
        val ecjOutput = """
----------
1. WARNING in /root/projects/test/src/App.java (at line 8)
	int unused = 42;
	    ^^^^^^
The value of the local variable unused is not used
----------
2. ERROR in /root/projects/test/src/App.java (at line 12)
	foo();
	^^^
The method foo() is undefined for the type App
----------
2 problems (1 error, 1 warning)
        """.trimIndent()

        val diagnostics = parser.parse(ecjOutput, "/root/projects/test")
        assertEquals(2, diagnostics.size)

        assertEquals(Severity.WARNING, diagnostics[0].severity)
        assertEquals("src/App.java", diagnostics[0].file)
        assertEquals(8, diagnostics[0].line)

        assertEquals(Severity.ERROR, diagnostics[1].severity)
        assertEquals("src/App.java", diagnostics[1].file)
        assertEquals(12, diagnostics[1].line)
    }

    @Test
    fun testEmptyOutputGivesEmptyList() {
        val diagnostics = parser.parse("", "")
        assertTrue(diagnostics.isEmpty())
    }

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }
}
