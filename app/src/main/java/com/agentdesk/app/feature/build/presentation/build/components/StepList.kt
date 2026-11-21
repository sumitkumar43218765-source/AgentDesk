package com.agentdesk.app.feature.build.presentation.build.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.StepStatus

@Composable
fun StepList(
    stepStatuses: Map<BuildStepId, StepStatus>,
    stepDurations: Map<BuildStepId, Long>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = "Pipeline Steps",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            BuildStepId.entries.forEach { stepId ->
                val status = stepStatuses[stepId] ?: StepStatus.PENDING
                val durationMs = stepDurations[stepId]
                StepRow(
                    stepId = stepId,
                    status = status,
                    durationMs = durationMs
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    stepId: BuildStepId,
    status: StepStatus,
    durationMs: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                StepStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                StepStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StepStatus.SUCCESS -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                StepStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
                StepStatus.SKIPPED -> {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Skipped",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = stepDisplayName(stepId),
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == StepStatus.SKIPPED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
        }

        if (durationMs != null && durationMs > 0) {
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun stepDisplayName(stepId: BuildStepId): String = when (stepId) {
    BuildStepId.RESOURCE_COMPILE -> "Resource Compile (aapt2)"
    BuildStepId.RESOURCE_LINK -> "Resource Link (aapt2)"
    BuildStepId.JAVA_COMPILE -> "Java Compile (ECJ)"
    BuildStepId.DEX -> "Dexing (D8)"
    BuildStepId.PACKAGE -> "Package (ZIP/JAR)"
    BuildStepId.ALIGN -> "Align (zipalign)"
    BuildStepId.SIGN -> "Sign & Verify (apksigner)"
}

private fun formatDuration(ms: Long): String {
    return if (ms < 1000) "${ms}ms" else "%.1fs".format(ms / 1000.0)
}
