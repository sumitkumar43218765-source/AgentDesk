package com.agentdesk.app.feature.agent.data.runtime

import com.agentdesk.app.feature.agent.data.local.AgentRunDao
import com.agentdesk.app.feature.agent.data.local.AgentRunEntity
import com.agentdesk.app.feature.agent.domain.model.AgentRun
import com.agentdesk.app.feature.agent.domain.model.RunStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunStateStore @Inject constructor(
    private val agentRunDao: AgentRunDao
) {

    suspend fun saveRun(run: AgentRun) {
        agentRunDao.upsert(AgentRunEntity.fromDomain(run))
    }

    suspend fun updateStep(runId: String, stepCount: Int, additionalTokens: Int = 0) {
        val existing = agentRunDao.getById(runId) ?: return
        val updated = existing.copy(
            stepCount = stepCount,
            lastEventAt = System.currentTimeMillis(),
            tokensUsed = existing.tokensUsed + additionalTokens
        )
        agentRunDao.upsert(updated)
    }

    suspend fun updateStatus(runId: String, status: RunStatus) {
        val existing = agentRunDao.getById(runId) ?: return
        val updated = existing.copy(
            status = status.name,
            lastEventAt = System.currentTimeMillis()
        )
        agentRunDao.upsert(updated)
    }

    suspend fun getRun(runId: String): AgentRun? {
        return agentRunDao.getById(runId)?.toDomain()
    }

    suspend fun getLatestRun(conversationId: String): AgentRun? {
        return agentRunDao.getLatestByConversation(conversationId)?.toDomain()
    }

    fun observeLatestRun(conversationId: String): Flow<AgentRun?> {
        return agentRunDao.observeLatestByConversation(conversationId).map { it?.toDomain() }
    }

    suspend fun markInterruptedOnStartup() {
        agentRunDao.markInterruptedOnStartup()
    }
}
