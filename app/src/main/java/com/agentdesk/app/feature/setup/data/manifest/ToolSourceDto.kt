package com.agentdesk.app.feature.setup.data.manifest

import kotlinx.serialization.Serializable

@Serializable
data class ToolSourceDto(
    val kind: String,
    val packages: List<String> = emptyList(),
    val url: String? = null
)
