package com.agentdesk.app.feature.projects.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.projects.presentation.tree.FileTreeViewModel

@Composable
fun ProjectDetailRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
    treeViewModel: FileTreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val treeUiState by treeViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.project?.slug) {
        val slug = uiState.project?.slug
        if (slug != null) {
            treeViewModel.loadTree(slug)
        }
    }

    ProjectDetailScreen(
        uiState = uiState,
        treeUiState = treeUiState,
        onTabSelected = viewModel::selectTab,
        onFileClick = { entry ->
            val slug = uiState.project?.slug ?: return@ProjectDetailScreen
            if (entry.isDirectory) {
                treeViewModel.toggleDirectory(entry)
            } else {
                navController.navigate(Routes.FileViewer(projectId = slug, path = entry.relativePath))
            }
        },
        onToggleTreeFilter = treeViewModel::toggleHideGitAndBuild,
        onOpenTerminal = {
            navController.navigate(Routes.Terminal)
        },
        onExportBackup = viewModel::exportToDownloads,
        onOpenCheckpointDialog = viewModel::openCheckpointDialog,
        onCloseCheckpointDialog = viewModel::closeCheckpointDialog,
        onCreateCheckpoint = viewModel::createCheckpoint,
        onOpenHistory = viewModel::openHistory,
        onBuildClick = {
            val id = uiState.project?.id
            if (id != null) {
                navController.navigate(Routes.Build(id))
            }
        },
        onCloseHistory = viewModel::closeHistory,
        onRestoreCommit = viewModel::restoreCheckpoint,
        onBackClick = { navController.popBackStack() },
        onClearMessage = viewModel::clearMessage,
        modifier = modifier
    )
}
