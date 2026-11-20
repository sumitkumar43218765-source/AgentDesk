package com.agentdesk.app.feature.terminal.domain

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val truncated: Boolean,
    val fullLogPath: String?
)
