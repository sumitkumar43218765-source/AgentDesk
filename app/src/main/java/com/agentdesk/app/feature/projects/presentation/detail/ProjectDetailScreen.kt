package com.agentdesk.app.feature.projects.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.MonoText
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.projects.domain.model.FileEntry
import com.agentdesk.app.feature.projects.presentation.detail.components.GitHistorySheet
import com.agentdesk.app.feature.projects.presentation.tree.FileTreeScreen
import com.agentdesk.app.feature.projects.presentation.tree.FileTreeUiState

@Composable
fun ProjectDetailScreen(
    uiState: ProjectDetailUiState,
    treeUiState: FileTreeUiState,
    onTabSelected: (Int) -> Unit,
    onFileClick: (FileEntry) -> Unit,
    onToggleTreeFilter: () -> Unit,
    onOpenTerminal: () -> Unit,
    onExportBackup: () -> Unit,
    onOpenCheckpointDialog: () -> Unit,
    onCloseCheckpointDialog: () -> Unit,
    onCreateCheckpoint: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onCloseHistory: () -> Unit,
    onRestoreCommit: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearMessage()
        }
    }

    val project = uiState.project

    Scaffold(
        topBar = {
            AppTopBar(
                title = project?.name ?: "Project Detail",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTerminal) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal")
                    }
                    IconButton(onClick = onOpenCheckpointDialog) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Checkpoint")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Git History")
                    }
                    IconButton(onClick = onExportBackup) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading || project == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("Files") }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text("Git & Overview") }
                    )
                }

                if (uiState.selectedTab == 0) {
                    FileTreeScreen(
                        uiState = treeUiState,
                        onNodeClick = onFileClick,
                        onToggleFilter = onToggleTreeFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Git & Overview Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.medium)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(Spacing.medium)) {
                                Text(
                                    text = "Project Configuration",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = "Package: ${project.packageName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Slug: ${project.slug}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Template: ${project.template.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.medium))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(Spacing.medium)) {
                                val status = uiState.gitStatus
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Git Working Tree",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusChip(
                                        text = if (status?.clean == true) "CLEAN" else "MODIFIED",
                                        level = if (status?.clean == true) StatusLevel.SUCCESS else StatusLevel.WARNING
                                    )
                                }

                                if (status != null) {
                                    Spacer(modifier = Modifier.height(Spacing.small))
                                    Text(
                                        text = "Branch: ${status.branch} · Staged: ${status.stagedCount} · Unstaged: ${status.unstagedCount} · Untracked: ${status.untrackedCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (uiState.diffStat.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(Spacing.small))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(8.dp)
                                    ) {
                                        MonoText(
                                            text = uiState.diffStat,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isHistoryOpen) {
        GitHistorySheet(
            commits = uiState.commits,
            onRestore = onRestoreCommit,
            onDismiss = onCloseHistory
        )
    }

    if (uiState.isCheckpointDialogOpen) {
        var commitMsg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onCloseCheckpointDialog,
            title = { Text("Create Git Checkpoint") },
            text = {
                OutlinedTextField(
                    value = commitMsg,
                    onValueChange = { commitMsg = it },
                    label = { Text("Checkpoint Message") },
                    placeholder = { Text("e.g. Added button click handler") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onCreateCheckpoint(commitMsg) },
                    enabled = commitMsg.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseCheckpointDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}
