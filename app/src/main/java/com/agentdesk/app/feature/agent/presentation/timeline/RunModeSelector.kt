package com.agentdesk.app.feature.agent.presentation.timeline

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agentdesk.app.feature.agent.domain.model.RunMode

@Composable
fun RunModeSelector(
    currentMode: RunMode,
    onModeSelected: (RunMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextMode = if (currentMode == RunMode.ASK) RunMode.AUTO_ACCEPT_EDITS else RunMode.ASK
    val label = if (currentMode == RunMode.ASK) "Mode: Ask" else "Mode: Auto Edits"

    AssistChip(
        onClick = { onModeSelected(nextMode) },
        label = { Text(label) },
        modifier = modifier
    )
}
