package com.agentdesk.app.feature.setup.domain.model

data class ToolSource(
    val kind: ToolSourceKind,
    val packages: List<String> = emptyList(),
    val url: String? = null
)
