package com.agentdesk.app.feature.agent.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agentdesk.app.feature.agent.domain.model.AgentRun
import com.agentdesk.app.feature.agent.domain.model.RunMode
import com.agentdesk.app.feature.agent.domain.model.RunStatus

@Entity(
    tableName = "agent_runs",
    indices = [
        Index("conversation_id"),
        Index("project_id"),
        Index("started_at")
    ]
)
data class AgentRunEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    val status: String,
    val mode: String,
    @ColumnInfo(name = "step_count")
    val stepCount: Int,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "last_event_at")
    val lastEventAt: Long,
    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int
) {
    fun toDomain(): AgentRun = AgentRun(
        id = id,
        conversationId = conversationId,
        projectId = projectId,
        status = runCatching { RunStatus.valueOf(status) }.getOrDefault(RunStatus.IDLE),
        mode = runCatching { RunMode.valueOf(mode) }.getOrDefault(RunMode.ASK),
        stepCount = stepCount,
        startedAt = startedAt,
        lastEventAt = lastEventAt,
        totalTokensUsed = tokensUsed
    )

    companion object {
        fun fromDomain(run: AgentRun): AgentRunEntity = AgentRunEntity(
            id = run.id,
            conversationId = run.conversationId,
            projectId = run.projectId,
            status = run.status.name,
            mode = run.mode.name,
            stepCount = run.stepCount,
            startedAt = run.startedAt,
            lastEventAt = run.lastEventAt,
            tokensUsed = run.totalTokensUsed
        )
    }
}
