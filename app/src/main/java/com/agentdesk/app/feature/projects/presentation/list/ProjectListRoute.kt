package com.agentdesk.app.feature.projects.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.projects.presentation.create.CreateProjectSheet

@Composable
fun ProjectListRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ProjectListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ProjectListScreen(
        uiState = uiState,
        onSearchChange = viewModel::onSearchQueryChanged,
        onProjectClick = { projectId ->
            navController.navigate(Routes.ProjectDetail(projectId))
        },
        onDuplicate = viewModel::duplicate,
        onDeletePrompt = viewModel::promptDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
        onExportBackup = viewModel::exportToDownloads,
        onCreateClick = viewModel::openCreateSheet,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier
    )

    if (uiState.isCreateSheetOpen) {
        CreateProjectSheet(
            onDismiss = viewModel::closeCreateSheet,
            onProjectCreated = { project ->
                viewModel.closeCreateSheet()
                navController.navigate(Routes.ProjectDetail(project.id))
            }
        )
    }
}
