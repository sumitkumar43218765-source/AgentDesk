package com.agentdesk.app.feature.setup.presentation.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.setup.domain.model.InstallPlan

@Composable
fun SizeSummaryBar(
    plan: InstallPlan?,
    isWifi: Boolean,
    isStarting: Boolean,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            if (plan != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download: ~${plan.totalDownloadMb} MB · Required space: ~${plan.requiredDiskSpaceMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Available free space: ${plan.availableDiskSpaceMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (plan.hasEnoughSpace) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!plan.hasEnoughSpace) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = " Insufficient storage! Need ${plan.spaceShortfallMb} MB more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isWifi) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isWifi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (isWifi) " Connected to Wi-Fi" else " Cellular data warning: large downloads",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Button(
                onClick = onDoneClick,
                enabled = (plan?.hasEnoughSpace == true) && !isStarting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isStarting) "Starting Install..." else "Done")
            }
        }
    }
}
