package com.agentdesk.app.feature.build.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.EmptyState
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildStatus
import com.agentdesk.app.feature.build.presentation.build.components.RawLogView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildHistoryScreen(
    uiState: BuildHistoryUiState,
    onSelectRun: (BuildRun?) -> Unit,
    onDeleteRun: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.projectName.isNotBlank()) "${uiState.projectName} History" else "Build History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.runs.isEmpty() -> {
                    EmptyState(title = "No build history yet.")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        items(uiState.runs, key = { it.id }) { run ->
                            BuildRunCard(
                                run = run,
                                onClick = { onSelectRun(run) },
                                onDelete = { onDeleteRun(run.id) }
                            )
                        }
                    }
                }
            }

            if (uiState.selectedRun != null) {
                AlertDialog(
                    onDismissRequest = { onSelectRun(null) },
                    title = {
                        Text(
                            text = "Build ${uiState.selectedRun.variant} (${uiState.selectedRun.status})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Started: ${formatTimestamp(uiState.selectedRun.startedAt)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (uiState.selectedRun.gitCommit != null) {
                                Text(
                                    text = "Commit: ${uiState.selectedRun.gitCommit.take(7)}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.small))
                            RawLogView(rawLog = uiState.selectedRunLog)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { onSelectRun(null) }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BuildRunCard(
    run: BuildRun,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (level, label) = when (run.status) {
                        BuildStatus.SUCCESS -> StatusLevel.SUCCESS to "SUCCESS"
                        BuildStatus.FAILED -> StatusLevel.ERROR to "FAILED"
                        BuildStatus.CANCELLED -> StatusLevel.WARNING to "CANCELLED"
                        BuildStatus.RUNNING -> StatusLevel.INFO to "RUNNING"
                    }
                    StatusChip(text = label, level = level)
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(
                        text = run.variant.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(run.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (run.summary.failedStep != null) {
                    Text(
                        text = "Failed at: ${run.summary.failedStep.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Run",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(millis))
}
