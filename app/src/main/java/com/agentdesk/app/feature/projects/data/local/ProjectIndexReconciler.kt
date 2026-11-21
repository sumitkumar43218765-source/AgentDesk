package com.agentdesk.app.feature.projects.data.local

import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectIndexReconciler @Inject constructor(
    private val pathProvider: PathProvider,
    private val projectDao: ProjectDao,
    private val metadataFile: ProjectMetadataFile
) {

    suspend fun reconcile() {
        val root = pathProvider.projectsRoot
        if (!root.exists()) {
            root.mkdirs()
            return
        }

        val onDiskDirs = root.listFiles { f -> f.isDirectory } ?: emptyArray()
        val foundSlugs = mutableSetOf<String>()

        for (dir in onDiskDirs) {
            val meta = metadataFile.read(dir)
            if (meta != null) {
                foundSlugs.add(meta.slug)
                val sizeBytes = calculateDirSize(dir)
                val existing = projectDao.getById(meta.id)

                val entity = ProjectEntity(
                    id = meta.id,
                    name = meta.name,
                    slug = meta.slug,
                    template = meta.template,
                    packageName = meta.packageName,
                    createdAt = meta.createdAt,
                    lastModifiedAt = dir.lastModified(),
                    lastBackupAt = meta.lastBackupAt,
                    checkpointCount = meta.checkpointCount,
                    sizeBytes = sizeBytes,
                    lastBuildStatus = existing?.lastBuildStatus
                )
                projectDao.upsert(entity)
            }
        }

        // Clean up projects in Room that no longer exist on disk
        val dbProjects = projectDao.getAll()
        for (p in dbProjects) {
            if (!foundSlugs.contains(p.slug)) {
                projectDao.delete(p.id)
            }
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }
}
