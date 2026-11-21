package com.agentdesk.app.feature.settings

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.data.local.BuildRunDao
import com.agentdesk.app.feature.build.data.local.BuildRunEntity
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.Conversation
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import com.agentdesk.app.feature.projects.domain.model.ExportTarget
import com.agentdesk.app.feature.projects.domain.model.ImportSource
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.model.ProjectType
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import com.agentdesk.app.feature.settings.domain.usecase.WipeWorkspaceUseCase
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WipeWorkspaceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIncorrectConfirmationFails() = runBlocking {
        val projects = mutableListOf<String>()
        val fakeProjectRepo = object : ProjectRepository {
            override fun observeProjects(): Flow<List<Project>> = flowOf(emptyList())
            override suspend fun create(name: String, template: TemplateId, packageName: String?) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun import(source: ImportSource) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun export(projectId: String, target: ExportTarget, options: ExportOptions) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun rename(projectId: String, newName: String) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun duplicate(projectId: String) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun delete(projectId: String): AppResult<Unit> {
                projects.remove(projectId)
                return AppResult.Success(Unit)
            }
            override suspend fun get(projectId: String): Project? = null
        }

        val fakeChatRepo = object : ChatRepository {
            override fun observeConversations(): Flow<List<Conversation>> = flowOf(emptyList())
            override suspend fun getConversation(id: String) = null
            override suspend fun saveConversation(conversation: Conversation) {}
            override suspend fun updateConversation(conversation: Conversation) {}
            override suspend fun deleteConversation(id: String) {}
            override fun observeMessages(conversationId: String) = flowOf(emptyList<ChatMessage>())
            override suspend fun getMessage(id: String) = null
            override suspend fun saveMessage(message: ChatMessage) {}
            override suspend fun updateMessage(message: ChatMessage) {}
            override suspend fun deleteMessage(id: String) {}
            override suspend fun recoverInterruptedMessages() {}
        }

        val fakeBuildRunDao = object : BuildRunDao {
            override fun observeByProject(projectId: String) = flowOf(emptyList<BuildRunEntity>())
            override suspend fun getByProject(projectId: String) = emptyList<BuildRunEntity>()
            override suspend fun getById(id: String) = null
            override suspend fun getAllOldestFirst() = emptyList<BuildRunEntity>()
            override suspend fun upsert(entity: BuildRunEntity) {}
            override suspend fun deleteById(id: String) {}
            override suspend fun deleteByIds(ids: List<String>) {}
            override suspend fun clearAll() {}
        }

        val dummyPathProvider = object : PathProvider {
            override val termuxPrefix = tempFolder.newFolder("usr")
            override val termuxHome = tempFolder.newFolder("home")
            override val tmpDir = tempFolder.newFolder("tmp")
            override val rootfsDir = tempFolder.newFolder("rootfs")
            override val rootfsHome = File(rootfsDir, "root")
            override val projectsRoot = tempFolder.newFolder("projects")
            override val setupStateDir = tempFolder.newFolder("setup")
            override val logsDir = tempFolder.newFolder("logs")
            override val buildLogsDir = tempFolder.newFolder("build_logs")
            override fun hostToGuest(host: File) = null
            override fun guestToHost(guest: String) = File(guest)
        }

        val useCase = WipeWorkspaceUseCase(fakeProjectRepo, fakeChatRepo, fakeBuildRunDao, dummyPathProvider)

        val result1 = useCase("wipe")
        assertTrue(result1 is AppResult.Failure)

        val result2 = useCase("wipe workspace")
        assertTrue(result2 is AppResult.Failure)

        val result3 = useCase("")
        assertTrue(result3 is AppResult.Failure)
    }

    @Test
    fun testSuccessfulWipeLeavesToolchainIntact() = runBlocking {
        val deletedProjects = mutableListOf<String>()
        val project1 = Project(
            id = "proj-1",
            name = "Project 1",
            slug = "project-1",
            packageName = "com.test.p1",
            type = ProjectType.STANDARD_EMPTY,
            createdAt = 1000L,
            lastModifiedAt = 2000L,
            gitAuthorName = "User",
            gitAuthorEmail = "user@test.com",
            buildCount = 1,
            lastSuccessfulBuildAt = null
        )

        val fakeProjectRepo = object : ProjectRepository {
            override fun observeProjects(): Flow<List<Project>> = flowOf(listOf(project1))
            override suspend fun create(name: String, template: TemplateId, packageName: String?) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun import(source: ImportSource) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun export(projectId: String, target: ExportTarget, options: ExportOptions) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun rename(projectId: String, newName: String) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun duplicate(projectId: String) = AppResult.Failure(com.agentdesk.app.core.common.AppError.Validation("err"))
            override suspend fun delete(projectId: String): AppResult<Unit> {
                deletedProjects.add(projectId)
                return AppResult.Success(Unit)
            }
            override suspend fun get(projectId: String) = project1
        }

        val deletedConversations = mutableListOf<String>()
        val conv1 = Conversation(
            id = "conv-1",
            projectId = "proj-1",
            title = "Test Chat",
            createdAt = 1000L,
            updatedAt = 2000L,
            activeRunId = null
        )

        val fakeChatRepo = object : ChatRepository {
            override fun observeConversations(): Flow<List<Conversation>> = flowOf(listOf(conv1))
            override suspend fun getConversation(id: String) = conv1
            override suspend fun saveConversation(conversation: Conversation) {}
            override suspend fun updateConversation(conversation: Conversation) {}
            override suspend fun deleteConversation(id: String) {
                deletedConversations.add(id)
            }
            override fun observeMessages(conversationId: String) = flowOf(emptyList<ChatMessage>())
            override suspend fun getMessage(id: String) = null
            override suspend fun saveMessage(message: ChatMessage) {}
            override suspend fun updateMessage(message: ChatMessage) {}
            override suspend fun deleteMessage(id: String) {}
            override suspend fun recoverInterruptedMessages() {}
        }

        var buildRunsCleared = false
        val fakeBuildRunDao = object : BuildRunDao {
            override fun observeByProject(projectId: String) = flowOf(emptyList<BuildRunEntity>())
            override suspend fun getByProject(projectId: String) = emptyList<BuildRunEntity>()
            override suspend fun getById(id: String) = null
            override suspend fun getAllOldestFirst() = emptyList<BuildRunEntity>()
            override suspend fun upsert(entity: BuildRunEntity) {}
            override suspend fun deleteById(id: String) {}
            override suspend fun deleteByIds(ids: List<String>) {}
            override suspend fun clearAll() {
                buildRunsCleared = true
            }
        }

        val rootfsDir = tempFolder.newFolder("rootfs")
        val toolsMarker = File(rootfsDir, "jdk17-installed")
        toolsMarker.writeText("ok")

        val projectsDir = tempFolder.newFolder("projects")
        val projectFile = File(projectsDir, "app.java")
        projectFile.writeText("class App {}")

        val buildLogsDir = tempFolder.newFolder("build_logs")
        val buildLog = File(buildLogsDir, "build_1.log")
        buildLog.writeText("build logs")

        val dummyPathProvider = object : PathProvider {
            override val termuxPrefix = tempFolder.newFolder("usr")
            override val termuxHome = tempFolder.newFolder("home")
            override val tmpDir = tempFolder.newFolder("tmp")
            override val rootfsDir = rootfsDir
            override val rootfsHome = File(rootfsDir, "root")
            override val projectsRoot = projectsDir
            override val setupStateDir = tempFolder.newFolder("setup")
            override val logsDir = tempFolder.newFolder("logs")
            override val buildLogsDir = buildLogsDir
            override fun hostToGuest(host: File) = null
            override fun guestToHost(guest: String) = File(guest)
        }

        val useCase = WipeWorkspaceUseCase(fakeProjectRepo, fakeChatRepo, fakeBuildRunDao, dummyPathProvider)

        val result = useCase("WIPE WORKSPACE")
        assertTrue(result is AppResult.Success)

        // Verify entities and directories were cleared
        assertEquals(listOf("proj-1"), deletedProjects)
        assertEquals(listOf("conv-1"), deletedConversations)
        assertTrue(buildRunsCleared)
        assertFalse(projectFile.exists())
        assertFalse(buildLog.exists())

        // Toolchain remains intact
        assertTrue(toolsMarker.exists())
    }
}
