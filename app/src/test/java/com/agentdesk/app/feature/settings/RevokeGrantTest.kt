package com.agentdesk.app.feature.settings

import com.agentdesk.app.feature.agent.data.local.PermissionGrantDao
import com.agentdesk.app.feature.agent.data.local.PermissionGrantEntity
import com.agentdesk.app.feature.settings.domain.usecase.RevokeGrantUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RevokeGrantTest {

    @Test
    fun testRevokeGrants() = runBlocking {
        val grants = mutableListOf(
            PermissionGrantEntity(
                id = "grant-1",
                projectId = "proj-1",
                toolName = "write_file",
                scopeKey = "src/MainActivity.kt",
                grantedAt = 1000L
            ),
            PermissionGrantEntity(
                id = "grant-2",
                projectId = "proj-1",
                toolName = "shell_exec",
                scopeKey = "git status",
                grantedAt = 2000L
            ),
            PermissionGrantEntity(
                id = "grant-3",
                projectId = "proj-2",
                toolName = "write_file",
                scopeKey = "build.gradle",
                grantedAt = 3000L
            )
        )

        val fakeDao = object : PermissionGrantDao {
            override suspend fun getGrantsForTool(projectId: String, toolName: String) =
                grants.filter { it.projectId == projectId && it.toolName == toolName }

            override suspend fun insert(entity: PermissionGrantEntity) {
                grants.add(entity)
            }

            override fun observeAll(): Flow<List<PermissionGrantEntity>> = flowOf(grants.toList())

            override suspend fun deleteById(id: String) {
                grants.removeAll { it.id == id }
            }

            override suspend fun clearForProject(projectId: String) {
                grants.removeAll { it.projectId == projectId }
            }

            override suspend fun clearAll() {
                grants.clear()
            }
        }

        val useCase = RevokeGrantUseCase(fakeDao)

        // 1. Observe all
        val observed = useCase.observeAll().first()
        assertEquals(3, observed.size)
        assertEquals("grant-1", observed[0].id)

        // 2. Revoke single
        useCase.revoke("grant-1")
        assertEquals(2, grants.size)
        assertTrue(grants.none { it.id == "grant-1" })

        // 3. Clear for project
        useCase.clearForProject("proj-1")
        assertEquals(1, grants.size)
        assertEquals("grant-3", grants.first().id)

        // 4. Clear all
        useCase.clearAll()
        assertTrue(grants.isEmpty())
    }
}
