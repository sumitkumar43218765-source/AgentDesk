package com.agentdesk.app.feature.projects.presentation.tree.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.projects.domain.model.FileEntry

@Composable
fun FileTreeNode(
    entry: FileEntry,
    depth: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.extraSmall)
            .padding(start = (depth * 20).dp, end = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            entry.isDirectory && isExpanded -> Icons.Default.FolderOpen
            entry.isDirectory -> Icons.Default.Folder
            else -> Icons.Default.Description
        }

        val iconColor = if (entry.isDirectory) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor
        )

        Spacer(modifier = Modifier.width(Spacing.small))

        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (!entry.isDirectory) {
            val sizeKb = entry.sizeBytes / 1024
            Text(
                text = "${sizeKb} KB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
