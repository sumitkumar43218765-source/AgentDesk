package com.agentdesk.app.feature.projects.domain.model

data class FileContent(
    val relativePath: String,
    val content: String,
    val totalBytes: Long,
    val isTruncated: Boolean = false
)
