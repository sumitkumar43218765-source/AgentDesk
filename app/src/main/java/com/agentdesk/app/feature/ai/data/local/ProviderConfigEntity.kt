package com.agentdesk.app.feature.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey val id: String,
    val type: String, // "ANTHROPIC" or "OPENAI_COMPATIBLE"
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val extraHeadersJson: String,
    val enabled: Boolean,
    val createdAt: Long
)
