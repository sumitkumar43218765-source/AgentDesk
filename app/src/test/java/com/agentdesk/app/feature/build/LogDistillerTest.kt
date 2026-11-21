package com.agentdesk.app.feature.build

import com.agentdesk.app.feature.build.data.parse.LogDistiller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogDistillerTest {

    @Test
    fun testStripAnsiCodes() {
        val raw = "\u001B[31mError message\u001B[0m in build"
        val distilled = LogDistiller.distill(raw)
        assertTrue(distilled.text.contains("Error message in build"))
        assertTrue(!distilled.text.contains("\u001B[31m"))
    }

    @Test
    fun testCollapseRepeatedLines() {
        val lines = listOf(
            "Compiling sources...",
            "Downloading chunk",
            "Downloading chunk",
            "Downloading chunk",
            "Finished."
        ).joinToString("\n")

        val distilled = LogDistiller.distill(lines)
        assertTrue(distilled.text.contains("Downloading chunk"))
        assertTrue(distilled.text.contains("repeated 3 times"))
    }

    @Test
    fun testPriorityForErrorLines() {
        val sb = StringBuilder()
        for (i in 1..100) {
            sb.append("Normal log line $i\n")
        }
        sb.append("FATAL ERROR: Compilation failed at line 42\n")
        for (i in 101..150) {
            sb.append("Later log line $i\n")
        }

        val distilled = LogDistiller.distill(sb.toString(), maxChars = 2000)
        assertTrue(distilled.text.contains("FATAL ERROR: Compilation failed at line 42"))
        assertTrue(distilled.errorCount >= 1)
        assertTrue(distilled.omittedLinesCount > 0)
    }

    @Test
    fun testEnforceMaxCharsCap() {
        val longLog = "Error: ".repeat(1500)
        val maxChars = 500
        val distilled = LogDistiller.distill(longLog, maxChars = maxChars)
        assertTrue(distilled.text.length <= maxChars)
        assertTrue(distilled.text.contains("truncated to 500 chars"))
    }
}
