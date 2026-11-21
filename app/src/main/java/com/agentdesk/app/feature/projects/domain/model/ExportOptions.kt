package com.agentdesk.app.feature.projects.domain.model

data class ExportOptions(
    val excludeBuild: Boolean = true,
    val excludeGit: Boolean = true
)
