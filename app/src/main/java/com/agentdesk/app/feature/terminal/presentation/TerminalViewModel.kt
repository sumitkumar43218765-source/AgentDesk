package com.agentdesk.app.feature.terminal.presentation

import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.mvi.MviViewModel
import com.agentdesk.app.core.mvi.UiEffect
import com.agentdesk.app.feature.terminal.data.DefaultTerminalViewClient
import com.agentdesk.app.feature.terminal.data.TermuxSessionHandle
import com.agentdesk.app.feature.terminal.domain.SessionRequest
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.regex.Pattern
import javax.inject.Inject

sealed interface TerminalEffect : UiEffect {
    data class ShowMessage(val message: String) : TerminalEffect
    data class ShareText(val text: String) : TerminalEffect
    data class OpenBrowser(val url: String) : TerminalEffect
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val shellBackend: ShellBackend,
    val viewClient: DefaultTerminalViewClient
) : MviViewModel<TerminalUiState, TerminalEvent, TerminalEffect>(TerminalUiState()) {

    companion object {
        private val URL_PATTERN = Pattern.compile(
            "https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
        )
    }

    init {
        viewClient.onFontSizeChanged = { size ->
            setState { copy(fontSize = size) }
        }
        viewClient.onLongPressListener = {
            onEvent(TerminalEvent.OpenContextMenu)
        }
        createNewSession(isFailsafe = false)
    }

    override fun onEvent(event: TerminalEvent) {
        when (event) {
            is TerminalEvent.SelectSession -> selectSession(event.index)
            is TerminalEvent.NewSession -> createNewSession(isFailsafe = false)
            is TerminalEvent.NewFailsafeSession -> createNewSession(isFailsafe = true)
            is TerminalEvent.CloseSession -> closeSession(event.index)
            is TerminalEvent.PromptRenameSession -> {
                val currentTitle = currentState.sessions.getOrNull(event.index)?.title ?: ""
                setState { copy(renameSessionIndex = event.index, renameInputText = currentTitle) }
            }
            is TerminalEvent.RenameInputChanged -> setState { copy(renameInputText = event.text) }
            is TerminalEvent.ConfirmRenameSession -> renameSession(event.index, event.newTitle)
            is TerminalEvent.DismissRenameDialog -> setState { copy(renameSessionIndex = null, renameInputText = "") }
            is TerminalEvent.ExtraKeyPressed -> handleExtraKey(event.key)
            is TerminalEvent.ClearCurrentSession -> clearCurrentSession()
            is TerminalEvent.KillCurrentProcess -> killCurrentProcess()
            is TerminalEvent.ToggleDrawer -> setState { copy(isDrawerOpen = !isDrawerOpen) }
            is TerminalEvent.SetDrawerOpen -> setState { copy(isDrawerOpen = event.isOpen) }
            is TerminalEvent.OpenContextMenu -> setState { copy(isContextMenuOpen = true) }
            is TerminalEvent.DismissContextMenu -> setState { copy(isContextMenuOpen = false) }
            is TerminalEvent.SelectUrl -> extractAndShowUrls()
            is TerminalEvent.DismissUrlDialog -> setState { copy(showUrlDialog = false, detectedUrls = emptyList()) }
            is TerminalEvent.ShareTranscript -> shareTranscript()
            is TerminalEvent.ToggleKeepScreenOn -> setState { copy(keepScreenOn = !keepScreenOn) }
            is TerminalEvent.ToggleKeyboard -> viewClient.toggleSoftKeyboard()
            is TerminalEvent.FontSizeChanged -> setState { copy(fontSize = event.size) }
        }
    }

    private fun selectSession(index: Int) {
        if (index in currentState.sessions.indices) {
            setState { copy(selectedSessionIndex = index, isDrawerOpen = false) }
        }
    }

    private fun createNewSession(isFailsafe: Boolean) {
        val count = currentState.sessions.size + 1
        val title = if (isFailsafe) "Failsafe $count" else "Session $count"
        val request = SessionRequest(
            title = title,
            environment = if (isFailsafe) ShellEnvironment.TERMUX_NATIVE else ShellEnvironment.PROOT_UBUNTU,
            isFailsafe = isFailsafe
        )
        val handle = shellBackend.openSession(request)
        val tab = TerminalSessionTab(
            id = handle.id,
            title = title,
            handle = handle,
            isFailsafe = isFailsafe
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
                isRunning = true,
                isDrawerOpen = false
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

    private fun renameSession(index: Int, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty() || index !in currentState.sessions.indices) {
            setState { copy(renameSessionIndex = null, renameInputText = "") }
            return
        }

        val updated = currentState.sessions.toMutableList()
        updated[index] = updated[index].copy(title = trimmed)
        setState { copy(sessions = updated, renameSessionIndex = null, renameInputText = "") }
    }

    private fun clearCurrentSession() {
        val activeHandle = getActiveHandle()
        activeHandle?.terminalSession?.reset()
        activeHandle?.write("clear\r")
    }

    private fun killCurrentProcess() {
        val activeHandle = getActiveHandle()
        activeHandle?.close()
        setState { copy(isRunning = false, isContextMenuOpen = false) }
    }

    private fun extractAndShowUrls() {
        val activeHandle = getActiveHandle() ?: return
        val transcript = activeHandle.getTranscriptText()
        val matcher = URL_PATTERN.matcher(transcript)
        val urls = LinkedHashSet<String>()
        while (matcher.find()) {
            urls.add(matcher.group())
        }

        setState {
            copy(
                isContextMenuOpen = false,
                detectedUrls = urls.toList(),
                showUrlDialog = true
            )
        }
    }

    private fun shareTranscript() {
        val activeHandle = getActiveHandle() ?: return
        val transcript = activeHandle.getTranscriptText()
        setState { copy(isContextMenuOpen = false) }
        if (transcript.isNotBlank()) {
            emitEffect(TerminalEffect.ShareText(transcript))
        } else {
            emitEffect(TerminalEffect.ShowMessage("Transcript is empty"))
        }
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
            "HOME" -> activeHandle?.write("\u001b[H")
            "END" -> activeHandle?.write("\u001b[F")
            "PGUP" -> activeHandle?.write("\u001b[5~")
            "PGDN" -> activeHandle?.write("\u001b[6~")
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
