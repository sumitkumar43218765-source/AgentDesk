package com.agentdesk.app.feature.projects.data.fs

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZipExporter @Inject constructor() {

    fun exportToStream(projectDir: File, outputStream: OutputStream, options: ExportOptions): AppResult<Unit> {
        try {
            ZipOutputStream(outputStream).use { zos ->
                val projectPath = projectDir.canonicalPath

                projectDir.walkTopDown().forEach { file ->
                    val relativePath = file.canonicalPath.removePrefix(projectPath).trimStart(File.separatorChar)

                    if (relativePath.isNotEmpty()) {
                        val shouldExclude = (options.excludeBuild && (relativePath.startsWith("build") || relativePath.contains("/build/"))) ||
                            (options.excludeGit && (relativePath.startsWith(".git") || relativePath.contains("/.git/")))

                        if (!shouldExclude) {
                            val entryName = if (file.isDirectory) "$relativePath/" else relativePath
                            val entry = ZipEntry(entryName)
                            entry.time = file.lastModified()
                            zos.putNextEntry(entry)

                            if (file.isFile) {
                                FileInputStream(file).use { fis ->
                                    fis.copyTo(zos)
                                }
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            return AppResult.Success(Unit)
        } catch (e: Exception) {
            return AppResult.Failure(
                AppError.Storage(
                    cause = e,
                    message = "Failed to export zip: ${e.message}"
                )
            )
        }
    }

    fun exportToFile(projectDir: File, targetZipFile: File, options: ExportOptions): AppResult<Unit> {
        targetZipFile.parentFile?.mkdirs()
        return try {
            FileOutputStream(targetZipFile).use { fos ->
                exportToStream(projectDir, fos, options)
            }
        } catch (e: Exception) {
            AppResult.Failure(
                AppError.Storage(
                    cause = e,
                    message = "Failed to write export file: ${e.message}"
                )
            )
        }
    }
}
