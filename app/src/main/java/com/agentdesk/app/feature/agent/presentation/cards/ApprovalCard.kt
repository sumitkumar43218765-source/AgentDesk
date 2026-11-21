package com.agentdesk.app.feature.agent.presentation.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.PermissionRequest
import com.agentdesk.app.feature.agent.domain.model.RiskLevel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApprovalCard(
    request: PermissionRequest,
    onDecision: (PermissionDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Approval Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val riskLevel = when (request.summary.risk) {
                    RiskLevel.HIGH -> StatusLevel.ERROR
                    RiskLevel.MEDIUM -> StatusLevel.WARNING
                    RiskLevel.LOW -> StatusLevel.INFO
                    RiskLevel.DENIED -> StatusLevel.ERROR
                }
                StatusChip(text = "${request.summary.risk} RISK", level = riskLevel)
            }

            Text(
                text = request.summary.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            request.summary.details?.let { details ->
                Text(
                    text = details,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onDecision(PermissionDecision.AllowOnce) }) {
                    Text("Allow once")
                }
                OutlinedButton(onClick = { onDecision(PermissionDecision.AllowSession) }) {
                    Text("Allow session")
                }
                OutlinedButton(onClick = { onDecision(PermissionDecision.AllowProject) }) {
                    Text("Allow project")
                }
                OutlinedButton(onClick = { onDecision(PermissionDecision.Deny("User denied approval")) }) {
                    Text("Deny", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
