package com.agentdesk.app.feature.terminal.presentation

import com.agentdesk.app.core.mvi.UiState

data class TerminalSessionData(
    val id: String,
    val title: String,
    val output: String = ""
)

data class TerminalUiState(
    val sessions: List<TerminalSessionData> = listOf(TerminalSessionData("1", "Session 1")),
    val selectedSessionIndex: Int = 0,
    val commandInput: String = "",
    val fontSize: Int = 14,
    val isRunning: Boolean = false
) : UiState
