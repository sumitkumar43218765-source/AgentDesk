package com.agentdesk.app.feature.agent.presentation.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onStop,
        modifier = modifier
    ) {
        Text(
            text = "Stop",
            color = MaterialTheme.colorScheme.error
        )
    }
}
