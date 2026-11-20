package com.agentdesk.app.feature.terminal.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SessionTabs(
    sessionTitles: List<String>,
    selectedIndex: Int,
    onSelectSession: (Int) -> Unit,
    onNewSession: () -> Unit,
    onCloseSession: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sessionTitles.forEachIndexed { index, title ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelectSession(index) },
                label = { Text(title) },
                trailingIcon = {
                    if (sessionTitles.size > 1) {
                        IconButton(onClick = { onCloseSession(index) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close tab")
                        }
                    }
                },
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        IconButton(onClick = onNewSession) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "New Session")
        }
    }
}
