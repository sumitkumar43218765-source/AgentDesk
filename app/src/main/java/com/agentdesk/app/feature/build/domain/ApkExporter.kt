package com.agentdesk.app.feature.build.domain

import android.net.Uri
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.model.BuildRun

interface ApkExporter {
    suspend fun exportToDownloads(run: BuildRun): AppResult<Uri>
}
