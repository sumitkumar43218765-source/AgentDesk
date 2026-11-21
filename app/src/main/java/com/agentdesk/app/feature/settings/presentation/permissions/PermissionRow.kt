package com.agentdesk.app.feature.settings.presentation.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.feature.settings.domain.model.PermissionItem
import com.agentdesk.app.feature.settings.domain.model.PermissionState

@Composable
fun PermissionRow(
    permission: PermissionItem,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = permission.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                val (chipText, chipLevel) = when (permission.state) {
                    PermissionState.GRANTED -> "Granted" to StatusLevel.SUCCESS
                    PermissionState.NOT_GRANTED -> "Not Granted" to StatusLevel.WARNING
                    PermissionState.NOT_NEEDED -> "Not Needed" to StatusLevel.INFO
                }

                StatusChip(text = chipText, level = chipLevel)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            if (permission.state == PermissionState.NOT_GRANTED && permission.actionIntent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onActionClick,
                        modifier = Modifier.height(36.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(text = "Grant / Open Settings", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
