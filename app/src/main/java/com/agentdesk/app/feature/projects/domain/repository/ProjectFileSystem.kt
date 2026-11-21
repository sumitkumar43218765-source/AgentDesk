package com.agentdesk.app.feature.projects.domain.repository

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.FileContent
import com.agentdesk.app.feature.projects.domain.model.FileEntry
import java.io.File

interface ProjectFileSystem {
    fun root(projectId: String): File
    fun resolve(projectId: String, relativePath: String): AppResult<File>
    suspend fun list(projectId: String, relativeDir: String = "", depth: Int = 1): AppResult<List<FileEntry>>
    suspend fun read(projectId: String, relativePath: String, maxBytes: Int = 262_144): AppResult<FileContent>
    suspend fun write(projectId: String, relativePath: String, content: String): AppResult<Unit>
    suspend fun delete(projectId: String, relativePath: String): AppResult<Unit>
    fun guestPath(projectId: String): String
}
