package com.agentdesk.app.feature.projects.data.fs

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZipImporter @Inject constructor(
    private val zipSlipGuard: ZipSlipGuard
) {

    fun unzip(inputStream: InputStream, targetDir: File): AppResult<Unit> {
        targetDir.mkdirs()

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!zipSlipGuard.isValidEntry(targetDir, entry.name)) {
                        return AppResult.Failure(
                            AppError.Permission(
                                message = "Malicious zip entry detected (zip slip): ${entry.name}"
                            )
                        )
                    }

                    val outFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            return AppResult.Success(Unit)
        } catch (e: Exception) {
            return AppResult.Failure(
                AppError.Storage(
                    cause = e,
                    message = "Failed to import zip: ${e.message}"
                )
            )
        }
    }
}
