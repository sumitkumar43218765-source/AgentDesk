package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TermuxSessionHandle(
    override val id: String,
    val terminalSession: TerminalSession,
    private val onCloseCallback: (() -> Unit)? = null
) : SessionHandle {

    private val _isRunning = MutableStateFlow(true)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun markFinished() {
        _isRunning.value = false
    }

    override fun write(text: String) {
        terminalSession.write(text)
    }

    override fun close() {
        _isRunning.value = false
        try {
            terminalSession.finishIfRunning()
        } catch (ignored: Exception) {}
        onCloseCallback?.invoke()
    }
}
