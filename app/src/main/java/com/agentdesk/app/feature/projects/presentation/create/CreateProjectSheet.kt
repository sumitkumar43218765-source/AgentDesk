package com.agentdesk.app.feature.projects.presentation.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.presentation.create.components.TemplatePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectSheet(
    onDismiss: () -> Unit,
    onProjectCreated: (Project) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.projectCreated.collect { project ->
            onProjectCreated(project)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .padding(bottom = Spacing.extraLarge)
        ) {
            Text(
                text = "Create New Project",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Project Name") },
                placeholder = { Text("My First App") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            OutlinedTextField(
                value = uiState.packageName,
                onValueChange = viewModel::onPackageNameChanged,
                label = { Text("Package Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            TemplatePicker(
                selectedTemplate = uiState.template,
                onSelectTemplate = viewModel::onTemplateSelected
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Button(
                onClick = viewModel::createProject,
                enabled = uiState.name.isNotBlank() && !uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isCreating) "Creating..." else "Create Project")
            }
        }
    }
}
