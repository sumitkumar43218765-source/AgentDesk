package com.agentdesk.app.feature.projects.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ProjectMetadataDto(
    val id: String,
    val name: String,
    val slug: String,
    val template: String,
    val packageName: String,
    val createdAt: Long,
    val lastBackupAt: Long? = null,
    val checkpointCount: Int = 0
)

@Singleton
class ProjectMetadataFile @Inject constructor() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun read(projectDir: File): ProjectMetadataDto? {
        val metaFile = File(projectDir, ".agent/project.json")
        if (!metaFile.exists()) return null
        return try {
            json.decodeFromString<ProjectMetadataDto>(metaFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun write(projectDir: File, metadata: ProjectMetadataDto) {
        val agentDir = File(projectDir, ".agent").apply { if (!exists()) mkdirs() }
        val metaFile = File(agentDir, "project.json")
        val content = json.encodeToString(ProjectMetadataDto.serializer(), metadata)
        metaFile.writeText(content)
    }
}
