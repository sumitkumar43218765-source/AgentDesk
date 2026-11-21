package com.agentdesk.app.feature.agent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PermissionGrantDao {

    @Query("SELECT * FROM permission_grants WHERE project_id = :projectId AND tool_name = :toolName")
    suspend fun getGrantsForTool(projectId: String, toolName: String): List<PermissionGrantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PermissionGrantEntity)

    @Query("DELETE FROM permission_grants WHERE project_id = :projectId")
    suspend fun clearForProject(projectId: String)
}
