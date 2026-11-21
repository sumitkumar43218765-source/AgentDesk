package com.agentdesk.app.feature.projects

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.data.fs.PathSandbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathSandboxTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sandbox: PathSandbox
    private lateinit var projectRoot: File

    @Before
    fun setUp() {
        sandbox = PathSandbox()
        projectRoot = tempFolder.newFolder("my-project")
    }

    @Test
    fun resolve_validRelativePath_resolvesInsideRoot() {
        val res = sandbox.resolve(projectRoot, "src/MainActivity.java")
        assertTrue(res is AppResult.Success)
        val file = (res as AppResult.Success).value
        assertEquals(File(projectRoot, "src/MainActivity.java").canonicalPath, file.canonicalPath)
    }

    @Test
    fun resolve_emptyPath_returnsProjectRoot() {
        val res = sandbox.resolve(projectRoot, "")
        assertTrue(res is AppResult.Success)
        assertEquals(projectRoot.canonicalPath, (res as AppResult.Success).value.canonicalPath)
    }

    @Test
    fun resolve_parentDirectoryTraversal_isRejected() {
        val res = sandbox.resolve(projectRoot, "../secret.txt")
        assertTrue(res is AppResult.Failure)
    }

    @Test
    fun resolve_nestedParentTraversal_isRejected() {
        val res = sandbox.resolve(projectRoot, "src/../../outside.txt")
        assertTrue(res is AppResult.Failure)
    }

    @Test
    fun resolve_absolutePath_isRejected() {
        val res = sandbox.resolve(projectRoot, "/etc/passwd")
        assertTrue(res is AppResult.Failure)
    }
}
