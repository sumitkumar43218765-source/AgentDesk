package com.agentdesk.app.feature.terminal.domain

data class SessionRequest(
    val title: String,
    val command: String? = null,
    val environment: ShellEnvironment = ShellEnvironment.PROOT_UBUNTU,
    val cwd: String? = null,
    val isFailsafe: Boolean = false
)
