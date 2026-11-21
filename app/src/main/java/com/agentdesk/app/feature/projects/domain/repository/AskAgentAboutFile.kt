package com.agentdesk.app.feature.projects.domain.repository

interface AskAgentAboutFile {
    fun ask(projectId: String, relativePath: String, lineRange: IntRange? = null)
}
