package com.agentdesk.app.feature.projects.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val slug: String,
    val template: String,
    val packageName: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastBackupAt: Long? = null,
    val checkpointCount: Int = 0,
    val sizeBytes: Long = 0L,
    val lastBuildStatus: String? = null
)
