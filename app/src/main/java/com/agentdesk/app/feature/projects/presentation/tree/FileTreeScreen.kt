package com.agentdesk.app.feature.projects.presentation.tree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.components.EmptyState
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.projects.domain.model.FileEntry
import com.agentdesk.app.feature.projects.presentation.tree.components.FileTreeNode

@Composable
fun FileTreeScreen(
    uiState: FileTreeUiState,
    onNodeClick: (FileEntry) -> Unit,
    onToggleFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.hideGitAndBuild,
                onClick = onToggleFilter,
                label = { Text("Hide .git & build") }
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(title = "Project directory is empty.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.entries, key = { it.relativePath }) { entry ->
                    val depth = entry.relativePath.count { it == '/' }
                    val isExpanded = uiState.expandedPaths.contains(entry.relativePath)

                    FileTreeNode(
                        entry = entry,
                        depth = depth,
                        isExpanded = isExpanded,
                        onClick = { onNodeClick(entry) }
                    )
                }
            }
        }
    }
}
