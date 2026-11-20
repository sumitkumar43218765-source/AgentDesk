package com.agentdesk.app.feature.setup.presentation.install.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolProgress

@Composable
fun StepRow(
    toolProgress: ToolProgress,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (toolProgress.status) {
        ToolInstallStatus.INSTALLED -> "✅" to MaterialTheme.colorScheme.primary
        ToolInstallStatus.INSTALLING -> "⏳" to MaterialTheme.colorScheme.tertiary
        ToolInstallStatus.QUEUED -> "⌛" to MaterialTheme.colorScheme.outline
        ToolInstallStatus.FAILED -> "⚠️" to MaterialTheme.colorScheme.error
        ToolInstallStatus.SKIPPED -> "⏭️" to MaterialTheme.colorScheme.outline
        ToolInstallStatus.NOT_INSTALLED -> "⬜" to MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(Spacing.small))
        Text(
            text = toolProgress.name,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (toolProgress.status == ToolInstallStatus.INSTALLING) {
            Text(
                text = "${toolProgress.pct}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
