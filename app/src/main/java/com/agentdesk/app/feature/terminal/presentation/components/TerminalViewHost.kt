package com.agentdesk.app.feature.terminal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.MonoText

@Composable
fun TerminalViewHost(
    output: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        MonoText(
            text = output.ifBlank { "AgentDesk Linux Terminal (proot Ubuntu)\n$ " },
            color = Color(0xFF00FF66),
            selectable = true
        )
    }
}
