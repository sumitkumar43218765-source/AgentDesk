package com.agentdesk.app.feature.setup.presentation.manager.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusType
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.setup.domain.model.CheckResult

@Composable
fun HealthCheckCard(
    results: Map<String, CheckResult>,
    isRunning: Boolean,
    onRunChecks: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Environment Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).fillMaxWidth(0.1f))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            if (results.isEmpty()) {
                Text(
                    text = "No health check run yet. Tap below to verify binaries and proot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                for ((title, res) in results) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        when (res) {
                            is CheckResult.Pass -> StatusChip("PASS", StatusType.SUCCESS)
                            is CheckResult.Fail -> StatusChip("FAIL", StatusType.ERROR)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = onRunChecks,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run Checks")
                }

                OutlinedButton(
                    onClick = onCopyDiagnostics,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Copy Diagnostics")
                }
            }
        }
    }
}
