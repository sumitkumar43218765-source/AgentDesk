package com.agentdesk.app.feature.setup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_install_states")
data class ToolInstallStateEntity(
    @PrimaryKey
    val toolId: String,
    val installedVersion: String? = null,
    val state: String,
    val lastVerifiedAt: Long? = null,
    val sizeBytes: Long = 0L,
    val lastError: String? = null
)
