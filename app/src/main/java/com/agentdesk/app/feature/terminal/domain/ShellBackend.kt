package com.agentdesk.app.feature.terminal.domain

import kotlinx.coroutines.flow.Flow

interface ShellBackend {
    suspend fun isReady(): Boolean
    fun run(request: ShellRequest): Flow<ShellEvent>
    suspend fun runToCompletion(request: ShellRequest): ShellResult
    suspend fun cancel(executionId: String)
    fun openSession(request: SessionRequest): SessionHandle
}
