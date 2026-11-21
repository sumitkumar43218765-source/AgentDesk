package com.agentdesk.app.feature.terminal.presentation

import com.agentdesk.app.core.mvi.UiState
import com.agentdesk.app.feature.terminal.domain.SessionHandle

data class TerminalSessionTab(
    val id: String,
    val title: String,
    val handle: SessionHandle? = null
)

data class TerminalUiState(
    val sessions: List<TerminalSessionTab> = emptyList(),
    val selectedSessionIndex: Int = 0,
    val fontSize: Int = 14,
    val isRunning: Boolean = false,
    val isCtrlActive: Boolean = false,
    val isAltActive: Boolean = false
) : UiState
