package com.agentdesk.app.feature.projects.domain.model

data class GitStatus(
    val branch: String,
    val clean: Boolean,
    val stagedCount: Int,
    val unstagedCount: Int,
    val untrackedCount: Int
)
