package com.agentdesk.app.feature.projects

import com.agentdesk.app.feature.projects.data.git.GitOutputParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitOutputParserTest {

    private lateinit var parser: GitOutputParser

    @Before
    fun setUp() {
        parser = GitOutputParser()
    }

    @Test
    fun parseStatus_cleanStatus_returnsClean() {
        val porcelain = "## main...origin/main"
        val status = parser.parseStatus(porcelain)

        assertEquals("main", status.branch)
        assertTrue(status.clean)
        assertEquals(0, status.stagedCount)
        assertEquals(0, status.unstagedCount)
        assertEquals(0, status.untrackedCount)
    }

    @Test
    fun parseStatus_modifiedStatus_countsCorrectly() {
        val porcelain = """
            ## feature/new-ui
            M  src/MainActivity.java
             M res/layout/activity_main.xml
            ?? new_file.txt
        """.trimIndent()

        val status = parser.parseStatus(porcelain)

        assertEquals("feature/new-ui", status.branch)
        assertFalse(status.clean)
        assertEquals(1, status.stagedCount)
        assertEquals(1, status.unstagedCount)
        assertEquals(1, status.untrackedCount)
    }

    @Test
    fun parseLog_parsesCommits() {
        val logOutput = """
            a1b2c3d4e5f6|a1b2c3d|Agent|1731900000|Initial commit
            b2c3d4e5f6a1|b2c3d4e|Agent|1731900500|Added layout view
        """.trimIndent()

        val commits = parser.parseLog(logOutput)
        assertEquals(2, commits.size)
        assertEquals("a1b2c3d", commits[0].shortHash)
        assertEquals("Initial commit", commits[0].message)
        assertEquals(1731900000000L, commits[0].timestamp)
        assertEquals("Added layout view", commits[1].message)
    }
}
