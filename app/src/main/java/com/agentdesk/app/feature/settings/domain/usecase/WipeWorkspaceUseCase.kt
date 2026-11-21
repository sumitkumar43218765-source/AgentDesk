package com.agentdesk.app.feature.settings.domain.usecase

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.data.local.BuildRunDao
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class WipeWorkspaceUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository,
    private val buildRunDao: BuildRunDao,
    private val pathProvider: PathProvider
) {
    companion object {
        const val REQUIRED_CONFIRMATION = "WIPE WORKSPACE"
    }

    suspend operator fun invoke(confirmationInput: String): AppResult<Unit> {
        if (confirmationInput.trim() != REQUIRED_CONFIRMATION) {
            return AppResult.Failure(
                AppError.Permission(
                    "Confirmation failed. Must type '$REQUIRED_CONFIRMATION' exactly."
                )
            )
        }

        // 1. Delete all projects
        val projects = projectRepository.observeProjects().firstOrNull() ?: emptyList()
        for (p in projects) {
            projectRepository.delete(p.id)
        }

        // 2. Delete all chats
        val conversations = chatRepository.observeConversations().firstOrNull() ?: emptyList()
        for (c in conversations) {
            chatRepository.deleteConversation(c.id)
        }

        // 3. Clear build runs database
        runCatching {
            buildRunDao.clearAll()
        }

        // 4. Delete build logs and projects directory contents
        runCatching {
            pathProvider.buildLogsDir.listFiles()?.forEach { it.deleteRecursively() }
            pathProvider.projectsRoot.listFiles()?.forEach { it.deleteRecursively() }
        }

        return AppResult.Success(Unit)
    }
}
