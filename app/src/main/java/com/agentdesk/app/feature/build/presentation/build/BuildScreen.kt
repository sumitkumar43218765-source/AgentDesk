package com.agentdesk.app.feature.build.presentation.build

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.presentation.build.components.BuildActionsBar
import com.agentdesk.app.feature.build.presentation.build.components.DiagnosticsList
import com.agentdesk.app.feature.build.presentation.build.components.RawLogView
import com.agentdesk.app.feature.build.presentation.build.components.StepList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(
    uiState: BuildUiState,
    onVariantChange: (BuildVariant) -> Unit,
    onStartBuild: () -> Unit,
    onCancelBuild: () -> Unit,
    onInstallApk: () -> Unit,
    onExportApk: () -> Unit,
    onShareApk: () -> Unit,
    onFixWithAi: () -> Unit,
    onDiagnosticClick: (file: String?, line: Int?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    onClearStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearStatus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.projectName.isNotBlank()) "Build: ${uiState.projectName}" else "Build Engine",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Build History")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            BuildActionsBar(
                isBuilding = uiState.isBuilding,
                selectedVariant = uiState.selectedVariant,
                latestRun = uiState.latestRun,
                onVariantChange = onVariantChange,
                onStartBuild = onStartBuild,
                onCancelBuild = onCancelBuild,
                onInstallApk = onInstallApk,
                onExportApk = onExportApk,
                onShareApk = onShareApk,
                onFixWithAi = onFixWithAi
            )

            StepList(
                stepStatuses = uiState.stepStatuses,
                stepDurations = uiState.stepDurations
            )

            DiagnosticsList(
                diagnostics = uiState.diagnostics,
                onDiagnosticClick = onDiagnosticClick
            )

            RawLogView(rawLog = uiState.logOutput)
        }
    }
}
