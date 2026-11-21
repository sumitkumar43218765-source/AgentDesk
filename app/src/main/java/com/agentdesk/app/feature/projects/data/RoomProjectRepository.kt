package com.agentdesk.app.feature.projects.data

import android.os.Environment
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.projects.data.fs.ProjectSlugger
import com.agentdesk.app.feature.projects.data.fs.ZipExporter
import com.agentdesk.app.feature.projects.data.fs.ZipImporter
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import com.agentdesk.app.feature.projects.data.local.ProjectEntity
import com.agentdesk.app.feature.projects.data.local.ProjectIndexReconciler
import com.agentdesk.app.feature.projects.data.local.ProjectMetadataDto
import com.agentdesk.app.feature.projects.data.local.ProjectMetadataFile
import com.agentdesk.app.feature.projects.data.template.TemplateRepository
import com.agentdesk.app.feature.projects.domain.model.ExportOptions
import com.agentdesk.app.feature.projects.domain.model.ExportTarget
import com.agentdesk.app.feature.projects.domain.model.ImportSource
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.projects.domain.repository.GitService
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val fileSystem: ProjectFileSystem,
    private val pathProvider: PathProvider,
    private val gitService: GitService,
    private val slugger: ProjectSlugger,
    private val templateRepository: TemplateRepository,
    private val metadataFile: ProjectMetadataFile,
    private val reconciler: ProjectIndexReconciler,
    private val zipImporter: ZipImporter,
    private val zipExporter: ZipExporter,
    private val idGenerator: IdGenerator
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> {
        return projectDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun create(
        name: String,
        template: TemplateId,
        packageName: String?
    ): AppResult<Project> {
        val existingSlugs = projectDao.getAll().map { it.slug }.toSet()
        val slug = slugger.generateSlug(name, existingSlugs)
        val projectDir = File(pathProvider.projectsRoot, slug)
        val id = idGenerator.newId()
        val pkg = packageName ?: slugger.derivePackageName(slug)
        val now = System.currentTimeMillis()

        // 1. Populate template files
        val popResult = templateRepository.populateProject(projectDir, template, pkg, name)
        if (popResult is AppResult.Failure) return popResult

        // 2. Write metadata file
        val metaDto = ProjectMetadataDto(
            id = id,
            name = name,
            slug = slug,
            template = template.name,
            packageName = pkg,
            createdAt = now
        )
        metadataFile.write(projectDir, metaDto)

        // 3. Initialize git repository
        gitService.init(slug)

        // 4. Save to Room database
        val entity = ProjectEntity(
            id = id,
            name = name,
            slug = slug,
            template = template.name,
            packageName = pkg,
            createdAt = now,
            lastModifiedAt = now,
            sizeBytes = calculateDirSize(projectDir)
        )
        projectDao.upsert(entity)

        return AppResult.Success(entity.toDomain())
    }

    override suspend fun import(source: ImportSource): AppResult<Project> {
        val existingSlugs = projectDao.getAll().map { it.slug }.toSet()
        val (suggestedName, slug) = when (source) {
            is ImportSource.ZipFile -> {
                val base = source.suggestedName.removeSuffix(".zip")
                base to slugger.generateSlug(base, existingSlugs)
            }
            is ImportSource.SharedFolder -> {
                source.suggestedName to slugger.generateSlug(source.suggestedName, existingSlugs)
            }
        }

        val projectDir = File(pathProvider.projectsRoot, slug)
        projectDir.mkdirs()

        when (source) {
            is ImportSource.ZipFile -> {
                val unzipRes = zipImporter.unzip(source.streamSupplier(), projectDir)
                if (unzipRes is AppResult.Failure) return unzipRes
            }
            is ImportSource.SharedFolder -> {
                source.folder.copyRecursively(projectDir, overwrite = true)
            }
        }

        val existingMeta = metadataFile.read(projectDir)
        val now = System.currentTimeMillis()
        val id = existingMeta?.id ?: idGenerator.newId()
        val name = existingMeta?.name ?: suggestedName
        val template = existingMeta?.template ?: TemplateId.EMPTY.name
        val pkg = existingMeta?.packageName ?: slugger.derivePackageName(slug)

        val metaDto = ProjectMetadataDto(
            id = id,
            name = name,
            slug = slug,
            template = template,
            packageName = pkg,
            createdAt = existingMeta?.createdAt ?: now
        )
        metadataFile.write(projectDir, metaDto)

        // Initialize git if .git doesn't exist
        if (!File(projectDir, ".git").exists()) {
            gitService.init(slug)
        }

        val entity = ProjectEntity(
            id = id,
            name = name,
            slug = slug,
            template = template,
            packageName = pkg,
            createdAt = metaDto.createdAt,
            lastModifiedAt = now,
            sizeBytes = calculateDirSize(projectDir)
        )
        projectDao.upsert(entity)

        return AppResult.Success(entity.toDomain())
    }

    override suspend fun export(
        projectId: String,
        target: ExportTarget,
        options: ExportOptions
    ): AppResult<String> {
        val project = projectDao.getById(projectId)
            ?: return AppResult.Failure(AppError.Unknown("Project not found: $projectId"))

        val projectDir = File(pathProvider.projectsRoot, project.slug)
        if (!projectDir.exists()) {
            return AppResult.Failure(AppError.Storage("Project directory missing: ${projectDir.absolutePath}"))
        }

        val exportResult: AppResult<String> = when (target) {
            is ExportTarget.Downloads -> {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "AgentDesk"
                ).apply { if (!exists()) mkdirs() }
                val targetFile = File(downloadsDir, target.fileName)
                val res = zipExporter.exportToFile(projectDir, targetFile, options)
                if (res is AppResult.Success) AppResult.Success(targetFile.absolutePath) else AppResult.Failure(res.errorOrNull() ?: AppError.Storage("Export failed"))
            }
            is ExportTarget.SafUri -> {
                val res = zipExporter.exportToStream(projectDir, target.streamSupplier(), options)
                if (res is AppResult.Success) AppResult.Success("SAF Export complete") else AppResult.Failure(res.errorOrNull() ?: AppError.Storage("Export failed"))
            }
        }

        if (exportResult is AppResult.Success) {
            // Update backup timestamp and reset checkpoint count
            val now = System.currentTimeMillis()
            val meta = metadataFile.read(projectDir)
            if (meta != null) {
                metadataFile.write(projectDir, meta.copy(lastBackupAt = now, checkpointCount = 0))
            }
            projectDao.upsert(project.copy(lastBackupAt = now, checkpointCount = 0))
        }

        return exportResult
    }

    override suspend fun rename(projectId: String, newName: String): AppResult<Project> {
        val existing = projectDao.getById(projectId)
            ?: return AppResult.Failure(AppError.Unknown("Project not found: $projectId"))

        val projectDir = File(pathProvider.projectsRoot, existing.slug)
        val meta = metadataFile.read(projectDir)
        if (meta != null) {
            metadataFile.write(projectDir, meta.copy(name = newName))
        }

        val updated = existing.copy(name = newName, lastModifiedAt = System.currentTimeMillis())
        projectDao.upsert(updated)
        return AppResult.Success(updated.toDomain())
    }

    override suspend fun duplicate(projectId: String): AppResult<Project> {
        val existing = projectDao.getById(projectId)
            ?: return AppResult.Failure(AppError.Unknown("Project not found: $projectId"))

        val srcDir = File(pathProvider.projectsRoot, existing.slug)
        val existingSlugs = projectDao.getAll().map { it.slug }.toSet()
        val newSlug = slugger.generateSlug("${existing.name} Copy", existingSlugs)
        val destDir = File(pathProvider.projectsRoot, newSlug)

        srcDir.copyRecursively(destDir, overwrite = true)

        val newId = idGenerator.newId()
        val now = System.currentTimeMillis()
        val newMeta = ProjectMetadataDto(
            id = newId,
            name = "${existing.name} Copy",
            slug = newSlug,
            template = existing.template,
            packageName = slugger.derivePackageName(newSlug),
            createdAt = now
        )
        metadataFile.write(destDir, newMeta)

        val newEntity = ProjectEntity(
            id = newId,
            name = newMeta.name,
            slug = newSlug,
            template = newMeta.template,
            packageName = newMeta.packageName,
            createdAt = now,
            lastModifiedAt = now,
            sizeBytes = calculateDirSize(destDir)
        )
        projectDao.upsert(newEntity)

        return AppResult.Success(newEntity.toDomain())
    }

    override suspend fun delete(projectId: String): AppResult<Unit> {
        val existing = projectDao.getById(projectId)
        if (existing != null) {
            val projectDir = File(pathProvider.projectsRoot, existing.slug)
            projectDir.deleteRecursively()
            projectDao.delete(projectId)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun get(projectId: String): Project? {
        return projectDao.getById(projectId)?.toDomain()
    }

    private fun ProjectEntity.toDomain(): Project {
        return Project(
            id = id,
            name = name,
            slug = slug,
            template = try {
                TemplateId.valueOf(template)
            } catch (_: Exception) {
                TemplateId.EMPTY
            },
            packageName = packageName,
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt,
            lastBackupAt = lastBackupAt,
            checkpointCount = checkpointCount,
            sizeBytes = sizeBytes,
            lastBuildStatus = lastBuildStatus
        )
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }
}
