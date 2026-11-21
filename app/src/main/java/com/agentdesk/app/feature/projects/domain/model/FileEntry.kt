package com.agentdesk.app.feature.projects.domain.model

data class FileEntry(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModifiedAt: Long = 0L
)
