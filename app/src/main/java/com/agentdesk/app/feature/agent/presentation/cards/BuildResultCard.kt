package com.agentdesk.app.feature.agent.presentation.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel

@Composable
fun BuildResultCard(
    status: String,
    apkPath: String?,
    durationMs: Long,
    onInstallApk: () -> Unit,
    onViewLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSuccess = status.equals("SUCCESS", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Build Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(
                    text = if (isSuccess) "SUCCESS" else "FAILED",
                    level = if (isSuccess) StatusLevel.SUCCESS else StatusLevel.ERROR
                )
            }

            Text(
                text = "Duration: ${durationMs}ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (!apkPath.isNullOrBlank()) {
                Text(
                    text = "APK: $apkPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSuccess && !apkPath.isNullOrBlank()) {
                    Button(onClick = onInstallApk) {
                        Text("Install APK")
                    }
                }
                OutlinedButton(onClick = onViewLogs) {
                    Text("View Log")
                }
            }
        }
    }
}
