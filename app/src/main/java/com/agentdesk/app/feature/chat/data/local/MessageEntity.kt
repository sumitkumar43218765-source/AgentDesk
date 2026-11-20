package com.agentdesk.app.feature.chat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "USER" or "ASSISTANT"
    val blocksJson: String,
    val status: String, // "COMPLETE", "STREAMING", "INTERRUPTED", "ERROR"
    val usageJson: String?,
    val createdAt: Long
)
