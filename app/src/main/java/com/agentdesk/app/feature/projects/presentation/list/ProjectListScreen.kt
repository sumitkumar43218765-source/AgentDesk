package com.agentdesk.app.feature.projects.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.EmptyState
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.presentation.list.components.BackupBanner
import com.agentdesk.app.feature.projects.presentation.list.components.ProjectCard

@Composable
fun ProjectListScreen(
    uiState: ProjectListUiState,
    onSearchChange: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDeletePrompt: (Project) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onExportBackup: (Project) -> Unit,
    onCreateClick: () -> Unit,
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

    Scaffold(
        topBar = {
            AppTopBar(title = "Projects")
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Project")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Backup reminders banner for any pending project
            val needsBackup = uiState.projectsNeedingBackup.firstOrNull()
            if (needsBackup != null) {
                BackupBanner(
                    projectName = needsBackup.name,
                    onExportClick = { onExportBackup(needsBackup) }
                )
            }

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search projects...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small)
            )

            if (uiState.filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (uiState.searchQuery.isEmpty()) "No projects yet. Tap '+' to create your first app!" else "No matching projects found."
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.filteredProjects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                            onDuplicate = { onDuplicate(project.id) },
                            onDelete = { onDeletePrompt(project) }
                        )
                    }
                }
            }
        }
    }

    // Typed-name delete confirmation dialog
    val toDelete = uiState.projectToDelete
    if (toDelete != null) {
        var typedName by remember { mutableStateOf("") }
        val canConfirm = typedName == toDelete.name

        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete Project") },
            text = {
                Column {
                    Text("This will permanently delete the project folder and git history.")
                    Text("Type '${toDelete.name}' to confirm:")
                    OutlinedTextField(
                        value = typedName,
                        onValueChange = { typedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.small)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    enabled = canConfirm
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel")
                }
            }
        )
    }
}
