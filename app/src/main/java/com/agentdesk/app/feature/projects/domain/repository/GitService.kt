package com.agentdesk.app.feature.projects.domain.repository

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.GitCommit
import com.agentdesk.app.feature.projects.domain.model.GitStatus

interface GitService {
    suspend fun init(projectId: String): AppResult<Unit>
    suspend fun status(projectId: String): AppResult<GitStatus>
    suspend fun checkpoint(projectId: String, message: String): AppResult<GitCommit?>
    suspend fun log(projectId: String, limit: Int = 50): AppResult<List<GitCommit>>
    suspend fun diffStat(projectId: String, from: String? = null): AppResult<String>
    suspend fun restore(projectId: String, commit: String): AppResult<Unit>
    suspend fun setRemote(projectId: String, url: String?): AppResult<Unit>
    suspend fun push(projectId: String): AppResult<Unit>
}
