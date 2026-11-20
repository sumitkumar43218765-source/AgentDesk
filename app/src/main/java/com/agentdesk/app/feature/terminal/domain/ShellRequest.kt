package com.agentdesk.app.feature.terminal.domain

data class ShellRequest(
    val command: String,
    val cwd: String? = null,
    val env: Map<String, String> = emptyMap(),
    val environment: ShellEnvironment = ShellEnvironment.PROOT_UBUNTU,
    val timeoutMs: Long = 120_000,
    val executionId: String,
    val bindExternalStorage: Boolean = false
)
