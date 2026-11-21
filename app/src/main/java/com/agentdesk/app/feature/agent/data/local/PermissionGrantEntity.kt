package com.agentdesk.app.feature.agent.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agentdesk.app.feature.agent.domain.model.PermissionGrant

@Entity(
    tableName = "permission_grants",
    indices = [
        Index("project_id"),
        Index("tool_name")
    ]
)
data class PermissionGrantEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "tool_name")
    val toolName: String,
    @ColumnInfo(name = "scope_key")
    val scopeKey: String,
    @ColumnInfo(name = "granted_at")
    val grantedAt: Long
) {
    fun toDomain(): PermissionGrant = PermissionGrant(
        id = id,
        projectId = projectId,
        toolName = toolName,
        scopeKey = scopeKey,
        grantedAt = grantedAt
    )

    companion object {
        fun fromDomain(g: PermissionGrant): PermissionGrantEntity = PermissionGrantEntity(
            id = g.id,
            projectId = g.projectId,
            toolName = g.toolName,
            scopeKey = g.scopeKey,
            grantedAt = g.grantedAt
        )
    }
}
