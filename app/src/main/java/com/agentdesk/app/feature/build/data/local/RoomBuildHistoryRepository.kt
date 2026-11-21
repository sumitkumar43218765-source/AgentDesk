package com.agentdesk.app.feature.build.data.local

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.LogSlice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBuildHistoryRepository @Inject constructor(
    private val buildRunDao: BuildRunDao,
    private val buildLogStore: BuildLogStore,
    private val retentionPolicy: BuildRetentionPolicy
) {
    fun observeHistory(projectId: String): Flow<List<BuildRun>> {
        return buildRunDao.observeByProject(projectId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getRun(buildId: String): BuildRun? {
        return buildRunDao.getById(buildId)?.toDomain()
    }

    suspend fun saveRun(run: BuildRun) {
        buildRunDao.upsert(BuildRunEntity.fromDomain(run))
        retentionPolicy.enforceRetention(run.projectId)
    }

    suspend fun deleteRun(buildId: String) {
        buildLogStore.delete(buildId)
        buildRunDao.deleteById(buildId)
    }

    fun readLog(buildId: String, offset: Int, limit: Int): AppResult<LogSlice> {
        return buildLogStore.readSlice(buildId, offset, limit)
    }
}
