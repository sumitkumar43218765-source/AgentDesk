package com.agentdesk.app.feature.terminal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.agentdesk.app.feature.terminal.data.DefaultTerminalViewClient
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

@Composable
fun TerminalViewHost(
    terminalSession: TerminalSession?,
    terminalViewClient: DefaultTerminalViewClient,
    modifier: Modifier = Modifier
) {
    if (terminalSession == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF101010)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00FF66))
        }
        return
    }

    AndroidView(
        factory = { context ->
            TerminalView(context, null).apply {
                terminalViewClient.attachView(this)
                setTerminalViewClient(terminalViewClient)
                setTextSize(14)
                attachSession(terminalSession)
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
            }
        },
        update = { terminalView ->
            terminalViewClient.attachView(terminalView)
            terminalView.setTerminalViewClient(terminalViewClient)
            if (terminalView.mTermSession != terminalSession) {
                terminalView.attachSession(terminalSession)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
