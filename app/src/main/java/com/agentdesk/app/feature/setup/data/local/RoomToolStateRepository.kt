package com.agentdesk.app.feature.setup.data.local

import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolState
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomToolStateRepository @Inject constructor(
    private val dao: ToolInstallStateDao
) : ToolStateRepository {

    override fun observeToolStates(): Flow<List<ToolState>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getToolState(toolId: String): ToolState? {
        return dao.getById(toolId)?.toDomain()
    }

    override suspend fun updateToolState(state: ToolState) {
        dao.upsert(state.toEntity())
    }

    override suspend fun setToolStatus(toolId: String, status: ToolInstallStatus, error: String?) {
        val existing = dao.getById(toolId)
        val updated = ToolInstallStateEntity(
            toolId = toolId,
            installedVersion = existing?.installedVersion,
            state = status.name,
            lastVerifiedAt = if (status == ToolInstallStatus.INSTALLED) System.currentTimeMillis() else existing?.lastVerifiedAt,
            sizeBytes = existing?.sizeBytes ?: 0L,
            lastError = error
        )
        dao.upsert(updated)
    }

    override suspend fun getAllStates(): List<ToolState> {
        return dao.getAll().map { it.toDomain() }
    }

    private fun ToolInstallStateEntity.toDomain(): ToolState {
        val parsedStatus = try {
            ToolInstallStatus.valueOf(state)
        } catch (_: Exception) {
            ToolInstallStatus.NOT_INSTALLED
        }
        return ToolState(
            toolId = toolId,
            installedVersion = installedVersion,
            status = parsedStatus,
            lastVerifiedAt = lastVerifiedAt,
            sizeBytes = sizeBytes,
            lastError = lastError
        )
    }

    private fun ToolState.toEntity(): ToolInstallStateEntity {
        return ToolInstallStateEntity(
            toolId = toolId,
            installedVersion = installedVersion,
            state = status.name,
            lastVerifiedAt = lastVerifiedAt,
            sizeBytes = sizeBytes,
            lastError = lastError
        )
    }
}
