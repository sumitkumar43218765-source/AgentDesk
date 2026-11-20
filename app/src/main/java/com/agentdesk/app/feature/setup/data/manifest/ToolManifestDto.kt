package com.agentdesk.app.feature.setup.data.manifest

import kotlinx.serialization.Serializable

@Serializable
data class ToolManifestDto(
    val schemaVersion: Int,
    val tools: List<ToolItemDto>
)
