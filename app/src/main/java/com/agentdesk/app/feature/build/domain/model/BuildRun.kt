package com.agentdesk.app.feature.build.domain.model

data class BuildRun(
    val id: String,
    val projectId: String,
    val variant: BuildVariant,
    val status: BuildStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val apkHostPath: String?,
    val logPath: String,
    val summary: BuildSummary,
    val gitCommit: String?
)
