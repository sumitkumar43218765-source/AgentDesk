package com.agentdesk.app.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LogRotationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var logRotation: LogRotation

    @Before
    fun setUp() {
        logRotation = LogRotation()
    }

    @Test
    fun rotateIfNeeded_smallFile_doesNotRotate() {
        val logFile = tempFolder.newFile("app.log")
        logFile.writeText("Small log content")

        logRotation.rotateIfNeeded(logFile)

        assertTrue(logFile.exists())
        val rotatedFile = File(tempFolder.root, "app.log.1")
        assertFalse(rotatedFile.exists())
    }

    @Test
    fun rotateIfNeeded_oversizedFile_rotatesAndShifts() {
        val logFile = tempFolder.newFile("app.log")
        // Write bytes > 2 MB threshold
        val largeBytes = ByteArray((2 * 1024 * 1024) + 1024) { 'A'.code.toByte() }
        logFile.writeBytes(largeBytes)

        logRotation.rotateIfNeeded(logFile)

        assertFalse(logFile.exists())
        val rotated1 = File(tempFolder.root, "app.log.1")
        assertTrue(rotated1.exists())
        assertEquals(largeBytes.size.toLong(), rotated1.length())
    }
}
