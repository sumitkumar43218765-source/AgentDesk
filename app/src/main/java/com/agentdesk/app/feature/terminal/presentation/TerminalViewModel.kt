package com.agentdesk.app.feature.terminal.presentation

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.core.mvi.UiEffect
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellEvent
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface TerminalEffect : UiEffect {
    data class ShowMessage(val message: String) : TerminalEffect
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val shellBackend: ShellBackend
) : MviViewModel<TerminalUiState, TerminalEvent, TerminalEffect>(TerminalUiState()) {

    override fun onEvent(event: TerminalEvent) {
        when (event) {
            is TerminalEvent.SelectSession -> setState { copy(selectedSessionIndex = event.index) }
            is TerminalEvent.NewSession -> createNewSession()
            is TerminalEvent.CloseSession -> closeSession(event.index)
            is TerminalEvent.InputChanged -> setState { copy(commandInput = event.text) }
            is TerminalEvent.ExecuteCommand -> executeCommand()
            is TerminalEvent.ExtraKeyPressed -> handleExtraKey(event.key)
            is TerminalEvent.ClearCurrentSession -> clearCurrentSession()
        }
    }

    private fun createNewSession() {
        val count = currentState.sessions.size + 1
        val newSession = TerminalSessionData(
            id = UUID.randomUUID().toString(),
            title = "Session $count"
        )
        setState {
            copy(
                sessions = sessions + newSession,
                selectedSessionIndex = sessions.size
            )
        }
    }

    private fun closeSession(index: Int) {
        if (currentState.sessions.size <= 1) return
        val updated = currentState.sessions.toMutableList().apply { removeAt(index) }
        val newIndex = (currentState.selectedSessionIndex).coerceAtMost(updated.size - 1)
        setState { copy(sessions = updated, selectedSessionIndex = newIndex) }
    }

    private fun executeCommand() {
        val cmd = currentState.commandInput.trim()
        if (cmd.isBlank() || currentState.isRunning) return

        val activeSession = currentState.sessions[currentState.selectedSessionIndex]
        val executionId = "term_${UUID.randomUUID()}"

        val request = ShellRequest(
            command = cmd,
            environment = ShellEnvironment.PROOT_UBUNTU,
            executionId = executionId
        )

        appendOutput("$ $cmd\n")
        setState { copy(commandInput = "", isRunning = true) }

        viewModelScope.launch {
            shellBackend.run(request).collect { event ->
                when (event) {
                    is ShellEvent.Stdout -> appendOutput(event.chunk)
                    is ShellEvent.Stderr -> appendOutput(event.chunk)
                    is ShellEvent.Exited -> {
                        setState { copy(isRunning = false) }
                    }
                    is ShellEvent.Failure -> {
                        appendOutput("[Error: ${event.error.message}]\n")
                        setState { copy(isRunning = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun appendOutput(text: String) {
        val idx = currentState.selectedSessionIndex
        val updatedSessions = currentState.sessions.toMutableList()
        val cur = updatedSessions[idx]
        updatedSessions[idx] = cur.copy(output = cur.output + text)
        setState { copy(sessions = updatedSessions) }
    }

    private fun handleExtraKey(key: String) {
        when (key) {
            "TAB" -> setState { copy(commandInput = commandInput + "\t") }
            else -> setState { copy(commandInput = commandInput + key) }
        }
    }

    private fun clearCurrentSession() {
        val idx = currentState.selectedSessionIndex
        val updatedSessions = currentState.sessions.toMutableList()
        val cur = updatedSessions[idx]
        updatedSessions[idx] = cur.copy(output = "")
        setState { copy(sessions = updatedSessions) }
    }
}
