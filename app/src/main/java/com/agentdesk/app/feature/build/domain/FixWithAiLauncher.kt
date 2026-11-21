package com.agentdesk.app.feature.build.domain

interface FixWithAiLauncher {
    fun launch(projectId: String, buildId: String)
}
