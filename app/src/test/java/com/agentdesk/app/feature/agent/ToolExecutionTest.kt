package com.agentdesk.app.feature.agent

import com.agentdesk.app.core.common.DispatcherProvider
import com.agentdesk.app.feature.agent.data.tools.DeletePathTool
import com.agentdesk.app.feature.agent.data.tools.EditFileTool
import com.agentdesk.app.feature.agent.data.tools.ReadFileTool
import com.agentdesk.app.feature.agent.data.tools.WriteFileTool
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.projects.data.fs.DefaultProjectFileSystem
import com.agentdesk.app.feature.projects.data.fs.PathSandbox
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ToolExecutionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectDir: File
    private lateinit var fileSystem: DefaultProjectFileSystem
    private lateinit var readFileTool: ReadFileTool
    private lateinit var writeFileTool: WriteFileTool
    private lateinit var editFileTool: EditFileTool
    private lateinit var deletePathTool: DeletePathTool
    private lateinit var toolContext: ToolContext

    @Before
    fun setUp() {
        val rootDir = tempFolder.newFolder("agent_root")
        val projectsRoot = File(rootDir, "projects").apply { mkdirs() }
        projectDir = File(projectsRoot, "test-proj").apply { mkdirs() }

        val fakePathProvider = object : PathProvider {
            override val termuxPrefix: File = File(rootDir, "prefix")
            override val termuxHome: File = File(rootDir, "home")
            override val tmpDir: File = File(rootDir, "tmp")
            override val rootfsDir: File = File(rootDir, "rootfs")
            override val rootfsHome: File = File(rootDir, "rootfs_home")
            override val projectsRoot: File = projectsRoot
            override val setupStateDir: File = File(rootDir, "setup")
            override val logsDir: File = File(rootDir, "logs")
            override val buildLogsDir: File = File(rootDir, "build_logs")
            override fun hostToGuest(host: File): String = host.absolutePath
            override fun guestToHost(guest: String): File = File(guest)
        }

        val fakeDispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Unconfined
            override val io: CoroutineDispatcher = Dispatchers.Unconfined
            override val default: CoroutineDispatcher = Dispatchers.Unconfined
        }

        val sandbox = PathSandbox()
        fileSystem = DefaultProjectFileSystem(fakePathProvider, sandbox, fakeDispatchers)

        readFileTool = ReadFileTool(fileSystem)
        writeFileTool = WriteFileTool(fileSystem)
        editFileTool = EditFileTool(fileSystem)
        deletePathTool = DeletePathTool(fileSystem)

        toolContext = ToolContext(
            projectId = "test-proj",
            conversationId = "conv-1",
            runId = "run-1",
            cancelJob = Job()
        )
    }

    @Test
    fun `path traversal attempt is rejected by file tools`() = runBlocking {
        val escapeArgs = buildJsonObject {
            put("path", "../outside.txt")
        }

        val readOut = readFileTool.execute(escapeArgs, toolContext)
        assertTrue(readOut.isError)

        val writeArgs = buildJsonObject {
            put("path", "../outside.txt")
            put("content", "hacked")
        }
        val writeOut = writeFileTool.execute(writeArgs, toolContext)
        assertTrue(writeOut.isError)

        val deleteArgs = buildJsonObject {
            put("path", "../../etc/passwd")
        }
        val deleteOut = deletePathTool.execute(deleteArgs, toolContext)
        assertTrue(deleteOut.isError)
    }

    @Test
    fun `edit_file successfully performs targeted replacement`() = runBlocking {
        val initialContent = "line 1\nHello World\nline 3"
        val writeArgs = buildJsonObject {
            put("path", "hello.txt")
            put("content", initialContent)
        }
        val writeRes = writeFileTool.execute(writeArgs, toolContext)
        assertFalse(writeRes.isError)

        val editArgs = buildJsonObject {
            put("path", "hello.txt")
            put("old_str", "Hello World")
            put("new_str", "Hello Android")
        }
        val editRes = editFileTool.execute(editArgs, toolContext)
        assertFalse(editRes.isError)
        assertTrue(editRes.text.contains("+Hello Android"))

        val readArgs = buildJsonObject { put("path", "hello.txt") }
        val readRes = readFileTool.execute(readArgs, toolContext)
        assertTrue(readRes.text.contains("Hello Android"))
    }

    @Test
    fun `edit_file rejects non-existent or ambiguous targets without replace_all`() = runBlocking {
        val writeArgs = buildJsonObject {
            put("path", "repeat.txt")
            put("content", "repeat\nrepeat\nother")
        }
        writeFileTool.execute(writeArgs, toolContext)

        val notFoundArgs = buildJsonObject {
            put("path", "repeat.txt")
            put("old_str", "missing target")
            put("new_str", "new text")
        }
        val notFoundRes = editFileTool.execute(notFoundArgs, toolContext)
        assertTrue(notFoundRes.isError)
        assertTrue(notFoundRes.text.contains("Target text not found"))

        val ambiguousArgs = buildJsonObject {
            put("path", "repeat.txt")
            put("old_str", "repeat")
            put("new_str", "replaced")
        }
        val ambiguousRes = editFileTool.execute(ambiguousArgs, toolContext)
        assertTrue(ambiguousRes.isError)
        assertTrue(ambiguousRes.text.contains("multiple times"))
    }
}
