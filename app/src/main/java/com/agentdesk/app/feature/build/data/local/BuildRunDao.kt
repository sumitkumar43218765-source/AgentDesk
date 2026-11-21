package com.agentdesk.app.feature.build.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildRunDao {

    @Query("SELECT * FROM build_runs WHERE project_id = :projectId ORDER BY started_at DESC")
    fun observeByProject(projectId: String): Flow<List<BuildRunEntity>>

    @Query("SELECT * FROM build_runs WHERE project_id = :projectId ORDER BY started_at DESC")
    suspend fun getByProject(projectId: String): List<BuildRunEntity>

    @Query("SELECT * FROM build_runs WHERE id = :id")
    suspend fun getById(id: String): BuildRunEntity?

    @Query("SELECT * FROM build_runs ORDER BY started_at ASC")
    suspend fun getAllOldestFirst(): List<BuildRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BuildRunEntity)

    @Query("DELETE FROM build_runs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM build_runs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
