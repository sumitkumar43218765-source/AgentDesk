package com.agentdesk.app.feature.build.domain.usecase

import android.net.Uri
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.ApkExporter
import com.agentdesk.app.feature.build.domain.model.BuildRun
import javax.inject.Inject

class ExportApkUseCase @Inject constructor(
    private val apkExporter: ApkExporter
) {
    suspend operator fun invoke(run: BuildRun): AppResult<Uri> {
        return apkExporter.exportToDownloads(run)
    }
}
