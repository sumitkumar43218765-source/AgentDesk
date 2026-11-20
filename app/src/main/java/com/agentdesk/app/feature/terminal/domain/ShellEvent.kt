package com.agentdesk.app.feature.terminal.domain

import com.agentdesk.app.core.common.AppError

sealed interface ShellEvent {
    data class Started(val executionId: String) : ShellEvent
    data class Stdout(val chunk: String) : ShellEvent
    data class Stderr(val chunk: String) : ShellEvent
    data class Exited(val code: Int, val durationMs: Long) : ShellEvent
    data object TimedOut : ShellEvent
    data object Cancelled : ShellEvent
    data class Failure(val error: AppError) : ShellEvent
}
