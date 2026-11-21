package com.agentdesk.app.feature.settings.domain.usecase

import com.agentdesk.app.feature.agent.data.local.PermissionGrantDao
import com.agentdesk.app.feature.agent.domain.model.PermissionGrant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RevokeGrantUseCase @Inject constructor(
    private val grantDao: PermissionGrantDao
) {
    fun observeAll(): Flow<List<PermissionGrant>> {
        return grantDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    suspend fun revoke(id: String) {
        grantDao.deleteById(id)
    }

    suspend fun clearForProject(projectId: String) {
        grantDao.clearForProject(projectId)
    }

    suspend fun clearAll() {
        grantDao.clearAll()
    }
}
