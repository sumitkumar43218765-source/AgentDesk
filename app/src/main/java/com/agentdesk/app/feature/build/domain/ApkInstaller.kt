package com.agentdesk.app.feature.build.domain

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.model.InstallOutcome
import java.io.File

interface ApkInstaller {
    suspend fun install(apk: File): AppResult<InstallOutcome>
}
