package com.agentdesk.app.feature.settings.data

import android.content.Context
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.logging.LogRedactor
import com.agentdesk.app.feature.settings.domain.model.DiagnosticsBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsZipWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRedactor: LogRedactor
) {
    fun writeZip(bundle: DiagnosticsBundle): AppResult<File> {
        val outFile = File(context.cacheDir, "agentdesk-diagnostics-${bundle.timestamp}.zip")
        return writeZipTo(bundle, outFile)
    }

    fun writeZipTo(bundle: DiagnosticsBundle, outFile: File): AppResult<File> {
        return try {
            outFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outFile)).use { zos ->
                // 1. Write environment summary
                zos.putNextEntry(ZipEntry("environment_summary.txt"))
                zos.write(bundle.environmentSummary.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Write redacted log files
                for (logPath in bundle.logFiles) {
                    val file = File(logPath)
                    if (file.exists() && file.isFile) {
                        val text = file.readText(Charsets.UTF_8)
                        val redacted = logRedactor.redact(text)
                        zos.putNextEntry(ZipEntry("logs/${file.name}"))
                        zos.write(redacted.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
            }
            AppResult.Success(outFile)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage("Failed to generate diagnostics zip: ${e.message}"))
        }
    }
}
