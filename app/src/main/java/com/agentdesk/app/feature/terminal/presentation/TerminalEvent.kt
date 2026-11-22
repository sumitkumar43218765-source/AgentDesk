package com.agentdesk.app.feature.terminal.presentation

import com.agentdesk.app.core.mvi.UiEvent

sealed interface TerminalEvent : UiEvent {
    data class SelectSession(val index: Int) : TerminalEvent
    data object NewSession : TerminalEvent
    data object NewFailsafeSession : TerminalEvent
    data class CloseSession(val index: Int) : TerminalEvent
    data class PromptRenameSession(val index: Int) : TerminalEvent
    data class RenameInputChanged(val text: String) : TerminalEvent
    data class ConfirmRenameSession(val index: Int, val newTitle: String) : TerminalEvent
    data object DismissRenameDialog : TerminalEvent
    data class ExtraKeyPressed(val key: String) : TerminalEvent
    data object ClearCurrentSession : TerminalEvent
    data object KillCurrentProcess : TerminalEvent
    data object ToggleDrawer : TerminalEvent
    data class SetDrawerOpen(val isOpen: Boolean) : TerminalEvent
    data object OpenContextMenu : TerminalEvent
    data object DismissContextMenu : TerminalEvent
    data object SelectUrl : TerminalEvent
    data object DismissUrlDialog : TerminalEvent
    data object ShareTranscript : TerminalEvent
    data object ToggleKeepScreenOn : TerminalEvent
    data object ToggleKeyboard : TerminalEvent
    data class FontSizeChanged(val size: Int) : TerminalEvent
}
