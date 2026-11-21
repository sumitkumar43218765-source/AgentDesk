package com.agentdesk.app.feature.build

import com.agentdesk.app.feature.build.data.local.BuildLogStore
import com.agentdesk.app.feature.build.data.local.BuildRetentionPolicy
import com.agentdesk.app.feature.build.data.local.BuildRunDao
import com.agentdesk.app.feature.build.data.local.BuildRunEntity
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BuildRetentionPolicyTest {

    private val deletedIds = mutableListOf<String>()

    private val fakeDao = object : BuildRunDao {
        val storedRuns = mutableListOf<BuildRunEntity>()

        override fun observeByProject(projectId: String): Flow<List<BuildRunEntity>> = emptyFlow()
        override suspend fun getByProject(projectId: String): List<BuildRunEntity> = storedRuns.filter { it.projectId == projectId }
        override suspend fun getById(id: String): BuildRunEntity? = storedRuns.find { it.id == id }
        override suspend fun getAllOldestFirst(): List<BuildRunEntity> = storedRuns.sortedBy { it.startedAt }
        override suspend fun upsert(entity: BuildRunEntity) { storedRuns.add(entity) }
        override suspend fun deleteById(id: String) {
            deletedIds.add(id)
            storedRuns.removeAll { it.id == id }
        }
        override suspend fun deleteByIds(ids: List<String>) {
            deletedIds.addAll(ids)
            storedRuns.removeAll { ids.contains(it.id) }
        }
        override suspend fun clearAll() {
            storedRuns.clear()
        }
    }

    private val fakePathProvider = object : PathProvider {
        private val temp = File(System.getProperty("java.io.tmpdir"), "retention_test_${System.nanoTime()}").apply { mkdirs() }
        override val termuxPrefix: File = temp
        override val termuxHome: File = temp
        override val tmpDir: File = temp
        override val rootfsDir: File = temp
        override val rootfsHome: File = temp
        override val projectsRoot: File = temp
        override val setupStateDir: File = temp
        override val logsDir: File = temp
        override val buildLogsDir: File = temp
        override fun hostToGuest(host: File): String? = null
        override fun guestToHost(guest: String): File = temp
    }

    @Test
    fun testPrunesRunsOver20PerProject() = runTest {
        deletedIds.clear()
        fakeDao.storedRuns.clear()

        val logStore = BuildLogStore(fakePathProvider)
        val policy = BuildRetentionPolicy(fakeDao, logStore)

        // Add 25 runs
        for (i in 1..25) {
            val entity = BuildRunEntity(
                id = "run_$i",
                projectId = "proj_1",
                variant = "DEBUG",
                status = "SUCCESS",
                startedAt = 1000L + i,
                finishedAt = 2000L + i,
                apkHostPath = null,
                logPath = "log_$i.log",
                failedStep = null,
                diagnosticsJson = "[]",
                stepsJson = "[]",
                gitCommit = null
            )
            fakeDao.storedRuns.add(entity)
        }

        policy.enforceRetention("proj_1")

        // 5 runs should have been deleted (keeping 20)
        assertEquals(5, deletedIds.size)
        assertEquals(20, fakeDao.storedRuns.size)
    }
}
