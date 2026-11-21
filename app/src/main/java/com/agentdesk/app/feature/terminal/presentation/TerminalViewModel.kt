package com.agentdesk.app.feature.terminal.presentation

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.core.mvi.UiEffect
import com.agentdesk.app.feature.terminal.data.DefaultTerminalViewClient
import com.agentdesk.app.feature.terminal.data.TermuxSessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

sealed interface TerminalEffect : UiEffect {
    data class ShowMessage(val message: String) : TerminalEffect
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val shellBackend: ShellBackend,
    val viewClient: DefaultTerminalViewClient
) : MviViewModel<TerminalUiState, TerminalEvent, TerminalEffect>(TerminalUiState()) {

    init {
        viewClient.onFontSizeChanged = { size ->
            setState { copy(fontSize = size) }
        }
        createNewSession()
    }

    override fun onEvent(event: TerminalEvent) {
        when (event) {
            is TerminalEvent.SelectSession -> selectSession(event.index)
            is TerminalEvent.NewSession -> createNewSession()
            is TerminalEvent.CloseSession -> closeSession(event.index)
            is TerminalEvent.ExtraKeyPressed -> handleExtraKey(event.key)
            is TerminalEvent.ClearCurrentSession -> clearCurrentSession()
            is TerminalEvent.FontSizeChanged -> setState { copy(fontSize = event.size) }
        }
    }

    private fun selectSession(index: Int) {
        if (index in currentState.sessions.indices) {
            setState { copy(selectedSessionIndex = index) }
        }
    }

    private fun createNewSession() {
        val count = currentState.sessions.size + 1
        val title = "Session $count"
        val request = SessionRequest(
            title = title,
            environment = ShellEnvironment.PROOT_UBUNTU
        )
        val handle = shellBackend.openSession(request)
        val tab = TerminalSessionTab(
            id = handle.id,
            title = title,
            handle = handle
        )

        handle.isRunning
            .onEach { running ->
                if (!running) {
                    val curIdx = currentState.selectedSessionIndex
                    if (curIdx in currentState.sessions.indices && currentState.sessions[curIdx].id == handle.id) {
                        setState { copy(isRunning = false) }
                    }
                }
            }
            .launchIn(viewModelScope)

        setState {
            copy(
                sessions = sessions + tab,
                selectedSessionIndex = sessions.size,
                isRunning = true
            )
        }
    }

    private fun closeSession(index: Int) {
        if (currentState.sessions.size <= 1) return
        val sessionToClose = currentState.sessions.getOrNull(index)
        sessionToClose?.handle?.close()

        val updated = currentState.sessions.toMutableList().apply { removeAt(index) }
        val newIndex = currentState.selectedSessionIndex.coerceAtMost(updated.size - 1)
        setState { copy(sessions = updated, selectedSessionIndex = newIndex) }
    }

    private fun clearCurrentSession() {
        val activeHandle = getActiveHandle()
        activeHandle?.terminalSession?.reset()
        activeHandle?.write("clear\r")
    }

    private fun handleExtraKey(key: String) {
        val activeHandle = getActiveHandle()

        when (key) {
            "ESC" -> activeHandle?.write("\u001b")
            "TAB" -> activeHandle?.write("\t")
            "CTRL" -> {
                val newActive = !currentState.isCtrlActive
                viewClient.setControlActive(newActive)
                setState { copy(isCtrlActive = newActive) }
            }
            "ALT" -> {
                val newActive = !currentState.isAltActive
                viewClient.setAltActive(newActive)
                setState { copy(isAltActive = newActive) }
            }
            "▲" -> activeHandle?.write("\u001b[A")
            "▼" -> activeHandle?.write("\u001b[B")
            "◀" -> activeHandle?.write("\u001b[D")
            "▶" -> activeHandle?.write("\u001b[C")
            "/" -> activeHandle?.write("/")
            "-" -> activeHandle?.write("-")
            "|" -> activeHandle?.write("|")
            "~" -> activeHandle?.write("~")
            else -> activeHandle?.write(key)
        }
    }

    private fun getActiveHandle(): TermuxSessionHandle? {
        val cur = currentState.sessions.getOrNull(currentState.selectedSessionIndex)
        return cur?.handle as? TermuxSessionHandle
    }

    override fun onCleared() {
        super.onCleared()
        for (session in currentState.sessions) {
            try {
                session.handle?.close()
            } catch (ignored: Exception) {}
        }
    }
}
