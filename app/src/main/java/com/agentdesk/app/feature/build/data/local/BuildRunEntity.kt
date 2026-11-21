package com.agentdesk.app.feature.build.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildStatus
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.BuildSummary
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.Severity
import com.agentdesk.app.feature.build.domain.model.StepResultSummary
import com.agentdesk.app.feature.build.domain.model.StepStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "build_runs",
    indices = [
        Index("project_id"),
        Index("started_at")
    ]
)
data class BuildRunEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    val variant: String,
    val status: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "finished_at")
    val finishedAt: Long?,
    @ColumnInfo(name = "apk_host_path")
    val apkHostPath: String?,
    @ColumnInfo(name = "log_path")
    val logPath: String,
    @ColumnInfo(name = "failed_step")
    val failedStep: String?,
    @ColumnInfo(name = "diagnostics_json")
    val diagnosticsJson: String,
    @ColumnInfo(name = "steps_json")
    val stepsJson: String,
    @ColumnInfo(name = "git_commit")
    val gitCommit: String?
) {
    fun toDomain(): BuildRun {
        val json = Json { ignoreUnknownKeys = true }
        val diagList = try {
            json.decodeFromString<List<DiagnosticDto>>(diagnosticsJson).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
        val stepList = try {
            json.decodeFromString<List<StepResultSummaryDto>>(stepsJson).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }

        return BuildRun(
            id = id,
            projectId = projectId,
            variant = runCatching { BuildVariant.valueOf(variant) }.getOrDefault(BuildVariant.DEBUG),
            status = runCatching { BuildStatus.valueOf(status) }.getOrDefault(BuildStatus.FAILED),
            startedAt = startedAt,
            finishedAt = finishedAt,
            apkHostPath = apkHostPath,
            logPath = logPath,
            summary = BuildSummary(
                failedStep = failedStep?.let { runCatching { BuildStepId.valueOf(it) }.getOrNull() },
                diagnostics = diagList,
                steps = stepList
            ),
            gitCommit = gitCommit
        )
    }

    companion object {
        fun fromDomain(run: BuildRun): BuildRunEntity {
            val json = Json { ignoreUnknownKeys = true }
            val diagDtos = run.summary.diagnostics.map { DiagnosticDto.fromDomain(it) }
            val stepDtos = run.summary.steps.map { StepResultSummaryDto.fromDomain(it) }

            return BuildRunEntity(
                id = run.id,
                projectId = run.projectId,
                variant = run.variant.name,
                status = run.status.name,
                startedAt = run.startedAt,
                finishedAt = run.finishedAt,
                apkHostPath = run.apkHostPath,
                logPath = run.logPath,
                failedStep = run.summary.failedStep?.name,
                diagnosticsJson = json.encodeToString(diagDtos),
                stepsJson = json.encodeToString(stepDtos),
                gitCommit = run.gitCommit
            )
        }
    }
}

@Serializable
data class DiagnosticDto(
    val severity: String,
    val file: String?,
    val line: Int?,
    val column: Int?,
    val message: String,
    val step: String
) {
    fun toDomain(): BuildDiagnostic = BuildDiagnostic(
        severity = runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.ERROR),
        file = file,
        line = line,
        column = column,
        message = message,
        step = runCatching { BuildStepId.valueOf(step) }.getOrDefault(BuildStepId.JAVA_COMPILE)
    )

    companion object {
        fun fromDomain(d: BuildDiagnostic) = DiagnosticDto(
            severity = d.severity.name,
            file = d.file,
            line = d.line,
            column = d.column,
            message = d.message,
            step = d.step.name
        )
    }
}

@Serializable
data class StepResultSummaryDto(
    val step: String,
    val status: String,
    val durationMs: Long
) {
    fun toDomain(): StepResultSummary = StepResultSummary(
        step = runCatching { BuildStepId.valueOf(step) }.getOrDefault(BuildStepId.RESOURCE_COMPILE),
        status = runCatching { StepStatus.valueOf(status) }.getOrDefault(StepStatus.PENDING),
        durationMs = durationMs
    )

    companion object {
        fun fromDomain(s: StepResultSummary) = StepResultSummaryDto(
            step = s.step.name,
            status = s.status.name,
            durationMs = s.durationMs
        )
    }
}
