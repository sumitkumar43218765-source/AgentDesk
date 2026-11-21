package com.agentdesk.app.feature.agent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRunDao {

    @Query("SELECT * FROM agent_runs WHERE conversation_id = :conversationId ORDER BY started_at DESC LIMIT 1")
    fun observeLatestByConversation(conversationId: String): Flow<AgentRunEntity?>

    @Query("SELECT * FROM agent_runs WHERE conversation_id = :conversationId ORDER BY started_at DESC LIMIT 1")
    suspend fun getLatestByConversation(conversationId: String): AgentRunEntity?

    @Query("SELECT * FROM agent_runs WHERE id = :id")
    suspend fun getById(id: String): AgentRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgentRunEntity)

    @Query("UPDATE agent_runs SET status = 'INTERRUPTED' WHERE status = 'RUNNING'")
    suspend fun markInterruptedOnStartup()

    @Query("DELETE FROM agent_runs WHERE id = :id")
    suspend fun deleteById(id: String)
}
