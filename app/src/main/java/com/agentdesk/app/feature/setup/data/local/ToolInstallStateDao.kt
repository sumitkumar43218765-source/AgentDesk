package com.agentdesk.app.feature.setup.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolInstallStateDao {

    @Query("SELECT * FROM tool_install_states")
    fun observeAll(): Flow<List<ToolInstallStateEntity>>

    @Query("SELECT * FROM tool_install_states")
    suspend fun getAll(): List<ToolInstallStateEntity>

    @Query("SELECT * FROM tool_install_states WHERE toolId = :toolId")
    suspend fun getById(toolId: String): ToolInstallStateEntity?

    @Upsert
    suspend fun upsert(entity: ToolInstallStateEntity)

    @Query("DELETE FROM tool_install_states WHERE toolId = :toolId")
    suspend fun delete(toolId: String)
}
