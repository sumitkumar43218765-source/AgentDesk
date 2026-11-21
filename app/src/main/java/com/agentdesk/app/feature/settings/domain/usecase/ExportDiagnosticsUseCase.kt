package com.agentdesk.app.feature.settings.domain.usecase

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.settings.data.DiagnosticsCollector
import com.agentdesk.app.feature.settings.data.DiagnosticsZipWriter
import java.io.File
import javax.inject.Inject

class ExportDiagnosticsUseCase @Inject constructor(
    private val collector: DiagnosticsCollector,
    private val zipWriter: DiagnosticsZipWriter
) {
    suspend operator fun invoke(): AppResult<File> {
        val bundle = collector.collect()
        return zipWriter.writeZip(bundle)
    }
}
