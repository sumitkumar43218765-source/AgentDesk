package com.agentdesk.app.feature.build.domain

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.model.BuildEvent
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.LogSlice
import kotlinx.coroutines.flow.Flow

interface BuildEngine {
    fun build(projectId: String, variant: BuildVariant = BuildVariant.DEBUG): Flow<BuildEvent>
    suspend fun cancel(buildId: String)
    fun observeHistory(projectId: String): Flow<List<BuildRun>>
    suspend fun getRun(buildId: String): BuildRun?
    suspend fun readLog(buildId: String, offset: Int, limit: Int): AppResult<LogSlice>
}
