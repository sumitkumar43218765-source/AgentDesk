package com.agentdesk.app.feature.projects.domain.model

data class Project(
    val id: String,
    val name: String,
    val slug: String,
    val template: TemplateId,
    val packageName: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastBackupAt: Long? = null,
    val checkpointCount: Int = 0,
    val sizeBytes: Long = 0L,
    val lastBuildStatus: String? = null
)
