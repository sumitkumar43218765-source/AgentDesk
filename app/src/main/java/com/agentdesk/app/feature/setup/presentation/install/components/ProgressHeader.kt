package com.agentdesk.app.feature.setup.presentation.install.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.setup.domain.model.InstallProgress
import com.agentdesk.app.feature.setup.domain.model.Phase

@Composable
fun ProgressHeader(
    progress: InstallProgress,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            val statusText = when (progress.phase) {
                Phase.PLANNING -> "Planning install..."
                Phase.INSTALLING -> "Installing tools (${progress.overallPct}%)..."
                Phase.VERIFYING -> "Verifying environment..."
                Phase.SMOKE_TEST -> "Running smoke test..."
                Phase.DONE -> "All required tools installed!"
                Phase.FAILED -> "Installation failed"
                Phase.PAUSED -> "Paused (Waiting for network)"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                color = if (progress.phase == Phase.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            LinearProgressIndicator(
                progress = { progress.overallPct / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                items(progress.tools, key = { it.toolId }) { tool ->
                    StepRow(toolProgress = tool)
                }
            }
        }
    }
}
