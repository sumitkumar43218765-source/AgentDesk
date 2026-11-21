package com.agentdesk.app.feature.build.data.engine

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.data.engine.steps.AlignStep
import com.agentdesk.app.feature.build.data.engine.steps.DexStep
import com.agentdesk.app.feature.build.data.engine.steps.JavaCompileStep
import com.agentdesk.app.feature.build.data.engine.steps.PackageStep
import com.agentdesk.app.feature.build.data.engine.steps.ResourceCompileStep
import com.agentdesk.app.feature.build.data.engine.steps.ResourceLinkStep
import com.agentdesk.app.feature.build.data.engine.steps.SignStep
import com.agentdesk.app.feature.build.data.local.RoomBuildHistoryRepository
import com.agentdesk.app.feature.build.data.parse.ManifestReader
import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.KeystoreManager
import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.BuildEvent
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildStatus
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.BuildSummary
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.LogSlice
import com.agentdesk.app.feature.build.domain.model.Severity
import com.agentdesk.app.feature.build.domain.model.StepResultSummary
import com.agentdesk.app.feature.build.domain.model.StepStatus
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import com.agentdesk.app.feature.projects.domain.repository.GitService
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBuildEngine @Inject constructor(
    private val projectDao: ProjectDao,
    private val pathProvider: PathProvider,
    private val manifestReader: ManifestReader,
    private val keystoreManager: KeystoreManager,
    private val historyRepository: RoomBuildHistoryRepository,
    private val buildScheduler: BuildScheduler,
    private val buildEnvironment: BuildEnvironment,
    private val gitService: GitService,
    private val resourceCompileStep: ResourceCompileStep,
    private val resourceLinkStep: ResourceLinkStep,
    private val javaCompileStep: JavaCompileStep,
    private val dexStep: DexStep,
    private val packageStep: PackageStep,
    private val alignStep: AlignStep,
    private val signStep: SignStep
) : BuildEngine {

    override fun build(projectId: String, variant: BuildVariant): Flow<BuildEvent> = channelFlow {
        val project = projectDao.getById(projectId)
        if (project == null) {
            val errRun = createFailedRun(
                projectId = projectId,
                variant = variant,
                errorMessage = "Project with id '$projectId' not found in database."
            )
            send(BuildEvent.Started(errRun.id))
            send(BuildEvent.Finished(errRun))
            return@channelFlow
        }

        val projectDir = File(pathProvider.projectsRoot, project.slug)
        val manifestFile = File(projectDir, "AndroidManifest.xml")
        val manifestResult = manifestReader.read(manifestFile)

        if (manifestResult is AppResult.Failure) {
            val diag = BuildDiagnostic(
                severity = Severity.ERROR,
                file = "AndroidManifest.xml",
                line = 1,
                column = null,
                message = manifestResult.error.message,
                step = BuildStepId.RESOURCE_LINK
            )
            val errRun = createFailedRun(
                projectId = projectId,
                variant = variant,
                errorMessage = manifestResult.error.message,
                diagnostics = listOf(diag)
            )
            send(BuildEvent.Started(errRun.id))
            send(BuildEvent.Finished(errRun))
            return@channelFlow
        }

        val manifestInfo = (manifestResult as AppResult.Success).value

        val signingResult = keystoreManager.getSigningConfig(variant)
        if (signingResult is AppResult.Failure) {
            val errRun = createFailedRun(
                projectId = projectId,
                variant = variant,
                errorMessage = "Failed to obtain signing config: ${signingResult.error.message}"
            )
            send(BuildEvent.Started(errRun.id))
            send(BuildEvent.Finished(errRun))
            return@channelFlow
        }

        val signingConfig = (signingResult as AppResult.Success).value

        val buildId = UUID.randomUUID().toString()
        val buildDir = File(projectDir, "build")

        // Full rebuild in MVP: clean build/
        buildDir.deleteRecursively()
        buildDir.mkdirs()

        val context = BuildContext(
            projectId = projectId,
            projectSlug = project.slug,
            projectDir = projectDir,
            guestProjectDir = "/root/projects/${project.slug}",
            buildDir = buildDir,
            manifestInfo = manifestInfo,
            variant = variant,
            signingConfig = signingConfig,
            androidJarGuestPath = buildEnvironment.resolveAndroidJar(),
            buildToolsGuestDir = buildEnvironment.resolveBuildTools(),
            javaHomeGuestPath = buildEnvironment.resolveJavaHome()
        )

        send(BuildEvent.Started(buildId))

        buildScheduler.schedule(buildId) {
            val job = currentCoroutineContext()[kotlinx.coroutines.Job]
            if (job != null) {
                buildScheduler.registerActiveJob(job)
            }

            val steps = listOf<BuildStep>(
                resourceCompileStep,
                resourceLinkStep,
                javaCompileStep,
                dexStep,
                packageStep,
                alignStep,
                signStep
            )

            val stepSummaries = mutableListOf<StepResultSummary>()
            val allDiagnostics = mutableListOf<BuildDiagnostic>()
            var failedStepId: BuildStepId? = null
            var overallStatus = BuildStatus.SUCCESS
            val startTime = System.currentTimeMillis()

            for (step in steps) {
                if (!currentCoroutineContext().isActive) {
                    overallStatus = BuildStatus.CANCELLED
                    break
                }

                if (failedStepId != null) {
                    stepSummaries.add(
                        StepResultSummary(
                            step = step.id,
                            status = StepStatus.SKIPPED,
                            durationMs = 0L
                        )
                    )
                    send(BuildEvent.StepFinished(step.id, StepStatus.SKIPPED, 0L))
                    continue
                }

                send(BuildEvent.StepStarted(step.id))

                val result = step.execute(context) { chunk ->
                    trySend(BuildEvent.StepLog(step.id, chunk))
                }

                stepSummaries.add(
                    StepResultSummary(
                        step = step.id,
                        status = result.status,
                        durationMs = result.durationMs
                    )
                )

                if (result.diagnostics.isNotEmpty()) {
                    allDiagnostics.addAll(result.diagnostics)
                }

                send(BuildEvent.StepFinished(step.id, result.status, result.durationMs))

                if (result.status == StepStatus.FAILED) {
                    failedStepId = step.id
                    overallStatus = BuildStatus.FAILED
                }
            }

            var gitCommitSha: String? = null
            val apkFile = File(buildDir, "app.apk")
            val apkHostPath = if (overallStatus == BuildStatus.SUCCESS && apkFile.exists()) {
                apkFile.absolutePath
            } else {
                null
            }

            if (overallStatus == BuildStatus.SUCCESS) {
                val checkpointRes = gitService.checkpoint(project.slug, "build $buildId ok")
                if (checkpointRes is AppResult.Success) {
                    gitCommitSha = checkpointRes.value.sha
                }
            }

            val finalRun = BuildRun(
                id = buildId,
                projectId = projectId,
                variant = variant,
                status = overallStatus,
                startedAt = startTime,
                finishedAt = System.currentTimeMillis(),
                apkHostPath = apkHostPath,
                logPath = pathProvider.buildLogsDir.resolve("$buildId.log").absolutePath,
                summary = BuildSummary(
                    failedStep = failedStepId,
                    diagnostics = allDiagnostics,
                    steps = stepSummaries
                ),
                gitCommit = gitCommitSha
            )

            historyRepository.saveRun(finalRun)
            send(BuildEvent.Finished(finalRun))
        }
    }

    override suspend fun cancel(buildId: String) {
        buildScheduler.cancel(buildId)
    }

    override fun observeHistory(projectId: String): Flow<List<BuildRun>> {
        return historyRepository.observeHistory(projectId)
    }

    override suspend fun getRun(buildId: String): BuildRun? {
        return historyRepository.getRun(buildId)
    }

    override suspend fun readLog(buildId: String, offset: Int, limit: Int): AppResult<LogSlice> {
        return historyRepository.readLog(buildId, offset, limit)
    }

    private suspend fun createFailedRun(
        projectId: String,
        variant: BuildVariant,
        errorMessage: String,
        diagnostics: List<BuildDiagnostic> = emptyList()
    ): BuildRun {
        val id = UUID.randomUUID().toString()
        val run = BuildRun(
            id = id,
            projectId = projectId,
            variant = variant,
            status = BuildStatus.FAILED,
            startedAt = System.currentTimeMillis(),
            finishedAt = System.currentTimeMillis(),
            apkHostPath = null,
            logPath = pathProvider.buildLogsDir.resolve("$id.log").absolutePath,
            summary = BuildSummary(
                failedStep = BuildStepId.RESOURCE_LINK,
                diagnostics = diagnostics,
                steps = emptyList()
            ),
            gitCommit = null
        )
        historyRepository.saveRun(run)
        return run
    }
}
