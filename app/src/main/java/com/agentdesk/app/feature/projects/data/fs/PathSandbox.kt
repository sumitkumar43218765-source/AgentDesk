package com.agentdesk.app.feature.projects.data.fs

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PathSandbox @Inject constructor() {

    fun resolve(projectRoot: File, relativePath: String): AppResult<File> {
        val trimmed = relativePath.trim()

        if (trimmed.isEmpty()) {
            return AppResult.Success(projectRoot)
        }

        // Check for path traversal tokens
        val segments = trimmed.split('/', '\\')
        if (segments.any { it == ".." || it == "." }) {
            return AppResult.Failure(
                AppError.Permission(
                    message = "Path traversal rejected: path contains '.' or '..' segment ($relativePath)"
                )
            )
        }

        if (trimmed.startsWith("/") || trimmed.startsWith("\\")) {
            return AppResult.Failure(
                AppError.Permission(
                    message = "Absolute path escape rejected: $relativePath"
                )
            )
        }

        val resolved = File(projectRoot, trimmed)

        // Symlink escape check
        return try {
            val canonicalRoot = projectRoot.canonicalPath
            val canonicalTarget = resolved.canonicalPath

            val isInside = canonicalTarget == canonicalRoot ||
                canonicalTarget.startsWith(canonicalRoot + File.separator)

            if (isInside) {
                AppResult.Success(resolved)
            } else {
                AppResult.Failure(
                    AppError.Permission(
                        message = "Path escapes sandbox via symlink: $relativePath"
                    )
                )
            }
        } catch (e: Exception) {
            AppResult.Failure(
                AppError.Permission(
                    message = "Failed to verify canonical path: ${e.message}"
                )
            )
        }
    }
}
