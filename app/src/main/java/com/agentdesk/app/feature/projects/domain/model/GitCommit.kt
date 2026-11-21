package com.agentdesk.app.feature.projects.domain.model

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val timestamp: Long
)
