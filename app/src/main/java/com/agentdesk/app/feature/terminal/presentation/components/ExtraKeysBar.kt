package com.agentdesk.app.feature.terminal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExtraKeysBar(
    onKeyPress: (String) -> Unit,
    isCtrlActive: Boolean = false,
    isAltActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val keys = listOf("ESC", "TAB", "CTRL", "ALT", "/", "-", "|", "~", "▲", "▼", "◀", "▶")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        for (key in keys) {
            val isHighlighted = (key == "CTRL" && isCtrlActive) || (key == "ALT" && isAltActive)
            val btnColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else Color.White

            TextButton(
                onClick = { onKeyPress(key) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = btnColor,
                    contentColor = contentColor
                ),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(text = key, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
