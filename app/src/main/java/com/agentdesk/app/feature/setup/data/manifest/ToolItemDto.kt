package com.agentdesk.app.feature.setup.data.manifest

import kotlinx.serialization.Serializable

@Serializable
data class ToolItemDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val required: Boolean = false,
    val dependsOn: List<String> = emptyList(),
    val sizeEstimateMb: Long = 0L,
    val environment: String = "PROOT_UBUNTU",
    val source: ToolSourceDto,
    val install: List<String> = emptyList(),
    val verify: String,
    val version: String
)
