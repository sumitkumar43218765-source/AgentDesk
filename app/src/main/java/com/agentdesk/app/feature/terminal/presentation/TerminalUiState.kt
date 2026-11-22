package com.agentdesk.app.feature.terminal.presentation

import com.agentdesk.app.core.mvi.UiState
import com.agentdesk.app.feature.terminal.domain.SessionHandle

data class TerminalSessionTab(
    val id: String,
    val title: String,
    val handle: SessionHandle? = null,
    val isFailsafe: Boolean = false
)

data class TerminalUiState(
    val sessions: List<TerminalSessionTab> = emptyList(),
    val selectedSessionIndex: Int = 0,
    val fontSize: Int = 14,
    val isRunning: Boolean = false,
    val isCtrlActive: Boolean = false,
    val isAltActive: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val isContextMenuOpen: Boolean = false,
    val keepScreenOn: Boolean = false,
    val detectedUrls: List<String> = emptyList(),
    val showUrlDialog: Boolean = false,
    val renameSessionIndex: Int? = null,
    val renameInputText: String = ""
) : UiState
