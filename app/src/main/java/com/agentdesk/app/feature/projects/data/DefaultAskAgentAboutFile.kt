package com.agentdesk.app.feature.projects.data

import com.agentdesk.app.core.logging.AppLogger
import com.agentdesk.app.feature.projects.domain.repository.AskAgentAboutFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAskAgentAboutFile @Inject constructor(
    private val logger: AppLogger
) : AskAgentAboutFile {

    override fun ask(projectId: String, relativePath: String, lineRange: IntRange?) {
        logger.i("AskAgentAboutFile", "File referenced for agent: $projectId / $relativePath ($lineRange)")
    }
}
