package com.agentdesk.app.feature.settings.presentation.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsScreen(
    defaultRunMode: String,
    maxSteps: Int,
    shellTimeout: Int,
    allowNetwork: Boolean,
    tokenBudget: Int,
    onRunModeChanged: (String) -> Unit,
    onMaxStepsChanged: (Int) -> Unit,
    onShellTimeoutChanged: (Int) -> Unit,
    onAllowNetworkChanged: (Boolean) -> Unit,
    onTokenBudgetChanged: (Int) -> Unit,
    onManageGrantsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Run mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Default Run Mode", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = defaultRunMode == "ASK",
                            onClick = { onRunModeChanged("ASK") }
                        )
                        Text(text = "Ask for Approval", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = defaultRunMode == "AUTO_ACCEPT_EDITS",
                            onClick = { onRunModeChanged("AUTO_ACCEPT_EDITS") }
                        )
                        Text(text = "Auto-accept Edits", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Max Steps
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Max Steps per Run: $maxSteps", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = maxSteps.toFloat(),
                        onValueChange = { onMaxStepsChanged(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 8
                    )
                }
            }

            // Shell Timeout
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Shell Command Timeout: ${shellTimeout}s", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = shellTimeout.toFloat(),
                        onValueChange = { onShellTimeoutChanged(it.toInt()) },
                        valueRange = 10f..600f,
                        steps = 11
                    )
                }
            }

            // Allow Network in Shell
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Allow Network in Shell", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Permit agent shell commands to access the internet (apt, git, curl).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allowNetwork,
                        onCheckedChange = onAllowNetworkChanged
                    )
                }
            }

            // Token Budget
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Run Token Budget: ${tokenBudget / 1000}k tokens", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = tokenBudget.toFloat(),
                        onValueChange = { onTokenBudgetChanged(it.toInt()) },
                        valueRange = 50_000f..1_000_000f,
                        steps = 18
                    )
                }
            }

            // Manage Grants Button
            Button(
                onClick = onManageGrantsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Project Permission Grants")
            }
        }
    }
}
