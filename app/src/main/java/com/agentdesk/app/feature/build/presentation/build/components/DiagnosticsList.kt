package com.agentdesk.app.feature.build.presentation.build.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.Severity

@Composable
fun DiagnosticsList(
    diagnostics: List<BuildDiagnostic>,
    onDiagnosticClick: (file: String?, line: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (diagnostics.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = "Diagnostics (${diagnostics.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            diagnostics.forEach { diag ->
                DiagnosticItem(
                    diagnostic = diag,
                    onClick = { onDiagnosticClick(diag.file, diag.line) }
                )
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    diagnostic: BuildDiagnostic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val isError = diagnostic.severity == Severity.ERROR
            SuggestionChip(
                onClick = onClick,
                label = { Text(diagnostic.severity.name) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    labelColor = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
                )
            )

            if (diagnostic.file != null) {
                val locText = if (diagnostic.line != null) {
                    if (diagnostic.column != null) "${diagnostic.file}:${diagnostic.line}:${diagnostic.column}"
                    else "${diagnostic.file}:${diagnostic.line}"
                } else diagnostic.file

                Text(
                    text = locText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = diagnostic.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
