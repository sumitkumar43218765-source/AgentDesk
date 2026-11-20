package com.agentdesk.app.feature.terminal.domain

import kotlinx.coroutines.flow.StateFlow

interface SessionHandle {
    val id: String
    val isRunning: StateFlow<Boolean>
    fun write(text: String)
    fun close()
}
