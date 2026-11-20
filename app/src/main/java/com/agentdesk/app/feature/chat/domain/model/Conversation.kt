package com.agentdesk.app.feature.chat.domain.model

data class Conversation(
    val id: String,
    val projectId: String? = null,
    val title: String,
    val providerId: String,
    val model: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
)
