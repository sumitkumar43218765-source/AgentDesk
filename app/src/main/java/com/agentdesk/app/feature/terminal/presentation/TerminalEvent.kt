package com.agentdesk.app.feature.terminal.presentation

import com.agentdesk.app.core.mvi.UiEvent

sealed interface TerminalEvent : UiEvent {
    data class SelectSession(val index: Int) : TerminalEvent
    data object NewSession : TerminalEvent
    data class CloseSession(val index: Int) : TerminalEvent
    data class ExtraKeyPressed(val key: String) : TerminalEvent
    data object ClearCurrentSession : TerminalEvent
    data class FontSizeChanged(val size: Int) : TerminalEvent
}
