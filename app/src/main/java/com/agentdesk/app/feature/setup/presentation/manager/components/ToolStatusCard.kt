package com.agentdesk.app.feature.setup.presentation.manager.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolState

@Composable
fun ToolStatusCard(
    tool: ToolDefinition,
    state: ToolState?,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onRepair: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInstalled = state?.status == ToolInstallStatus.INSTALLED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (tool.required) {
                    StatusChip(text = "Required", level = StatusLevel.INFO)
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                }
                StatusChip(
                    text = state?.status?.name ?: "NOT INSTALLED",
                    level = when (state?.status) {
                        ToolInstallStatus.INSTALLED -> StatusLevel.SUCCESS
                        ToolInstallStatus.INSTALLING -> StatusLevel.WARNING
                        ToolInstallStatus.FAILED -> StatusLevel.ERROR
                        else -> StatusLevel.INFO
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Version: ${state?.installedVersion ?: tool.version} · Est: ${tool.sizeEstimateMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row {
                    if (isInstalled) {
                        if (tool.required) {
                            OutlinedButton(onClick = onRepair) {
                                Text("Repair")
                            }
                        } else {
                            OutlinedButton(onClick = onRepair) {
                                Text("Reinstall")
                            }
                            Spacer(modifier = Modifier.width(Spacing.small))
                            OutlinedButton(onClick = onRemove) {
                                Text("Remove")
                            }
                        }
                    } else {
                        Button(onClick = onInstall) {
                            Text("Install")
                        }
                    }
                }
            }
        }
    }
}
