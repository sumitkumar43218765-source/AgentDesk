package com.agentdesk.app.feature.build.domain

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFixWithAiLauncher @Inject constructor() : FixWithAiLauncher {
    override fun launch(projectId: String, buildId: String) {
        // Will be wired to M6 Agent runtime when M6 is implemented
    }
}
