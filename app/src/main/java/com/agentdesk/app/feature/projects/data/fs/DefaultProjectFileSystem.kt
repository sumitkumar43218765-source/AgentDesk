package com.agentdesk.app.feature.projects.data.fs

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.common.DispatcherProvider
import com.agentdesk.app.feature.projects.domain.model.FileContent
import com.agentdesk.app.feature.projects.domain.model.FileEntry
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProjectFileSystem @Inject constructor(
    private val pathProvider: PathProvider,
    private val sandbox: PathSandbox,
    private val dispatchers: DispatcherProvider
) : ProjectFileSystem {

    override fun root(projectId: String): File {
        return File(pathProvider.projectsRoot, projectId)
    }

    override fun resolve(projectId: String, relativePath: String): AppResult<File> {
        val rootDir = root(projectId)
        return sandbox.resolve(rootDir, relativePath)
    }

    override suspend fun list(
        projectId: String,
        relativeDir: String,
        depth: Int
    ): AppResult<List<FileEntry>> = withContext(dispatchers.io) {
        val resolveRes = resolve(projectId, relativeDir)
        if (resolveRes is AppResult.Failure) return@withContext resolveRes

        val targetDir = (resolveRes as AppResult.Success).value
        if (!targetDir.exists()) {
            return@withContext AppResult.Failure(AppError.Storage("Directory does not exist: $relativeDir"))
        }

        val rootDir = root(projectId)
        val entries = mutableListOf<FileEntry>()

        val files = targetDir.listFiles() ?: emptyArray()
        for (f in files) {
            val relPath = f.canonicalPath.removePrefix(rootDir.canonicalPath).trimStart(File.separatorChar)
            entries.add(
                FileEntry(
                    name = f.name,
                    relativePath = relPath,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isFile) f.length() else 0L,
                    lastModifiedAt = f.lastModified()
                )
            )
        }

        // Sort: directories first, then alphabetical
        entries.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        AppResult.Success(entries)
    }

    override suspend fun read(
        projectId: String,
        relativePath: String,
        maxBytes: Int
    ): AppResult<FileContent> = withContext(dispatchers.io) {
        val resolveRes = resolve(projectId, relativePath)
        if (resolveRes is AppResult.Failure) return@withContext resolveRes

        val file = (resolveRes as AppResult.Success).value
        if (!file.exists() || !file.isFile) {
            return@withContext AppResult.Failure(AppError.Storage("File not found: $relativePath"))
        }

        try {
            val totalBytes = file.length()
            val isTruncated = totalBytes > maxBytes
            val bytesToRead = if (isTruncated) maxBytes else totalBytes.toInt()

            val bytes = ByteArray(bytesToRead)
            FileInputStream(file).use { fis ->
                var read = 0
                while (read < bytesToRead) {
                    val count = fis.read(bytes, read, bytesToRead - read)
                    if (count == -1) break
                    read += count
                }
            }

            val text = String(bytes, 0, bytesToRead, Charsets.UTF_8)
            AppResult.Success(
                FileContent(
                    relativePath = relativePath,
                    content = text,
                    totalBytes = totalBytes,
                    isTruncated = isTruncated
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage("Failed to read file: ${e.message}", e))
        }
    }

    override suspend fun write(
        projectId: String,
        relativePath: String,
        content: String
    ): AppResult<Unit> = withContext(dispatchers.io) {
        val resolveRes = resolve(projectId, relativePath)
        if (resolveRes is AppResult.Failure) return@withContext resolveRes

        val file = (resolveRes as AppResult.Success).value
        file.parentFile?.mkdirs()

        // Atomic write via temp file + atomic rename
        val tempFile = File(file.parentFile, "${file.name}.tmp_${System.currentTimeMillis()}")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            tempFile.delete()
            AppResult.Failure(AppError.Storage("Failed to write file: ${e.message}", e))
        }
    }

    override suspend fun delete(
        projectId: String,
        relativePath: String
    ): AppResult<Unit> = withContext(dispatchers.io) {
        val resolveRes = resolve(projectId, relativePath)
        if (resolveRes is AppResult.Failure) return@withContext resolveRes

        val file = (resolveRes as AppResult.Success).value
        if (!file.exists()) {
            return@withContext AppResult.Success(Unit)
        }

        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (deleted) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(AppError.Storage("Failed to delete $relativePath"))
        }
    }

    override fun guestPath(projectId: String): String {
        return pathProvider.hostToGuest(root(projectId)) ?: "/root/projects/$projectId"
    }
}
