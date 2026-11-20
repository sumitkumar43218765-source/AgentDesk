package com.agentdesk.app.feature.terminal

import com.agentdesk.app.feature.terminal.data.OutputBuffer
import com.agentdesk.app.feature.terminal.domain.PathProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OutputBufferTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var pathProvider: PathProvider
    private lateinit var outputBuffer: OutputBuffer

    @Before
    fun setUp() {
        pathProvider = mockk()
        every { pathProvider.logsDir } returns tempFolder.root
        outputBuffer = OutputBuffer(pathProvider)
    }

    @Test
    fun appendStdout_underLimit_keepsInMemoryWithoutTruncation() {
        val session = outputBuffer.createSession("exec_small")
        session.appendStdout("Hello stdout\n")

        val result = session.finish()
        assertEquals("Hello stdout\n", result.stdout)
        assertEquals(false, result.isTruncated)
    }

    @Test
    fun appendStdout_overFiveMb_truncatesInMemoryAndSpillsToDisk() {
        val session = outputBuffer.createSession("exec_large")
        // 5 MB output chunks
        val largeChunk = "A".repeat(500 * 1024)
        repeat(11) { // 5.5 MB total
            session.appendStdout(largeChunk)
        }

        val result = session.finish()
        assertTrue(result.isTruncated)
        assertTrue(result.stdout.length <= OutputBuffer.MAX_MEMORY_BYTES)
        assertNotNull(result.fullLogPath)

        val diskFile = File(result.fullLogPath!!)
        assertTrue(diskFile.exists())
        assertTrue(diskFile.length() > OutputBuffer.MAX_MEMORY_BYTES)
    }
}
