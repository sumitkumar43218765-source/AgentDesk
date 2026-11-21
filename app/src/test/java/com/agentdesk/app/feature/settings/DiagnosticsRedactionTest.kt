package com.agentdesk.app.feature.settings

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.logging.LogRedactor
import com.agentdesk.app.feature.settings.data.DiagnosticsZipWriter
import com.agentdesk.app.feature.settings.domain.model.DiagnosticsBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class DiagnosticsRedactionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val logRedactor = LogRedactor()

    @Test
    fun testPlantedApiKeysAreRedacted() {
        val rawInput = """
            Connecting to OpenAI: api_key=sk-proj-1234567890abcdef1234567890
            Connecting to Anthropic: sk-ant-api03-abcdef123456789012345678901234
            Authorization: Bearer mySecretToken1234567890
            User config: password = "super_secret_password"
        """.trimIndent()

        val redacted = logRedactor.redact(rawInput)

        assertFalse(redacted.contains("sk-proj-1234567890abcdef1234567890"))
        assertFalse(redacted.contains("sk-ant-api03-abcdef123456789012345678901234"))
        assertFalse(redacted.contains("mySecretToken1234567890"))
        assertFalse(redacted.contains("super_secret_password"))

        assertTrue(redacted.contains("sk-ant-[REDACTED]"))
        assertTrue(redacted.contains("Bearer [REDACTED]"))
    }

    @Test
    fun testDiagnosticsZipWriterRedactsLogs() {
        val dummyContext = object : android.content.ContextWrapper(null) {}
        val zipWriter = DiagnosticsZipWriter(dummyContext, logRedactor)

        val logFile = tempFolder.newFile("sample_run.log")
        logFile.writeText("Agent executed: export OPENAI_API_KEY=sk-1234567890abcdef1234567890")

        val bundle = DiagnosticsBundle(
            timestamp = 1000L,
            appVersion = "1.0.0 (1)",
            androidVersion = 34,
            deviceModel = "Pixel 8",
            logFiles = listOf(logFile.absolutePath),
            environmentSummary = "Environment info: user token: Bearer abcdef123456"
        )

        val zipOut = File(tempFolder.root, "test-diagnostics.zip")
        val result = zipWriter.writeZipTo(bundle, zipOut)

        assertTrue(result is AppResult.Success)
        assertTrue(zipOut.exists())

        // Read zip and verify contents
        ZipFile(zipOut).use { zip ->
            val summaryEntry = zip.getEntry("environment_summary.txt")
            val summaryText = zip.getInputStream(summaryEntry).bufferedReader().readText()
            assertTrue(summaryText.contains("Environment info"))

            val logEntry = zip.getEntry("logs/${logFile.name}")
            val logText = zip.getInputStream(logEntry).bufferedReader().readText()
            assertFalse(logText.contains("sk-1234567890abcdef1234567890"))
            assertTrue(logText.contains("sk-[REDACTED]"))
        }
    }
}
