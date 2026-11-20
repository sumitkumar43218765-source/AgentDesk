package com.agentdesk.app.feature.setup.presentation.install.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.theme.Spacing

@Composable
fun InstallActionsBar(
    canSkip: Boolean,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onOpenTerminal: () -> Unit,
    onCopyLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Text("Retry")
            }

            if (canSkip) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
            }

            OutlinedButton(
                onClick = onOpenTerminal,
                modifier = Modifier.weight(1f)
            ) {
                Text("Terminal")
            }

            OutlinedButton(
                onClick = onCopyLog,
                modifier = Modifier.weight(1f)
            ) {
                Text("Copy Log")
            }
        }
    }
}
