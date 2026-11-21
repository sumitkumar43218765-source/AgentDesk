package com.agentdesk.app.feature.projects

import com.agentdesk.app.feature.projects.data.fs.ZipSlipGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZipSlipGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var guard: ZipSlipGuard
    private lateinit var destDir: File

    @Before
    fun setUp() {
        guard = ZipSlipGuard()
        destDir = tempFolder.newFolder("dest")
    }

    @Test
    fun isValidEntry_validPath_returnsTrue() {
        assertTrue(guard.isValidEntry(destDir, "src/com/example/Main.java"))
        assertTrue(guard.isValidEntry(destDir, "res/layout/activity_main.xml"))
    }

    @Test
    fun isValidEntry_zipSlipTraversal_returnsFalse() {
        assertFalse(guard.isValidEntry(destDir, "../evil.sh"))
        assertFalse(guard.isValidEntry(destDir, "dir/../../evil.sh"))
        assertFalse(guard.isValidEntry(destDir, "/etc/passwd"))
    }
}
