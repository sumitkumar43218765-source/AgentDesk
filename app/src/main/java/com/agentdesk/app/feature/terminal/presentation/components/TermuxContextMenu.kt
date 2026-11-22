package com.agentdesk.app.feature.terminal.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermuxContextMenuDialog(
    isOpen: Boolean,
    keepScreenOn: Boolean,
    onDismiss: () -> Unit,
    onSelectUrl: () -> Unit,
    onShareTranscript: () -> Unit,
    onResetTerminal: () -> Unit,
    onKillProcess: () -> Unit,
    onToggleKeepScreenOn: () -> Unit
) {
    if (!isOpen) return

    val menuItems = listOf(
        "Select URL" to onSelectUrl,
        "Share transcript" to onShareTranscript,
        "Reset terminal" to onResetTerminal,
        "Kill process" to onKillProcess,
        (if (keepScreenOn) "Disable keep screen on" else "Keep screen on") to onToggleKeepScreenOn
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Termux Terminal", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                menuItems.forEachIndexed { index, (label, action) ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                action()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (index < menuItems.size - 1) {
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun TermuxUrlDialog(
    isOpen: Boolean,
    urls: List<String>,
    onDismiss: () -> Unit,
    onUrlSelected: (String) -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select URL", style = MaterialTheme.typography.titleMedium) },
        text = {
            if (urls.isEmpty()) {
                Text("No URLs found in terminal output.", color = Color(0xFF888888))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(urls) { url ->
                        Text(
                            text = url,
                            color = Color(0xFF64B5F6),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUrlSelected(url) }
                                .padding(vertical = 10.dp)
                        )
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun TermuxRenameDialog(
    isOpen: Boolean,
    currentTitle: String,
    onTitleChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Session", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    placeholder = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentTitle) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp)
    )
}
