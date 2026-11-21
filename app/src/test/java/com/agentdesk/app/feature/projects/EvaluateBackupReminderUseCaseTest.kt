package com.agentdesk.app.feature.projects

import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.projects.domain.usecase.EvaluateBackupReminderUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class EvaluateBackupReminderUseCaseTest {

    private lateinit var useCase: EvaluateBackupReminderUseCase

    private fun createProject(
        checkpointCount: Int = 0,
        createdAt: Long = 1000L,
        lastBackupAt: Long? = null
    ): Project {
        return Project(
            id = "test-proj",
            name = "Test",
            slug = "test",
            template = TemplateId.HELLO_JAVA,
            packageName = "com.example.test",
            createdAt = createdAt,
            lastModifiedAt = createdAt,
            lastBackupAt = lastBackupAt,
            checkpointCount = checkpointCount
        )
    }

    @Before
    fun setUp() {
        useCase = EvaluateBackupReminderUseCase()
    }

    @Test
    fun invoke_whenCheckpointsReachThreshold_returnsTrue() {
        val project = createProject(checkpointCount = 10)
        assertTrue(useCase(project, now = 2000L))
    }

    @Test
    fun invoke_whenSevenDaysPassedSinceBackup_returnsTrue() {
        val sevenDaysMs = TimeUnit.DAYS.toMillis(7)
        val project = createProject(checkpointCount = 2, createdAt = 1000L, lastBackupAt = 1000L)
        assertTrue(useCase(project, now = 1000L + sevenDaysMs + 1000L))
    }

    @Test
    fun invoke_whenFreshBackupAndFewCheckpoints_returnsFalse() {
        val project = createProject(checkpointCount = 3, createdAt = 1000L, lastBackupAt = 2000L)
        assertFalse(useCase(project, now = 3000L))
    }
}
